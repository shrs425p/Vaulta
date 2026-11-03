package com.vaulta.viewer

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class PptxSlide(
    val slideNumber: Int,
    val title: String?,
    val subtitle: String? = null,
    val bullets: List<String> = emptyList(),
    val rawText: String = "",
    val backgroundColorHex: String? = null,
    val images: List<ByteArray> = emptyList()
)

data class DocxParagraph(
    val text: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isBullet: Boolean = false
)

data class DocxTable(
    val rows: List<List<String>>
)

data class DocxDocument(
    val title: String?,
    val paragraphs: List<DocxParagraph>,
    val tables: List<DocxTable> = emptyList(),
    val images: List<ByteArray> = emptyList(),
    val rawText: String
)

object DocumentPreviewHelper {

    fun extractEmbeddedImagesFromBinary(bytes: ByteArray): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        try {
            var i = 0
            while (i < bytes.size - 8) {
                // Check JPEG SOI (FF D8 FF)
                if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD8.toByte() && bytes[i + 2] == 0xFF.toByte()) {
                    val start = i
                    var j = i + 3
                    while (j < bytes.size - 1 && j - start < 8_000_000) {
                        if (bytes[j] == 0xFF.toByte() && bytes[j + 1] == 0xD9.toByte()) {
                            val img = bytes.copyOfRange(start, j + 2)
                            if (img.size > 1024) {
                                list.add(img)
                            }
                            i = j + 2
                            break
                        }
                        j++
                    }
                }
                // Check PNG header (89 50 4E 47)
                else if (bytes[i] == 0x89.toByte() && bytes[i + 1] == 0x50.toByte() &&
                    bytes[i + 2] == 0x4E.toByte() && bytes[i + 3] == 0x47.toByte()
                ) {
                    val start = i
                    var j = i + 8
                    while (j < bytes.size - 8 && j - start < 8_000_000) {
                        if (bytes[j] == 0x49.toByte() && bytes[j + 1] == 0x45.toByte() &&
                            bytes[j + 2] == 0x4E.toByte() && bytes[j + 3] == 0x44.toByte()
                        ) {
                            val end = (j + 8).coerceAtMost(bytes.size)
                            val img = bytes.copyOfRange(start, end)
                            if (img.size > 1024) {
                                list.add(img)
                            }
                            i = end
                            break
                        }
                        j++
                    }
                }
                i++
                if (list.size >= 20) break
            }
        } catch (_: Exception) {}
        return list
    }

    fun extractDocxDocument(bytes: ByteArray): DocxDocument {
        return try {
            var documentXml: String? = null
            val mediaImages = mutableListOf<ByteArray>()

            try {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (name.endsWith("word/document.xml", ignoreCase = true)) {
                            documentXml = zis.readBytes().toString(Charsets.UTF_8)
                        } else if (name.contains("word/media/", ignoreCase = true) && !entry.isDirectory) {
                            val img = zis.readBytes()
                            if (img.isNotEmpty()) {
                                mediaImages.add(img)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            } catch (_: Exception) {}

            if (documentXml != null) {
                val doc = parseWordXmlToDocument(documentXml, mediaImages)
                if (doc.paragraphs.isNotEmpty() || doc.tables.isNotEmpty()) {
                    return doc
                }
            }

            // Fallback for legacy .doc or binary files
            val binaryImages = extractEmbeddedImagesFromBinary(bytes)
            val allImages = if (mediaImages.isNotEmpty()) mediaImages else binaryImages
            val rawText = extractBinaryOfficeText(bytes) ?: fallbackRegexExtract(String(bytes, Charsets.ISO_8859_1))
            val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

            val paragraphs = lines.map { line ->
                val isHeading = (line.length < 60 && !line.endsWith('.')) || line.all { it.isUpperCase() || !it.isLetter() }
                val isBullet = line.startsWith("-") || line.startsWith("*") || line.startsWith("•")
                val cleanText = if (isBullet) line.removePrefix("-").removePrefix("*").removePrefix("•").trim() else line
                DocxParagraph(
                    text = cleanText,
                    isHeading = isHeading && !isBullet,
                    headingLevel = if (isHeading && !isBullet) 2 else 0,
                    isBullet = isBullet
                )
            }

            val title = paragraphs.firstOrNull { it.isHeading }?.text ?: paragraphs.firstOrNull()?.text
            DocxDocument(
                title = title,
                paragraphs = paragraphs,
                tables = emptyList(),
                images = allImages,
                rawText = rawText
            )
        } catch (_: Exception) {
            DocxDocument(title = null, paragraphs = emptyList(), tables = emptyList(), images = emptyList(), rawText = "")
        }
    }

    private fun parseWordXmlToDocument(xml: String, images: List<ByteArray>): DocxDocument {
        val paragraphs = mutableListOf<DocxParagraph>()
        val tables = mutableListOf<DocxTable>()
        val currentPara = StringBuilder()
        var isBullet = false
        var isHeading = false
        var headingLevel = 0

        var inTable = false
        var inCell = false
        val currentCell = StringBuilder()
        val currentRow = mutableListOf<String>()
        val currentTableRows = mutableListOf<List<String>>()

        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var eventType = parser.eventType
            var inText = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name?.substringAfterLast(':')?.lowercase() ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (localName) {
                            "tbl" -> {
                                inTable = true
                                currentTableRows.clear()
                            }
                            "tr" -> currentRow.clear()
                            "tc" -> {
                                inCell = true
                                currentCell.clear()
                            }
                            "p" -> {
                                currentPara.clear()
                                isBullet = false
                                isHeading = false
                                headingLevel = 0
                            }
                            "pstyle" -> {
                                val styleVal = parser.getAttributeValue(null, "val")?.lowercase() ?: ""
                                if (styleVal.contains("title") || styleVal.contains("heading 1") || styleVal == "1" || styleVal == "heading1") {
                                    isHeading = true
                                    headingLevel = 1
                                } else if (styleVal.contains("heading 2") || styleVal == "2" || styleVal == "heading2") {
                                    isHeading = true
                                    headingLevel = 2
                                } else if (styleVal.contains("heading 3") || styleVal == "3" || styleVal == "heading3") {
                                    isHeading = true
                                    headingLevel = 3
                                }
                            }
                            "numpr" -> isBullet = true
                            "t" -> inText = true
                            "br" -> {
                                if (inCell) currentCell.append(" ") else currentPara.append("\n")
                            }
                            "tab" -> {
                                if (inCell) currentCell.append(" ") else currentPara.append("\t")
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            val text = parser.text ?: ""
                            if (inCell) {
                                currentCell.append(text)
                            } else {
                                currentPara.append(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (localName) {
                            "t" -> inText = false
                            "tc" -> {
                                inCell = false
                                val cellText = currentCell.toString().trim()
                                currentRow.add(cellText)
                            }
                            "tr" -> {
                                if (currentRow.isNotEmpty()) {
                                    currentTableRows.add(currentRow.toList())
                                }
                            }
                            "tbl" -> {
                                inTable = false
                                if (currentTableRows.isNotEmpty()) {
                                    tables.add(DocxTable(currentTableRows.toList()))
                                }
                                currentTableRows.clear()
                            }
                            "p" -> {
                                if (!inTable) {
                                    val text = currentPara.toString().trim()
                                    if (text.isNotBlank()) {
                                        val isShortHeader = text.length < 60 && !text.endsWith('.') && text.lines().size == 1 &&
                                                (isHeading || text.all { it.isUpperCase() || !it.isLetter() })
                                        paragraphs.add(
                                            DocxParagraph(
                                                text = text,
                                                isHeading = isHeading || isShortHeader,
                                                headingLevel = if (headingLevel > 0) headingLevel else if (isShortHeader) 2 else 0,
                                                isBullet = isBullet || text.startsWith("• ") || text.startsWith("- ")
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        val title = paragraphs.firstOrNull { it.isHeading }?.text ?: paragraphs.firstOrNull()?.text
        val rawText = paragraphs.joinToString("\n\n") { it.text }
        return DocxDocument(
            title = title,
            paragraphs = paragraphs,
            tables = tables,
            images = images,
            rawText = rawText
        )
    }

    fun extractDocxText(bytes: ByteArray): String? {
        val doc = extractDocxDocument(bytes)
        return doc.rawText.takeIf { it.isNotBlank() } ?: fallbackRegexExtract(String(bytes, Charsets.ISO_8859_1))
    }

    private fun parseWordXml(xml: String): String {
        val doc = parseWordXmlToDocument(xml, emptyList())
        return doc.rawText
    }

    fun extractPptxSlides(bytes: ByteArray): List<PptxSlide> {
        return try {
            val slideMap = sortedMapOf<Int, String>()
            val relsMap = mutableMapOf<Int, String>()
            val mediaMap = mutableMapOf<String, ByteArray>()
            val slideRegex = Regex(""".*ppt[/\\]slides[/\\]slide(\d+)\.xml$""", RegexOption.IGNORE_CASE)
            val relsRegex = Regex(""".*ppt[/\\]slides[/\\]_rels[/\\]slide(\d+)\.xml\.rels$""", RegexOption.IGNORE_CASE)

            try {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val normalized = entry.name.replace('\\', '/')
                        val slideMatch = slideRegex.matchEntire(normalized)
                        val relsMatch = relsRegex.matchEntire(normalized)

                        if (slideMatch != null) {
                            val num = slideMatch.groupValues[1].toIntOrNull() ?: 1
                            slideMap[num] = zis.readBytes().toString(Charsets.UTF_8)
                        } else if (relsMatch != null) {
                            val num = relsMatch.groupValues[1].toIntOrNull() ?: 1
                            relsMap[num] = zis.readBytes().toString(Charsets.UTF_8)
                        } else if (normalized.contains("ppt/media/", ignoreCase = true) && !entry.isDirectory) {
                            val filename = normalized.substringAfterLast('/')
                            val imgBytes = zis.readBytes()
                            if (imgBytes.isNotEmpty()) {
                                mediaMap[filename] = imgBytes
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            } catch (_: Exception) {}

            if (slideMap.isNotEmpty()) {
                val slides = mutableListOf<PptxSlide>()
                for ((num, xml) in slideMap) {
                    val relsXml = relsMap[num]
                    val slide = parseSlideXmlToSlide(num, xml, relsXml, mediaMap)
                    if (slide.rawText.isNotBlank() || slide.images.isNotEmpty()) {
                        slides.add(slide)
                    }
                }
                if (slides.isNotEmpty()) return slides
            }

            extractLegacyPptSlides(bytes)
        } catch (_: Exception) {
            extractLegacyPptSlides(bytes)
        }
    }

    private fun parseSlideXmlToSlide(
        slideNum: Int,
        xml: String,
        relsXml: String?,
        mediaMap: Map<String, ByteArray>
    ): PptxSlide {
        val paragraphs = mutableListOf<String>()
        val currentParagraph = StringBuilder()
        var title: String? = null
        var subtitle: String? = null
        var isTitleShape = false
        var isSubTitleShape = false

        val colorRegex = Regex("""<a:srgbClr\s+val="([0-9a-fA-F]{6})"/>""")
        val bgMatch = colorRegex.find(xml)
        val bgColorHex = bgMatch?.let { "#" + it.groupValues[1] }

        val slideImages = mutableListOf<ByteArray>()
        if (relsXml != null) {
            val relRegex = Regex("""Id="([^"]+)".*?Target="([^"]+)"""")
            val idToTarget = mutableMapOf<String, String>()
            relRegex.findAll(relsXml).forEach { match ->
                val id = match.groupValues[1]
                val target = match.groupValues[2].substringAfterLast('/')
                idToTarget[id] = target
            }

            val blipRegex = Regex("""<a:blip[^>]*r:embed="([^"]+)"""")
            blipRegex.findAll(xml).forEach { match ->
                val embedId = match.groupValues[1]
                val targetFile = idToTarget[embedId]
                if (targetFile != null) {
                    val imgData = mediaMap[targetFile]
                    if (imgData != null && !slideImages.contains(imgData)) {
                        slideImages.add(imgData)
                    }
                }
            }
        }
        if (slideImages.isEmpty() && mediaMap.isNotEmpty()) {
            val allImages = mediaMap.values.toList()
            val index = (slideNum - 1) % allImages.size
            slideImages.add(allImages[index])
        }

        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var eventType = parser.eventType
            var inText = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name?.substringAfterLast(':')?.lowercase() ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (localName) {
                            "sp" -> {
                                isTitleShape = false
                                isSubTitleShape = false
                            }
                            "ph" -> {
                                val type = parser.getAttributeValue(null, "type")?.lowercase() ?: ""
                                if (type == "title" || type == "ctrtitle") {
                                    isTitleShape = true
                                } else if (type == "subtitle" || type == "sub") {
                                    isSubTitleShape = true
                                }
                            }
                            "t" -> inText = true
                            "br" -> currentParagraph.append("\n")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            currentParagraph.append(parser.text ?: "")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (localName) {
                            "t" -> inText = false
                            "p" -> {
                                val text = currentParagraph.toString().trim()
                                if (text.isNotBlank()) {
                                    if (isTitleShape && title == null) {
                                        title = text
                                    } else if (isSubTitleShape && subtitle == null) {
                                        subtitle = text
                                    } else {
                                        paragraphs.add(text)
                                    }
                                }
                                currentParagraph.clear()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        val remaining = currentParagraph.toString().trim()
        if (remaining.isNotBlank()) {
            paragraphs.add(remaining)
        }

        if (title == null && paragraphs.isNotEmpty()) {
            title = paragraphs.removeAt(0)
        }

        val rawText = listOfNotNull(title, subtitle).plus(paragraphs).joinToString("\n")
        return PptxSlide(
            slideNumber = slideNum,
            title = title,
            subtitle = subtitle,
            bullets = paragraphs,
            rawText = rawText,
            backgroundColorHex = bgColorHex,
            images = slideImages
        )
    }

    fun extractLegacyPptSlides(bytes: ByteArray): List<PptxSlide> {
        val images = extractEmbeddedImagesFromBinary(bytes)
        val text = extractBinaryOfficeText(bytes) ?: ""
        val rawLines = text.lines().map { it.trim() }.filter { it.length >= 2 }

        if (rawLines.isEmpty() && images.isEmpty()) {
            return emptyList()
        }

        val slides = mutableListOf<PptxSlide>()
        var slideIndex = 1
        val chunkSize = 4
        val chunks = rawLines.chunked(chunkSize)

        for (chunk in chunks) {
            val slideTitle = chunk.firstOrNull() ?: "Slide $slideIndex"
            val bullets = if (chunk.size > 1) chunk.drop(1) else emptyList()
            val slideImages = if (images.isNotEmpty()) {
                val imgIdx = (slideIndex - 1) % images.size
                listOf(images[imgIdx])
            } else emptyList()

            slides.add(
                PptxSlide(
                    slideNumber = slideIndex++,
                    title = slideTitle,
                    bullets = bullets,
                    rawText = "$slideTitle\n" + bullets.joinToString("\n"),
                    images = slideImages
                )
            )
        }

        if (slides.isEmpty() && images.isNotEmpty()) {
            for ((idx, img) in images.withIndex()) {
                slides.add(
                    PptxSlide(
                        slideNumber = idx + 1,
                        title = "Slide ${idx + 1}",
                        bullets = emptyList(),
                        rawText = "Slide ${idx + 1}",
                        images = listOf(img)
                    )
                )
            }
        }

        return slides
    }

    fun extractPptxText(bytes: ByteArray): String? {
        val slides = extractPptxSlides(bytes)
        if (slides.isEmpty()) return fallbackRegexExtract(String(bytes, Charsets.ISO_8859_1)).takeIf { it.isNotBlank() }
        val sb = StringBuilder()
        for (slide in slides) {
            sb.append("════════ Slide ").append(slide.slideNumber).append(" ════════\n")
            if (slide.title != null) {
                sb.append(slide.title).append("\n\n")
            }
            for (bullet in slide.bullets) {
                sb.append("• ").append(bullet).append("\n")
            }
            sb.append("\n")
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun fallbackRegexExtract(content: String): String {
        return try {
            val textRegex = Regex("""<[a-zA-Z0-9]*:?t\b[^>]*>(.*?)</[a-zA-Z0-9]*:?t>""", RegexOption.DOT_MATCHES_ALL)
            val pRegex = Regex("""<[a-zA-Z0-9]*:?p\b[^>]*>""")
            val cleaned = content.replace(pRegex, "\n\n")
            val sb = StringBuilder()
            textRegex.findAll(cleaned).forEach { match ->
                val txt = match.groupValues[1]
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                sb.append(txt).append(" ")
            }
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun extractBinaryOfficeText(bytes: ByteArray): String? {
        return try {
            val sb = StringBuilder()
            var i = 0
            while (i < bytes.size - 1) {
                val b0 = bytes[i].toInt() and 0xFF
                val b1 = bytes[i + 1].toInt() and 0xFF
                if (b1 == 0 && (b0 in 32..126 || b0 == 10 || b0 == 13 || b0 == 9)) {
                    sb.append(b0.toChar())
                    i += 2
                } else if (b0 in 32..126 || b0 == 10 || b0 == 13 || b0 == 9) {
                    sb.append(b0.toChar())
                    i++
                } else {
                    i++
                }
            }
            val text = sb.toString()
            val lines = text.split(Regex("""[\r\n]+"""))
                .map { it.trim() }
                .filter { line -> line.length >= 4 && line.count { ch -> ch.isLetterOrDigit() } >= 3 }
            lines.joinToString("\n\n").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun extractXlsxRows(bytes: ByteArray): List<List<String>> {
        return try {
            var sharedStringsXml: String? = null
            var sheetXml: String? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val normalized = entry.name.replace('\\', '/')
                    if (normalized.endsWith("xl/sharedStrings.xml", ignoreCase = true)) {
                        sharedStringsXml = zis.readBytes().toString(Charsets.UTF_8)
                    } else if (normalized.endsWith("xl/worksheets/sheet1.xml", ignoreCase = true)) {
                        sheetXml = zis.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zis.nextEntry
                }
            }

            val sheet = sheetXml ?: return emptyList()
            val sharedStrings = if (sharedStringsXml != null) parseSharedStrings(sharedStringsXml!!) else emptyList()
            parseSheetXml(sheet, sharedStrings)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val list = ArrayList<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var eventType = parser.eventType
            var inText = false
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name?.substringAfterLast(':')?.lowercase() ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (localName == "t") {
                            inText = true
                            currentText.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (localName == "t") {
                            inText = false
                        } else if (localName == "si") {
                            list.add(currentText.toString())
                            currentText.clear()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseSheetXml(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var eventType = parser.eventType
            var currentRow: ArrayList<String>? = null
            var isShared = false
            val currentCellText = StringBuilder()
            var inValue = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name?.substringAfterLast(':')?.lowercase() ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (localName) {
                            "row" -> {
                                currentRow = ArrayList()
                            }
                            "c" -> {
                                val type = parser.getAttributeValue(null, "t")
                                isShared = type == "s"
                                currentCellText.clear()
                            }
                            "v", "t" -> {
                                inValue = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inValue) {
                            currentCellText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (localName) {
                            "v", "t" -> {
                                inValue = false
                            }
                            "c" -> {
                                val raw = currentCellText.toString().trim()
                                val cellVal = if (isShared) {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) {
                                        sharedStrings[idx]
                                    } else {
                                        raw
                                    }
                                } else {
                                    raw
                                }
                                currentRow?.add(cellVal)
                            }
                            "row" -> {
                                currentRow?.let {
                                    if (it.any { cell -> cell.isNotBlank() }) {
                                        rows.add(it)
                                    }
                                }
                                currentRow = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return rows
    }

    fun extractOdtText(bytes: ByteArray): String? {
        return try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val normalized = entry.name.replace('\\', '/')
                    if (normalized.endsWith("content.xml", ignoreCase = true)) {
                        val xml = zis.readBytes().toString(Charsets.UTF_8)
                        return parseOdtXml(xml).ifBlank { fallbackRegexExtract(xml) }
                    }
                    entry = zis.nextEntry
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOdtXml(xml: String): String {
        return try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            val sb = StringBuilder()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name?.substringAfterLast(':')?.lowercase() ?: ""
                if (eventType == XmlPullParser.START_TAG && (localName == "p" || localName == "h")) {
                    val text = parser.nextText()
                    if (text.isNotBlank()) {
                        sb.append(text).append("\n\n")
                    }
                }
                eventType = parser.next()
            }
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun extractRtfText(rtf: String): String {
        return try {
            var text = rtf
            text = text.replace(Regex("""\\par\b"""), "\n")
            text = text.replace(Regex("""\\tab\b"""), "\t")
            text = text.replace(Regex("""\\[a-zA-Z0-9\-]+ ?"""), "")
            text = text.replace("{", "").replace("}", "")
            text.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")
        } catch (_: Exception) {
            rtf
        }
    }

    fun parseCsv(text: String, delimiter: Char = ','): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val lines = text.lines()
        for (line in lines) {
            if (line.isBlank()) continue
            val row = mutableListOf<String>()
            val sb = StringBuilder()
            var inQuotes = false
            for (ch in line) {
                when (ch) {
                    '"' -> inQuotes = !inQuotes
                    delimiter -> {
                        if (inQuotes) {
                            sb.append(ch)
                        } else {
                            row.add(sb.toString().trim())
                            sb.setLength(0)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            row.add(sb.toString().trim())
            rows.add(row)
        }
        return rows
    }
}

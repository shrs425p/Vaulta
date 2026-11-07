import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.vaulta.shrs"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vaulta.shrs"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("offline") {
            dimension = "distribution"
            applicationIdSuffix = ".offline"
            versionNameSuffix = "-offline"
            buildConfigField("Boolean", "IS_OFFLINE", "true")
        }
        create("cloud") {
            dimension = "distribution"
            buildConfigField("Boolean", "IS_OFFLINE", "false")
        }
    }

    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                load(stream)
            }
        }
    }

    val storeFilePath = providers.environmentVariable("VAULTA_RELEASE_STORE_FILE").orNull
        ?: providers.environmentVariable("RELEASE_STORE_FILE").orNull
        ?: localProperties.getProperty("RELEASE_STORE_FILE")
    val releaseStorePassword = providers.environmentVariable("VAULTA_RELEASE_KEYSTORE_PASSWORD").orNull
        ?: providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
        ?: localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = providers.environmentVariable("VAULTA_RELEASE_KEY_ALIAS").orNull
        ?: providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
        ?: localProperties.getProperty("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = providers.environmentVariable("VAULTA_RELEASE_KEY_PASSWORD").orNull
        ?: providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
        ?: localProperties.getProperty("RELEASE_KEY_PASSWORD")

    val hasValidReleaseKey = listOf(storeFilePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() } &&
        (!storeFilePath.isNullOrBlank() && file(storeFilePath).exists())

    if (hasValidReleaseKey) {
        signingConfigs {
            create("releaseKey") {
                storeFile = file(requireNotNull(storeFilePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasValidReleaseKey) {
                signingConfig = signingConfigs.getByName("releaseKey")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(project(":core-crypto"))
    implementation(project(":core-vault"))
    implementation(project(":core-storage"))
    "cloudImplementation"(project(":core-sync"))
    implementation(project(":feature-onboarding"))
    implementation(project(":feature-browser"))
    implementation(project(":feature-viewer"))
    implementation(project(":feature-settings"))

    implementation(libs.biometric)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.work.runtime)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

# Google Drive & HTTP
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-keep class androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

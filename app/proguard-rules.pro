# Vaulta ProGuard / R8 Hardening Rules
-keepattributes *Annotation*

# Google Tink crypto
-keep class com.google.crypto.tink.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Google API client & Apache HTTP optional classes on Android
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
# DriveSyncClientImpl uses NetHttpTransport; the optional Apache adapter is excluded.
-dontwarn com.google.api.client.http.apache.v2.ApacheHttpTransport

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

# Argon2kt Native JNI
-keep class com.lambdapioneer.argon2kt.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Never log or keep debug symbols for security modules
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

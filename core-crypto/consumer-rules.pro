# Preserve only Argon2Kt's JNI bridge classes. Native symbol names must remain
# stable for the packaged Argon2 JNI library to resolve them at runtime.
-keep class com.lambdapioneer.argon2kt.Argon2Jni { *; }
-keep class com.lambdapioneer.argon2kt.Argon2JniVerification { *; }

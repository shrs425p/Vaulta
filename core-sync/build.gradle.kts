plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.vaulta.sync"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// DriveSyncClientImpl explicitly uses Google NetHttpTransport. Do not package
// the unused Apache transport/client stack, which includes trust-all helpers.
configurations.configureEach {
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    implementation(project(":core-crypto"))
    implementation(project(":core-vault"))
    implementation(project(":core-storage"))
    implementation(libs.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    implementation(libs.google.api.drive)
    implementation(libs.google.api.client.android)
    implementation(libs.google.auth.http)
    implementation(libs.play.services.auth)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}

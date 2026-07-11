// Top-level build file — configuration shared across subprojects/modules
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    // FCM (family scan-notification feature) — DO NOT uncomment until you've
    // added your own app/google-services.json (see app/FCM_SETUP.md).
    // Applying this plugin without that file present will break the build.
    // id("com.google.gms.google-services") version "4.4.2" apply false
}

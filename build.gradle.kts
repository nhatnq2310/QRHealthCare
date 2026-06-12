// Top-level build file — configuration shared across subprojects/modules
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
}

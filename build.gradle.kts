// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // We will use the ID method since it's more direct for Firebase
    id("com.google.gms.google-services") version "4.4.1" apply false
}
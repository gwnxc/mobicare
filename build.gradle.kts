// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Add this line below:
    id("com.google.gms.google-services") version "4.4.1" apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
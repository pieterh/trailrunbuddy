import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.trailrunbuddy.shared"
        compileSdk = 36
        minSdk = 31
        
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Enable host-side (unit) tests to fix "Unused Kotlin Source Sets" for commonTest
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.javax.inject)
        }

        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.coroutines.test)
        }
    }
}

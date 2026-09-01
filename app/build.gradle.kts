import java.util.Properties

val releaseSigningProperties =
    (providers.gradleProperty("seliaCyclesKeystoreProperties").orNull
        ?: providers.environmentVariable("SELIA_CYCLES_KEYSTORE_PROPERTIES").orNull)
        ?.let(::file)
        ?.takeIf { it.isFile }
        ?.let { propertiesFile -> Properties().apply { propertiesFile.inputStream().use(::load) } }
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.majkeylab.seliacycles"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.majkeylab.seliacycles"
        minSdk = 29
        targetSdk = 36
        versionCode = 14
        versionName = "0.9.0-beta.6"
    }

    signingConfigs {
        if (releaseSigningProperties != null) {
            create("release") {
                storeFile = file(requireNotNull(releaseSigningProperties.getProperty("storeFile")))
                storePassword = requireNotNull(releaseSigningProperties.getProperty("storePassword"))
                keyAlias = requireNotNull(releaseSigningProperties.getProperty("keyAlias"))
                keyPassword = requireNotNull(releaseSigningProperties.getProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk.debugSymbolLevel = "FULL"
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "cs", "sk", "de", "pl", "es")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.github.skydoves:colorpicker-compose:1.1.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    debugImplementation("androidx.compose.ui:ui-tooling")
}

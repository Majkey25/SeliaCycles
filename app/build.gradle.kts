import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.util.Properties

val releaseSigningPropertiesPath = providers.gradleProperty("seliaCyclesKeystoreProperties").orNull
    ?: providers.environmentVariable("SELIA_CYCLES_KEYSTORE_PROPERTIES").orNull
val releaseSigningProperties = releaseSigningPropertiesPath?.let(::file)?.also { propertiesFile ->
    require(propertiesFile.isFile) { "Release signing properties file does not exist: $propertiesFile" }
}?.let { propertiesFile -> Properties().apply { propertiesFile.inputStream().use(::load) } }
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val distributionChannel = providers.gradleProperty("distributionChannel").orNull
require(distributionChannel == null || distributionChannel in setOf("play", "github")) {
    "distributionChannel must be play or github"
}

android {
    namespace = "com.majkeylab.seliacycles"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.majkeylab.seliacycles"
        minSdk = 29
        targetSdk = 36
        versionCode = 35
        versionName = "0.9.0-beta.27"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "EXTERNAL_DONATIONS_ENABLED", (distributionChannel == "github").toString())
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
        debug {
            buildConfigField("boolean", "EXTERNAL_DONATIONS_ENABLED", (distributionChannel != "play").toString())
        }
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            matchingFallbacks += "debug"
        }
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
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "cs", "sk", "de", "pl", "es")
    }
    // Cleanup must recognize titles written in any supported language, including offline Play installs.
    bundle { language { enableSplit = false } }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    testBuildType = "qa"
}

extensions.configure<ApplicationAndroidComponentsExtension> {
    // Keep debug JVM tests while device tests use the isolated QA build.
    beforeVariants(selector().withBuildType("debug")) { variant ->
        variant.enableUnitTest = true
    }
}

tasks.matching { it.name == "packageReleaseBundle" }.configureEach {
    val githubChannel = distributionChannel == "github"
    doFirst { check(!githubChannel) { "Google Play bundles must use distributionChannel=play" } }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.github.skydoves:colorpicker-compose:1.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    testImplementation("org.robolectric:robolectric:4.18")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

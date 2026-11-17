import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Release signing: read from keystore.properties (git-ignored, lives outside the repo's tracked files).
// If the file is absent (e.g. a fresh checkout) debug builds still work and the release build is simply unsigned.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.spcode.tnpscprep"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spcode.tnpscprep"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        // "perf": exactly the release code (R8 + shrunk resources) but signed with the DEBUG key, so it installs OVER a debug build without wiping
        // its data. Only for measuring speed on a phone; never hand this APK to anyone.
        create("perf") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // Ship only the two UI languages: drops translations bundled inside libraries (smaller APK).
    androidResources { localeFilters += listOf("en", "ta") }
}

ksp {
    // Exported Room schemas = baseline for future migrations.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)   // splash with the Mathi symbol (Android 12 style on every API level)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    // The XML pull parser is built into Android; JVM unit tests need a real implementation on the classpath.
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("xmlpull:xmlpull:1.1.3.1")
    // TEST-only: runs the real SQLite LIKE statement on Tamil text in JVM unit tests. Never packaged into the app.
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.0")
}

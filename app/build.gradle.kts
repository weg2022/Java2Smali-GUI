plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "weg.ide.tools.java2smali.ui"
    compileSdk = 35

    defaultConfig {
        applicationId = "weg.ide.tools.java2smali.ui"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = false
        buildConfig = true
    }

    packaging{
        jniLibs {
            useLegacyPackaging =true
        }
        resources {
            excludes.addAll(arrayOf(
                "androidsupportmultidexversion.txt",
                "META-INF/**.version",
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "license/**",
                "junit/runner/**",
                "LICENSE-junit.txt",
                "META-INF/INDEX.LIST",
                "org/spongycastle/x509/CertPathReviewerMessages_de.properties",
                "rebel.xml",
                "r8-version.properties",
                "**.jflex",
                "ecj.1",
                "**.flex"
            ))
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "includes" to listOf("*.jar"))))
    implementation("androidx.annotation:annotation:1.2.0")
}
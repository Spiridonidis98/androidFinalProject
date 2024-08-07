plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
    alias(libs.plugins.googleAndroidLibrariesMapsplatformSecretsGradlePlugin)
}

android {
    namespace = "com.kouts.spiri.smartalert"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kouts.spiri.smartalert"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("com.google.android.flexbox:flexbox:3.0.0");
    implementation(libs.play.services.maps)
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor( "com.github.bumptech.glide:compiler:4.12.0")

    implementation("androidx.navigation:navigation-fragment:2.6.0'");
    implementation("androidx.navigation:navigation-ui:2.6.0");
    implementation("com.google.android.material:material:1.8.0");
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.work.runtime)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
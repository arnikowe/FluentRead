plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.fluentread"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fluentread"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Tłumaczenia
    implementation("com.squareup.okhttp3:okhttp:4.12.0")


    //aniamcje
    implementation("com.airbnb.android:lottie-compose:6.1.0")


    implementation("com.google.android.gms:play-services-base:18.2.0")
    //implementation("com.google.android.gms:play-services-providerinstaller:20.0.0")


    implementation("com.google.android.gms:play-services-base:18.2.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config-ktx")


    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation ("com.google.accompanist:accompanist-flowlayout:0.31.5-beta")
    implementation("com.patrykandpatrick.vico:compose:1.10.0")
    implementation("com.patrykandpatrick.vico:core:1.10.0")






    // Cloudinary / Coil
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation(libs.translate)
    implementation(libs.generativeai)
    implementation(libs.play.services.maps)

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")

    //Chart
    implementation("com.patrykandpatrick.vico:core:1.14.0")
    implementation("com.patrykandpatrick.vico:compose:1.14.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")


}


apply(plugin = "com.google.gms.google-services")

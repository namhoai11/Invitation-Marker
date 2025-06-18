plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.invitationcard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.invitationcard"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(project(":sticker"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

//    implementation ("androidx.annotation:annotation:1.7.1")
//    implementation ("androidx.legacy:legacy-support-v4:1.0.0")

    // For downloading fonts
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")

// Gson converter cho Retrofit
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Thư viện xử lý file PSD
    implementation ("com.twelvemonkeys.imageio:imageio-psd:3.8.2")
    implementation ("org.apache.commons:commons-imaging:1.0-alpha2")

    // Thêm AndroidSVG - thư viện tốt nhất để xử lý SVG trên Android
    implementation("com.caverock:androidsvg-aar:1.4")

    implementation ("com.google.mlkit:text-recognition:16.0.0")

    implementation ("androidx.multidex:multidex:2.0.1")
}
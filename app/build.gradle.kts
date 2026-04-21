plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Thêm plugin Google Services
}

android {
    namespace = "com.example.btl_nhom6"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.btl_nhom6"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")      // Xác thực người dùng
    implementation("com.google.firebase:firebase-firestore") // Cơ sở dữ liệu đám mây
    implementation("com.google.firebase:firebase-storage")   // Lưu trữ hình ảnh

    // Glide for image loading
    implementation(libs.glide)

    // Room (vẫn giữ nếu bạn muốn lưu cache offline)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

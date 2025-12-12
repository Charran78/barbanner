// ============================================
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false // ✅ Versión actualizada a 1.9.22
}

// ============================================
// 2. build.gradle.kts (MÓDULO APP TV)
// Ubicación: /app/build.gradle.kts
// ============================================
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") // ✅ Plugin de serialización
}

android {
    namespace = "com.example.barbanner.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.barbanner.tv"
        minSdk = 26 // Android TV normalmente usa API 26+
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
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST" // ✅ Evita conflictos de Ktor
            excludes += "/META-INF/io.netty.versions.properties" // ✅ Evita conflictos
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose para TV (versiones específicas de TV)
    implementation("androidx.tv:tv-material:1.0.0-alpha10")
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    
    // Compose UI base
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // También necesario para algunos componentes
    implementation("androidx.compose.material:material-icons-extended:1.6.1")

    // Ktor SERVIDOR (para recibir mensajes desde mobile)
    val ktorVersion = "2.3.10"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    // Serialización Kotlinx (necesario para BannerAction)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Leanback (opcional, para mejor experiencia TV)
    implementation("androidx.leanback:leanback:1.2.0-alpha04")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
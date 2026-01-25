plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "xyz.useclippr.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        
        consumerProguardFiles("consumer-rules.pro")
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON parsing
    implementation("org.json:json:20231013")
    
    // Play Install Referrer (for deterministic attribution)
    implementation("com.android.installreferrer:installreferrer:2.2")
    
    // Google Play Services Ads (for GAID - advertising ID)
    // TODO(mastersam07): Enable this for GAID
    // implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

// Publishing configuration
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                
                groupId = "xyz.useclippr"
                artifactId = "clippr"
                version = "0.0.1"
                
                pom {
                    name.set("Clippr SDK")
                    description.set("Deep linking and mobile attribution SDK for Android")
                    url.set("https://github.com/nexlabstudio/clippr-android")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("clippr")
                            name.set("Nexlab Studio")
                            email.set("engr@nexlab.studio")
                        }
                    }
                    
                    scm {
                        url.set("https://github.com/nexlabstudio/clippr-android")
                    }
                }
            }
        }
    }
}

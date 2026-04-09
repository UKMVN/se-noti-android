plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val defaultChannelName = providers.gradleProperty("SE_NOTI_DEFAULT_CHANNEL_NAME").orElse("sell6868").get()
val defaultClientId = providers.gradleProperty("SE_NOTI_DEFAULT_CLIENT_ID").orElse("SE-APP").get()
val defaultCustomerIdValue = providers.gradleProperty("SE_NOTI_DEFAULT_CUSTOMER_ID_VALUE").orElse("SE2029").get()
val ablyApiKey = providers.gradleProperty("SE_NOTI_ABLY_API_KEY").orElse("").get()
val customApiUrl = providers.gradleProperty("SE_NOTI_CUSTOM_API_URL").orElse("").get()
val defaultPushEnabled = providers.gradleProperty("SE_NOTI_DEFAULT_PUSH_ABLY_ENABLED").orElse("true").get().toBoolean()
val defaultPushApiEnabled = providers.gradleProperty("SE_NOTI_DEFAULT_PUSH_API_ENABLED").orElse("false").get().toBoolean()

android {
    namespace = "com.senoti.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.senoti.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.2"
        buildConfigField("String", "DEFAULT_CHANNEL_NAME", "\"$defaultChannelName\"")
        buildConfigField("String", "DEFAULT_CLIENT_ID", "\"$defaultClientId\"")
        buildConfigField("String", "DEFAULT_CUSTOMER_ID_VALUE", "\"$defaultCustomerIdValue\"")
        buildConfigField("String", "ABLY_API_KEY", "\"$ablyApiKey\"")
        buildConfigField("String", "CUSTOM_API_URL", "\"$customApiUrl\"")
        buildConfigField("boolean", "DEFAULT_PUSH_ENABLED", defaultPushEnabled.toString())
        buildConfigField("boolean", "DEFAULT_PUSH_API_ENABLED", defaultPushApiEnabled.toString())
    }

    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}

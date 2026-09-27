plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ru.vsm.trainer"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "ru.vsm.trainer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Эмулятор видит компьютер разработчика по адресу 10.0.2.2
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
        }
        release {
            // Только HTTPS; по умолчанию — развёрнутый сервер, другой адрес: ./gradlew assembleRelease -PapiBaseUrl=https://…/
            val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "https://vsm-trainer.onrender.com/"
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Демо на эмуляторе: оптимизации release (R8, без отладки, профили Compose) и локальный backend.
        // В debug Compose заметно медленнее — плавность анимаций и новеллы показываем на этой сборке.
        create("demo") {
            initWith(getByName("release"))
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
        // APK для жюри и любого телефона: release против развёрнутого сервера (HTTPS), подписан отладочным
        // ключом — ставится без магазина. Ключ организации для публикации в репозитории не хранится
        create("hosted") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    // HTTP к эмулятору разрешён так же, как в debug
    sourceSets.getByName("demo").res.srcDirs("src/debug/res")

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
    testOptions {
        unitTests {
            // Robolectric: Compose UI-тесты и Android-классы в обычных JVM-тестах, без эмулятора
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

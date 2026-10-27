plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * URL opcional para recibir las opiniones de las pruebas (por ejemplo, un Google Apps Script
 * que las guarda en una hoja de cálculo). Se define en local.properties o con -PfeedbackUrl=...
 * Si está vacía, las opiniones solo se guardan en el teléfono y se exportan en CSV.
 */
val feedbackUrl: String = (project.findProperty("feedbackUrl") as String?)
    ?: rootProject.file("local.properties").takeIf { it.exists() }?.readLines()
        ?.firstOrNull { it.startsWith("feedbackUrl=") }?.substringAfter("=")?.trim()
    ?: ""

android {
    namespace = "com.floresvalencia.echoguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.floresvalencia.echoguard"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        buildConfigField("String", "FEEDBACK_URL", "\"$feedbackUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Los avisos no detienen la compilación; los errores sí.
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

ksp {
    // Esquema de la base de datos versionado en git (sirve para planear migraciones).
    arg("room.schemaLocation", "$projectDir/schemas")
}

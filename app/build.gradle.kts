import java.util.Properties
import com.gbo.build.ValidateReleaseConfigurationTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Validation APKs omit machine-local endpoints/credentials without editing their files.
val gboValidationBuild = providers.gradleProperty("gboValidationBuild").orNull == "true"
// Explicit Debug test override; does not edit private settings or enable remote capability.
val gboPhotoNamesV2Test = providers.gradleProperty("gboPhotoNamesV2Test").orNull == "true"
val driveProperties = Properties().apply {
    val config = rootProject.file("drive.local.properties")
    if (!gboValidationBuild && config.exists()) config.inputStream().use { load(it) }
}
val driveApiToken = driveProperties.getProperty("DRIVE_API_TOKEN", "")
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
val driveWebAppUrl = driveProperties.getProperty("DRIVE_WEB_APP_URL", "").trim()
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

val serverProperties = Properties().apply {
    val config = rootProject.file("server.local.properties")
    if (!gboValidationBuild && config.exists()) config.inputStream().use { load(it) }
}
fun localString(name: String) = serverProperties.getProperty(name, "")
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
fun localFlag(name: String) = serverProperties.getProperty(name, "false").toBoolean().toString()

// Public identity is pinned to the certificate verified in both historical field APKs.
val productionVersionCode = 16
val expectedReleaseCertSha256 = "0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315"
val releaseProperties = Properties().apply {
    val config = rootProject.file("keystore.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
fun releaseValue(name: String) = providers.environmentVariable(name).orNull
    ?: providers.gradleProperty(name).orNull ?: releaseProperties.getProperty(name, "")
val releaseStorePath = releaseValue("GBO_RELEASE_STORE_FILE")
val releaseStorePassword = releaseValue("GBO_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseValue("GBO_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseValue("GBO_RELEASE_KEY_PASSWORD")
val releaseStore = releaseStorePath.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
val releaseSigningReady = releaseStore?.isFile == true &&
    listOf(releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it.isNotBlank() }
val releaseConfigurationCheck = tasks.register<ValidateReleaseConfigurationTask>("validateReleaseConfiguration") {
    validationBuild.set(gboValidationBuild)
    signingConfigured.set(releaseSigningReady)
    userServiceConfigured.set(serverProperties.getProperty("SERVER_BASE_URL", "").startsWith("https://"))
    driveConfigured.set(driveProperties.getProperty("DRIVE_WEB_APP_URL", "").startsWith("https://") &&
        driveProperties.getProperty("DRIVE_API_TOKEN", "").isNotBlank())
    expectedCertificateSha256.set(expectedReleaseCertSha256)
    // Leave absent files unset so the existing missing-signing guard reports the failure.
    releaseStore?.takeIf { it.isFile }?.let { keystoreFile.set(it) }
    storePassword.set(releaseStorePassword)
    keyAlias.set(releaseKeyAlias)
    keyPassword.set(releaseKeyPassword)
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(releaseConfigurationCheck) }

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

val scapTemplateAssets = tasks.register<Copy>("scapTemplateAssets") {
    from(rootProject.file("docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx")) { rename { "scap_template.xlsx" } }
    from(rootProject.file("tools/scap_field_map.json"))
    from(rootProject.file("docs/reference/FICHA SCAP - GBO modelo - LOSA.xlsx")) { rename { "scap_slab_template.xlsx" } }
    from(rootProject.file("docs/reference/FICHA SCAP - GBO modelo - MODULAR.xlsx")) { rename { "scap_modular_template.xlsx" } }
    from(rootProject.file("tools/scap_slab_field_map.json"))
    from(rootProject.file("tools/scap_modular_field_map.json"))
    into(layout.buildDirectory.dir("generated/scapAssets"))
}
tasks.named("preBuild").configure { dependsOn(scapTemplateAssets) }

android {
    namespace = "com.tuempresa.inventariovial"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        buildConfigField("String", "DRIVE_API_TOKEN", "\"$driveApiToken\"")
        buildConfigField("String", "DRIVE_WEB_APP_URL", "\"$driveWebAppUrl\"")
        buildConfigField("String", "SERVER_BASE_URL", "\"${localString("SERVER_BASE_URL")}\"")
        for (flag in listOf("ACCESS_CONTROL_ENABLED", "AI_ENABLED", "AI_SCAP_ENABLED", "SIC17A_ENABLED", "SIC17B_ENABLED", "SIC18A_ENABLED", "GBO_PHOTO_NUMBERS_ENABLED", "GBO_VIDEO_REMOTE_ENABLED", "GBO_PHOTO_NAMES_V2_ENABLED")) {
            buildConfigField("boolean", flag, localFlag(flag))
        }
        applicationId = "com.tuempresa.inventariovial"
        minSdk = 26
        targetSdk = 37
        versionCode = productionVersionCode
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("gboHistoricalRelease") {
                storeFile = releaseStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if(gboPhotoNamesV2Test) buildConfigField("boolean", "GBO_PHOTO_NAMES_V2_ENABLED", "true")
        }
        release {
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("gboHistoricalRelease")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
    sourceSets.getByName("main").assets.directories.add(layout.buildDirectory.dir("generated/scapAssets").get().asFile.path)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.json:json:20250517")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // =========================================================
    // ROOM
    // =========================================================

    val roomVersion = "2.8.5"

    implementation(
        "androidx.room:room-runtime:$roomVersion"
    )

    implementation(
        "androidx.room:room-ktx:$roomVersion"
    )

    ksp(
        "androidx.room:room-compiler:$roomVersion"
    )


    // =========================================================
    // VIEWMODEL
    // =========================================================

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0"
    )
}

// Gate the complete signed APK with official Android Build Tools before distribution.
val verifyReleaseIdentity = tasks.register<Exec>("verifyReleaseIdentity") {
    dependsOn("packageRelease")
    workingDir(rootProject.projectDir)
    val sdkProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    val sdkPath = providers.environmentVariable("ANDROID_HOME").orNull
        ?: sdkProperties.getProperty("sdk.dir", "")
    commandLine("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
        rootProject.file("tools/apk_identity_audit.ps1").absolutePath,
        "-Apk", layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile.absolutePath,
        "-SdkPath", sdkPath, "-JavaHome", System.getProperty("java.home"),
        "-ExpectedPackage", "com.tuempresa.inventariovial", "-ExpectedVersionCode", productionVersionCode.toString(),
        "-ExpectedCertificateSha256", expectedReleaseCertSha256, "-RequireProduction")
}
tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(verifyReleaseIdentity) }

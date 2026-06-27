import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.net.URL

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.ledemoderne.kpqtx"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Download app icon during configuration phase if it doesn't exist
val iconFile = file("src/main/res/drawable/gemini_generated_icon.png")
if (!iconFile.exists()) {
    iconFile.parentFile.mkdirs()
    try {
        println("Downloading app icon during configuration...")
        val url = URL("https://random-sapphire-hyz6surw.edgeone.dev/Gemini_Generated_Image_.png")
        iconFile.writeBytes(url.readBytes())
        println("App icon downloaded successfully: ${iconFile.length()} bytes")
    } catch (e: Exception) {
        println("Could not download app icon, using fallback: ${e.message}")
        val srcFile = file("src/main/res/drawable/ic_launcher_foreground.xml")
        if (srcFile.exists()) {
            iconFile.writeBytes(srcFile.readBytes())
        } else {
            iconFile.writeText("") // simple fallback
        }
    }
}

// Ensure only xxxhdpi has the large physical launcher icons to optimize APK size
val otherDensities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi")
for (density in otherDensities) {
    file("src/main/res/mipmap-$density/ic_launcher.png").delete()
    file("src/main/res/mipmap-$density/ic_launcher_round.png").delete()
}

val densities = listOf("xxxhdpi")
if (iconFile.exists() && iconFile.length() > 0) {
    for (density in densities) {
        val dir = file("src/main/res/mipmap-$density")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val launcherFile = file("src/main/res/mipmap-$density/ic_launcher.png")
        val launcherRoundFile = file("src/main/res/mipmap-$density/ic_launcher_round.png")
        
        // Copy if not exists or different size
        if (!launcherFile.exists() || launcherFile.length() != iconFile.length()) {
            println("Copying icon to $density/ic_launcher.png")
            iconFile.copyTo(launcherFile, overwrite = true)
        }
        if (!launcherRoundFile.exists() || launcherRoundFile.length() != iconFile.length()) {
            println("Copying icon to $density/ic_launcher_round.png")
            iconFile.copyTo(launcherRoundFile, overwrite = true)
        }
    }
}

tasks.register("copyApk") {
    dependsOn("assembleDebug")
    doLast {
        val apkFile = file("build/outputs/apk/debug/app-debug.apk")
        if (!apkFile.exists()) {
            throw GradleException("Source APK does not exist!")
        }
        
        val buildOutputsDir = file("../.build-outputs")
        buildOutputsDir.mkdirs()
        val buildOutputsApk = file("../.build-outputs/app-debug.apk")
        if (buildOutputsApk.exists()) {
            buildOutputsApk.delete()
        }
        apkFile.copyTo(buildOutputsApk, overwrite = true)
        println("Copied APK to ${buildOutputsApk.absolutePath}, size: ${buildOutputsApk.length()} bytes")
        
        val apkDownloadDir = file("../APK_DOWNLOAD")
        apkDownloadDir.mkdirs()
        val apkDownloadApk = file("../APK_DOWNLOAD/app-debug.apk")
        if (apkDownloadApk.exists()) {
            apkDownloadApk.delete()
        }
        apkFile.copyTo(apkDownloadApk, overwrite = true)
        println("Copied APK to ${apkDownloadApk.absolutePath}, size: ${apkDownloadApk.length()} bytes")
        
        // Cleanup the temporary apk files if they exist
        file("../.build-outputs/temp-app-debug.apk").delete()
        file("../APK_DOWNLOAD/temp-app-debug.apk").delete()
    }
}

tasks.register("checkFiles") {
    doLast {
        val rootDir = file("..")
        println("Root dir: ${rootDir.absolutePath}")
        rootDir.listFiles()?.forEach { f ->
            println("Root child: ${f.name} - ${if (f.isDirectory) "dir" else "${f.length()} bytes"}")
            if (f.isDirectory && (f.name == "APK_DOWNLOAD" || f.name == ".build-outputs")) {
                f.listFiles()?.forEach { sub ->
                    println("  Sub child: ${sub.name} - ${sub.length()} bytes")
                }
            }
        }
        
        // Find large files in src
        println("Large files in src:")
        file("src").walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 500000) {
                println("  ${f.absolutePath.substringAfter("app/")} - ${f.length()} bytes")
            }
        }
    }
}



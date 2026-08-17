import java.util.Base64
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.iptvplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.iptvplayer"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

val prepareSuppliedArtwork = tasks.register("prepareSuppliedArtwork") {
    inputs.file(layout.projectDirectory.file("src/main/assets/iptv_channels_photo.webp.b64"))
    inputs.file(layout.projectDirectory.file("src/main/assets/movies_photo.webp.b64"))
    outputs.files(
        layout.projectDirectory.file("src/main/res/drawable-nodpi/iptv_channels_photo.webp"),
        layout.projectDirectory.file("src/main/res/drawable-nodpi/movies_photo.webp")
    )
    doLast {
        val assetsDir = file("src/main/assets")
        val drawableDir = file("src/main/res/drawable-nodpi")
        drawableDir.mkdirs()

        mapOf(
            "iptv_channels_photo.webp.b64" to "iptv_channels_photo.webp",
            "movies_photo.webp.b64" to "movies_photo.webp"
        ).forEach { (sourceName, targetName) ->
            val encoded = File(assetsDir, sourceName).readText().trim()
            File(drawableDir, targetName).writeBytes(Base64.getDecoder().decode(encoded))
        }

        val source = file("src/main/java/com/example/iptvplayer/MainActivity.kt")
        val text = source.readText()
        val old = """        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.home_movies_icon),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            ThreeDHomeIcon(kind, Modifier.size(94.dp))
        }"""
        val new = """        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.movies_photo),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            Image(
                painter = painterResource(R.drawable.iptv_channels_photo),
                contentDescription = "ТВ каналы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        }"""
        if (!text.contains(old)) error("HomeBtn block not found")
        source.writeText(text.replace(old, new, 1))
    }
}

tasks.matching { it.name == "preBuild" || it.name.endsWith("Kotlin") || it.name.contains("Kotlin") }.configureEach {
    dependsOn(prepareSuppliedArtwork)
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.4")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
}

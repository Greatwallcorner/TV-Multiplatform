import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.corner.MainKt"

        buildTypes.release.proguard{
//            obfuscate.set(true)
            isEnabled.set(true)
            configurationFiles.from(project.file("rules.pro"))
        }

        jvmArgs("-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Pkg)
            packageName = "TV"
            packageVersion = "1.0.2"
            modules(/*"java.instrument",*//* "java.management",*/ "java.net.http", "java.sql",
                "jdk.unsupported")
            println(project.layout.projectDirectory.dir("/"))
            if(System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")){
                appResourcesRootDir.set(project.file("resources/windows"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/TV-icon-s.png"))
                dirChooser = true
                upgradeUuid = "161FA5A0-A30B-4568-9E84-B3CD637CC8FE"
            }
        }

    }
}

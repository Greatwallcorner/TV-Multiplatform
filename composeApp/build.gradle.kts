import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
//    id("java")
}

room {
    schemaDirectory("$projectDir/src/commonMain/schemas")
}

dependencies {
//    implementation("io.ktor:ktor-server-cors:3.1.2")
    ksp(libs.roomCompiler)
}


kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                val ktorVer = "3.1.2"
                val logbackVer = "1.3.14"
                val hutoolVer = "5.8.27"
//              val kotlinVersion = extra["kotlin.version"] as String
//              implementation(project(":CatVod"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.materialIconsExtended)
//              implementation(libs.roomCompiler)

                // room database access
                implementation(libs.roomRuntime)
                implementation(libs.roomGuava)
                implementation(libs.roomKtx)
                implementation(libs.roomBundled)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.navigation.compose)

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
//              implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")

                // tool
                api("com.google.guava:guava:31.1-jre")
                implementation("cn.hutool:hutool-all:$hutoolVer")

                //DI
                api("io.insert-koin:koin-core:3.5.3")
                api("io.insert-koin:koin-test:3.5.3")

                // spider depend on
                api("org.json:json:20231013")
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("com.github.lookfirst:sardine:5.10")
                implementation("cn.wanghaomiao:JsoupXpath:2.5.1")
                implementation("org.jsoup:jsoup:1.15.3")
                implementation("com.google.zxing:core:3.3.0")
                implementation("org.nanohttpd:nanohttpd:2.3.1")
                implementation("com.github.luben:zstd-jni:1.5.7-4")

                //ktor http server
                implementation("io.ktor:ktor-server-core:$ktorVer")
                implementation("io.ktor:ktor-server-netty:$ktorVer")
//                implementation("io.ktor:ktor-server-status-pages:$ktorVer")
                implementation("io.ktor:ktor-server-cors:$ktorVer")
                implementation("io.ktor:ktor-server-default-headers:$ktorVer")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVer")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVer")
                implementation("io.ktor:ktor-server-swagger:$ktorVer")
                implementation("io.ktor:ktor-client-core:$ktorVer")
                implementation("io.ktor:ktor-client-okhttp:$ktorVer")

                // log
                implementation("ch.qos.logback:logback-classic:$logbackVer")
                // image-loader
                api(libs.image.loader)
                // optional - Moko Resources Decoder
//                api("io.github.qdsfdhvh:image-loader-extension-moko-resources:$imageLoader")

                api(project.dependencies.platform("com.squareup.okhttp3:okhttp-bom:5.0.0-alpha.14"))
                api("com.squareup.okhttp3:okhttp")
                api("com.squareup.okhttp3:okhttp-dnsoverhttps")


                // DLNA
                implementation(libs.jupnp.bom.compile)
                implementation(libs.jupnp.support)
                implementation(libs.jupnp.osgi)
                implementation("org.apache.logging.log4j:log4j-api:2.20.0")

                //web-player
                implementation("org.java-websocket:Java-WebSocket:1.6.0")

//              implementation(libs.jupnp)
//              implementation(libs.jetty.servlet)
//              implementation(libs.jetty.server)
//              implementation(libs.jetty.client)
//              implementation(project(":Upnp"))

//              api("com.arkivanov.decompose:decompose:3.3.0")
//              api("com.arkivanov.decompose:extensions-compose:3.3.0")

//              Add the dependency, typically under the commonMain source set

//              api("com.arkivanov.essenty:lifecycle:2.5.0")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }

        }
        val desktopMain by getting{
            dependencies {
                implementation(compose.desktop.currentOs)
                // Player
                implementation(libs.vlcj)
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
//            obfuscate.set(true)
            isEnabled.set(true)
            version.set("7.7.0")
            configurationFiles.from(project.file("src/desktopMain/rules.pro"))
        }

//        jvmArgs("-Dfile.encoding=UTF-8")
        jvmArgs("-Dsun.net.http.allowRestrictedHeaders=true")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LumenTV"
            packageVersion = libs.versions.app.version.get()
            vendor = "LumenTV Compose"

            modules(
                "java.management",
                "java.net.http",
                "jdk.unsupported",
                "java.naming",
                "java.base",
                "java.sql"
            )
            val dir = project.layout.projectDirectory.dir("src/desktopMain/appResources")
            println(dir)
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/desktopMain/appResources"))
//            app icons https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution#app-icon
            windows {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon-s.ico"))
                dirChooser = true
                upgradeUuid = "161FA5A0-A30B-4568-9E84-B3CD637CC8FE"
            }

            linux {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/TV-icon-s.png"))
            }

            macOS {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.icns"))
            }

        }

    }
}

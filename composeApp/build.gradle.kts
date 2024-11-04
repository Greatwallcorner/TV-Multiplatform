import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.sqldelight)
//    alias(libs.plugins.conveyor)
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.corner.database")
        }
    }
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            val ktorVer = "3.0.1"
            val logbackVer = "1.3.14"
            val imageLoader = "1.8.1"
            val hutoolVer = "5.8.27"
//            val kotlinVersion = extra["kotlin.version"] as String
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

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

            //ktor http server
            implementation("io.ktor:ktor-server-core:$ktorVer")
            implementation("io.ktor:ktor-server-netty:$ktorVer")
//                implementation("io.ktor:ktor-server-status-pages:$ktorVer")
            implementation("io.ktor:ktor-server-default-headers:$ktorVer")
            implementation("io.ktor:ktor-server-content-negotiation:$ktorVer")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVer")
            implementation("io.ktor:ktor-server-swagger:$ktorVer")
            implementation("io.ktor:ktor-client-core:$ktorVer")
            implementation("io.ktor:ktor-client-okhttp:$ktorVer")

            // log
            implementation("ch.qos.logback:logback-classic:$logbackVer")
            // image-loader
            api("io.github.qdsfdhvh:image-loader:$imageLoader")
            // optional - Moko Resources Decoder
//                api("io.github.qdsfdhvh:image-loader-extension-moko-resources:$imageLoader")

            api(project.dependencies.platform("com.squareup.okhttp3:okhttp-bom:5.0.0-alpha.14"))
            api("com.squareup.okhttp3:okhttp")
            api("com.squareup.okhttp3:okhttp-dnsoverhttps")


            // DLNA
            implementation("org.jupnp:org.jupnp:2.7.1")
            implementation("org.jupnp:org.jupnp.support:2.7.1")
//            // https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api
//            implementation("javax.servlet:javax.servlet-api:4.0.1")
//            // https://mvnrepository.com/artifact/org.eclipse.jetty.ee10/jetty-ee10-servlet
//            implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.0.14")
//            // https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-server
//            implementation("org.eclipse.jetty:jetty-server:12.0.14")
//            // https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-client
//            implementation("org.eclipse.jetty:jetty-client:12.0.14")

            api("javax.servlet:javax.servlet-api:4.0.1")
            // https://mvnrepository.com/artifact/org.eclipse.jetty.ee10/jetty-ee10-servlet
            api("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.0.14")
            // https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-server
            api("org.eclipse.jetty:jetty-server:12.0.14")
            // https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-client
            api("org.eclipse.jetty:jetty-client:12.0.14")
            // https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-servlet
            api("org.eclipse.jetty:jetty-servlet:11.0.24")






            api("com.arkivanov.decompose:decompose:2.2.2")
            api("com.arkivanov.decompose:extensions-compose-jetbrains:2.2.2")
            // Add the dependency, typically under the commonMain source set
            api("com.arkivanov.essenty:lifecycle:1.3.0")
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            // Player
            implementation(libs.vlcj)
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
//            obfuscate.set(true)
            isEnabled.set(true)
            version.set("7.4.0")
            configurationFiles.from(project.file("src/desktopMain/rules.pro"))
        }

        jvmArgs("-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TV"
            packageVersion = libs.versions.app.version.get()
            vendor = "TV Multiplatform"

            modules(
                "java.compiler",
                "java.instrument",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.rmi",
                "java.security.jgss",
                "java.sql",
                "jdk.httpserver",
                "jdk.unsupported",
            )
            val dir = project.layout.projectDirectory.dir("src/desktopMain/resources/res")
            println(dir)
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/desktopMain/resources/res"))
//            app icons https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution#app-icon
            windows {
                iconFile.set(project.file("src/commonMain/resources/pic/icon-s.ico"))
                dirChooser = true
                upgradeUuid = "161FA5A0-A30B-4568-9E84-B3CD637CC8FE"
            }

            linux {
                iconFile.set(project.file("src/commonMain/resources/pic/TV-icon-s.png"))
            }

            macOS {
                iconFile.set(project.file("src/commonMain/resources/pic/icon.icns"))
            }

        }

    }
}

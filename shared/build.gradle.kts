plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.9.20"
    id("app.cash.sqldelight") version "2.0.0"
//    id("io.ktor.plugin") version "2.3.7"
}


sqldelight {
    databases {
        create("Database") {
            packageName.set("com.corner.database")
        }
    }
}

kotlin {
    androidTarget()

    jvm("desktop")

    sourceSets {
        val ktorVer = "2.3.8"
        val logbackVer = "1.3.14"
        val imageLoader = "1.7.4"
        val kotlinVersion = extra["kotlin.version"] as String
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class) implementation(compose.components.resources)
                implementation(compose.uiTooling)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

//                // tool
                api("com.google.guava:guava:31.1-jre")

                //DI
                api("io.insert-koin:koin-core:3.5.3")
                api("io.insert-koin:koin-test:3.5.3")

                // spider depend on
                api("org.json:json:20231013")
//                implementation("org.apache.commons:commons-lang3:3.14.0")
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
                implementation("io.ktor:ktor-client-cio:$ktorVer")

                // log
                implementation("ch.qos.logback:logback-classic:$logbackVer")
                // image-loader
                api("io.github.qdsfdhvh:image-loader:$imageLoader")
                // optional - Moko Resources Decoder
//                api("io.github.qdsfdhvh:image-loader-extension-moko-resources:$imageLoader")

                api(project.dependencies.platform("com.squareup.okhttp3:okhttp-bom:5.0.0-alpha.12"))
                api("com.squareup.okhttp3:okhttp")
                api("com.squareup.okhttp3:okhttp-dnsoverhttps")


                // DLNA
                implementation("org.jupnp:org.jupnp:2.7.1")
                implementation("org.jupnp:org.jupnp.support:2.7.1")

                api("com.arkivanov.decompose:decompose:2.2.2")
                api("com.arkivanov.decompose:extensions-compose-jetbrains:2.2.2")
                // Add the dependency, typically under the commonMain source set
                api("com.arkivanov.essenty:lifecycle:1.3.0")

            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.8.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
                api("io.insert-koin:koin-androidx-compose:3.5.3")
            }
        }
//        val iosX64Main by getting
//        val iosArm64Main by getting
//        val iosSimulatorArm64Main by getting
//        val iosMain by creating {
//            dependsOn(commonMain)
//            iosX64Main.dependsOn(this)
//            iosArm64Main.dependsOn(this)
//            iosSimulatorArm64Main.dependsOn(this)
//        }
        val desktopMain by getting {
            JavaVersion.VERSION_17
            dependencies {
                implementation(compose.desktop.common)
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
//                implementation("org.nanohttpd:nanohttpd:2.3.1")
                // Player
                implementation("uk.co.caprica:vlcj:4.8.2")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
                implementation("io.mockk:mockk:1.12.5")
                // kotest
                implementation("io.kotest:kotest-runner-junit5:5.3.1")
                implementation("io.kotest:kotest-assertions-core:5.3.1")
                implementation("io.kotest:kotest-property:5.3.1")
                implementation("org.junit.jupiter:junit-jupiter:5.9.2")
                implementation("org.junit.vintage:junit-vintage-engine:5.9.2")
//                // Player
//                implementation("uk.co.caprica:vlcj:4.8.2")
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.myapplication.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

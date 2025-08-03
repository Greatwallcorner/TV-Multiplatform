rootProject.name = "LumenTV-Compose"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
//        特殊网络环境下使用第三方镜像库
//        maven { url = uri("https://maven.aliyun.com/repository/public/") }
//        maven { url = uri("https://maven.aliyun.com/repositories/jcenter") }
//        maven { url = uri("https://maven.aliyun.com/repositories/google") }
//        maven { url = uri("https://maven.aliyun.com/repositories/central") }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven("https://maven.hq.hydraulic.software")
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.1.20"
        id("com.google.devtools.ksp") version "2.1.10-1.0.29"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
//        特殊网络环境下使用第三方镜像库
//        maven { url = uri("https://maven.aliyun.com/repository/public/") }
//        maven { url = uri("https://maven.aliyun.com/repositories/jcenter") }
//        maven { url = uri("https://maven.aliyun.com/repositories/google") }
//        maven { url = uri("https://maven.aliyun.com/repositories/central") }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":Web-Player")
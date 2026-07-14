pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.0.21"
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "agvehicle-sdk"

// core = 기반(연결·제네릭·카탈로그). 나머지는 신호 도메인별 모듈(core 위에 얹힘).
include(":core", ":hitch", ":engine")

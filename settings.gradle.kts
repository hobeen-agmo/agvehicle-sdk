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

// core = 기반(연결·제네릭·카탈로그, 코루틴 의존 X).
// imu/engine/hitch = 신호 도메인별 콜백 모듈. flow = 옵션(코루틴, Flow API).
include(":core", ":hitch", ":engine", ":imu", ":vehicle", ":flow")

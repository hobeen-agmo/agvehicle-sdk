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

// 표준/플랫폼 공통: core=기반, engine/hitch/imu/vehicle=콜백 모듈, flow=옵션,
//   gpio=플랫폼 하드웨어 IO(범용 능력, 제조사 고유 아님).
// AGMO 제조사 고유(proprietary): oem-* 접두사로 구분. docs/sdk-conventions.md 참조.
//   oem-tractor(커스텀 트랙터) / oem-steer(조향모터) / oem-spreader(살포기) / oem-imu(자이로).
include(":core", ":hitch", ":engine", ":imu", ":vehicle", ":flow", ":gpio", ":genericio",
        ":oem-tractor", ":oem-steer", ":oem-spreader", ":oem-imu")

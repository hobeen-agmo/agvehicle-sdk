// oem-spreader — AGMO RDA_Spreader 도메인 (살포기/작업기: 살포율·게이트·섹션 읽기 + 살포율 제어).
// 🏭 AGMO 제조사 고유(proprietary) 모듈 — 표준 아님. core 위 타입 파사드. oem-tractor와 같은 구조.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.oem.spreader"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    publishing { singleVariant("release") }
}

dependencies {
    api(project(":core"))
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

// oem-tractor — AGMO Customized Tractor 도메인 (읽기 FNR/변속/PTO/유압/ACC + 제어 4WD/AutoLift/히치).
// 🏭 AGMO 제조사 고유(proprietary) 모듈 — 표준 아님. core 위 타입 파사드. hitch/vehicle과 같은 구조.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.oem.tractor"
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
    api(project(":core"))   // api: 앱이 core 타입(ControlSession 등)도 함께 본다

    testImplementation(kotlin("test"))
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

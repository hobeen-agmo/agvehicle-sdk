// oem-steer — AGMO SteerMotor 도메인 (조향각·모터 상태 읽기 + 조향 명령).
// 🏭 AGMO 제조사 고유(proprietary) 모듈 — 표준 아님. core 위 타입 파사드. hitch/tractor와 같은 구조.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.oem.steer"
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

    testImplementation(kotlin("test"))
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

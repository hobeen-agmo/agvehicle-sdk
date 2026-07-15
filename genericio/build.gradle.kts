// genericio — 범용 CAN IO 보드 도메인(아날로그/디지털 입력, PWM 출력). 표준/플랫폼 계열.
// 범용 하드웨어 능력(제조사 CAN 고유 아님). core 위 타입 파사드. engine 패턴.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.genericio"
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

// gpio — GPIO 도메인 모듈 (플랫폼 하드웨어 IO: 아날로그입력·디지털입력·디지털출력).
// 표준 계열(제조사 CAN 고유 아님, 범용 하드웨어 능력). core 위 타입 파사드. engine 패턴.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.gpio"
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

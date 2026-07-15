// oem-imu — AGMO 제조사 고유 IMU/자이로 (Allynav_R70 자이로 proprietary + MTLT305 설정 PGN).
// 🏭 AGMO 제조사 고유(proprietary) 모듈 — 표준 아님.
//   표준 자세 데이터(각도/각속도/가속도, Aceinna MTLT305 데이터 PGN)는 표준 :imu 모듈이
//   담당한다. 이 모듈은 표준으로 커버되지 않는 제조사 고유 자이로·설정만 다룬다(섞지 않음).
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.oem.imu"
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

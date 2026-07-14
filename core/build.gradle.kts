// core — SDK 기반: bindService 연결 + 제네릭 구독/제어 + 카탈로그.
// 도메인 모듈(hitch/engine/…)이 이 위에 얹힌다. AIDL 정본이 여기 있다.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.sdk"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures { aidl = true }   // src/main/aidl/**/*.aidl → Binder 스텁 생성
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    publishing { singleVariant("release") }
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

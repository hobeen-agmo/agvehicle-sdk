// flow — 신호를 Kotlin Flow로 받는 옵션 모듈. 코루틴 의존은 여기에만 격리된다
// (콜백 코어/도메인 모듈은 코루틴 없이 돈다 — Room의 room-ktx 같은 분리).
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.flow"
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
    // 도메인 모델(키·from)을 재사용 — Flow 조립에 필요
    api(project(":imu"))
    api(project(":engine"))
    api(project(":hitch"))
    // 이 모듈만 코루틴 의존 (callbackFlow/Flow)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

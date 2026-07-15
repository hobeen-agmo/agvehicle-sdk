// tractor — AGMO Customized Tractor 도메인 모듈 (읽기 FNR/변속/PTO/유압/ACC + 제어 4WD/AutoLift/히치).
// core 위에 타입 있는 파사드를 얹는다. hitch/vehicle 모듈과 같은 구조.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.tractor"
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
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

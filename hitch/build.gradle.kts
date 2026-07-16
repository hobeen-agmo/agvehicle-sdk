// hitch — 히치 제어 도메인 모듈. core 위에 타입 있는 파사드를 얹는다.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.hitch"
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

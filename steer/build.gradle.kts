// steer — AGMO SteerMotor 도메인 모듈 (조향각·모터 상태 읽기 + 조향 명령).
// core 위에 타입 있는 파사드를 얹는다. hitch/tractor 모듈과 같은 구조.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.steer"
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

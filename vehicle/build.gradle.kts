// vehicle — 홈 런처용 차량 상태 도메인 모듈(차속·PTO·배터리·DPF + 토글 제어). core 위.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.vehicle"
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

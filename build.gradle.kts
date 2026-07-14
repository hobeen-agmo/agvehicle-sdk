// agvehicle-sdk — 외부 앱 개발자용 SDK (AAR).
// AIDL이 이 저장소의 정본(source of truth) — 생성된 Binder 스텁이 AAR에 함께 실려
// 앱은 AIDL을 몰라도 IAgVehicle/IVehicleCallback을 바로 쓴다.
plugins {
    id("com.android.library") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("maven-publish")
}

android {
    namespace = "farm.agmo.vehicle.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 28          // Android 9+ (bindService·oneway 콜백 안정)
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        aidl = true          // src/main/aidl/**/*.aidl → Binder 스텁 생성
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    publishing { singleVariant("release") }
}

dependencies {
    // 의존성 0 — 순수 Android SDK만 사용(외부 앱이 부담 없이 넣을 수 있게)
}

// GitHub Packages(Maven)로 배포:  ./gradlew publish
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "farm.agmo.vehicle"
            artifactId = "agvehicle-sdk"
            version = "0.1.0"
            afterEvaluate { from(components["release"]) }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hobeen-agmo/agvehicle-sdk")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

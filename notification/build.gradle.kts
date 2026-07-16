// notification — 앱이 태블릿으로 알림을 보내는 도메인 모듈.
// 표준 Android 알림(NotificationManager + 레벨별 채널)을 post → 상단 상태바에 자동 표시되고,
// AgLauncher의 NotificationListenerService가 이를 미러링해 홈 종모양에도 같은 소스로 뜬다.
// core(신호 서비스) 의존 없음 — 알림 게시는 CAN 연결과 무관한 독립 능력이다.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "farm.agmo.vehicle.notification"
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
}

apply(from = "$rootDir/gradle/publish.gradle.kts")

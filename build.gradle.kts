// 루트 — 모듈 공통 설정을 subprojects로 한 곳에서 준다.
// 각 모듈(core/hitch/engine)은 자기 build.gradle.kts에서 android 블록만 최소로 둔다.
plugins {
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
}

subprojects {
    // 배포 좌표: farm.agmo.vehicle:<모듈명>:<버전>
    //   core   → farm.agmo.vehicle:core:0.1.0
    //   hitch  → farm.agmo.vehicle:hitch:0.1.0
    group = "farm.agmo.vehicle"
    version = "0.1.0"
}

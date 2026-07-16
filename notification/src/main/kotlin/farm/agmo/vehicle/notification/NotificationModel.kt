// NotificationModel.kt — 알림 도메인 모델 (순수 Kotlin, Android 무의존 — JVM 테스트 대상).
//
// 다른 도메인은 "CAN 메시지 하나 = 클래스 하나"지만, 알림은 앱→태블릿 방향 이벤트라
// 레벨(심각도) enum + 게시 스냅샷 data class + 연동 상수(KEYS 역할)로 구성한다.
package farm.agmo.vehicle.notification

/**
 * 알림 심각도. 각 레벨은 (a) 표준 Android 채널/중요도와 (b) 홈 종모양의 색·정렬에 1:1로 매핑된다.
 *   INFO     → 조용한 정보 (상태바에 쌓임)
 *   WARN     → 주의 (기본 중요도)
 *   CRITICAL → 즉시 대응 (최고 중요도, headline)
 */
enum class NotificationLevel {
    INFO, WARN, CRITICAL;

    companion object {
        /** 미지 문자열은 안전하게 INFO (extras 파싱용) — Quality.of 관례와 동일 */
        fun of(s: String?): NotificationLevel = entries.firstOrNull { it.name == s } ?: INFO
    }
}

/**
 * 게시된 알림 1건 — 홈 종모양이 표시에 쓰는 스냅샷.
 * SDK가 post한 표준 Android 알림을 AgLauncher의 NotificationListenerService가 미러링해
 * 이 형태로 조립한다(상태바와 단일 소스).
 */
data class AgNotification(
    val id: Int,
    val title: String,
    val message: String,
    val level: NotificationLevel,
    val whenMillis: Long,
    val sourcePackage: String? = null,
)

/**
 * 채널·extras·리스너 연동에 쓰는 공통 상수 (도메인 모듈의 KEYS 와 같은 역할).
 * AgLauncher의 리스너/종모양이 [EXTRA_LEVEL] 로 레벨을 정확히 복원한다.
 */
object NotificationKeys {
    const val CHANNEL_INFO = "agmo_info"
    const val CHANNEL_WARN = "agmo_warn"
    const val CHANNEL_CRITICAL = "agmo_critical"

    /** 표준 알림 extras 에 실어 보내는 레벨 이름 (NotificationLevel.name) */
    const val EXTRA_LEVEL = "farm.agmo.vehicle.notification.LEVEL"

    fun channelId(level: NotificationLevel): String = when (level) {
        NotificationLevel.INFO -> CHANNEL_INFO
        NotificationLevel.WARN -> CHANNEL_WARN
        NotificationLevel.CRITICAL -> CHANNEL_CRITICAL
    }
}

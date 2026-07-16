// AgNotifications.kt — 앱이 태블릿으로 알림을 보내는 심플 API.
//
//   AgNotifications.send(context, "RPM 경고", "엔진 회전수 2400 rpm 초과", NotificationLevel.WARN)
//
// 이 한 줄이 (a) 표준 Android 알림(NotificationManager + 레벨별 채널)을 post → 상단 상태바에
// 자동 표시되고, (b) 그 알림을 AgLauncher의 NotificationListenerService가 미러링해 홈 종모양에도
// 같은 소스로 뜬다. 즉 SDK는 표준 알림 하나만 post하고, 상태바와 종모양은 단일 소스로 일치한다.
package farm.agmo.vehicle.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

object AgNotifications {

    // post한 알림마다 고유 id — 같은 id면 갱신(교체), 다른 id면 별도로 쌓인다
    private val nextId = AtomicInteger(1000)

    /**
     * 알림 하나를 태블릿에 보낸다. 반환값은 이 알림의 id(취소·갱신에 사용).
     * @param level 심각도 — 레벨별 채널/중요도가 자동 선택된다(기본 INFO).
     */
    fun send(
        context: Context,
        title: String,
        message: String,
        level: NotificationLevel = NotificationLevel.INFO,
    ): Int {
        val ctx = context.applicationContext
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(nm)

        val id = nextId.getAndIncrement()
        // 종모양이 레벨을 정확히 복원하도록 extras 에 레벨을 실어 보낸다
        val extras = Bundle().apply { putString(NotificationKeys.EXTRA_LEVEL, level.name) }

        val notification = Notification.Builder(ctx, NotificationKeys.channelId(level))
            .setSmallIcon(android.R.drawable.stat_notify_more)  // 플랫폼 기본 아이콘(라이브러리 리소스 무의존)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .addExtras(extras)
            .apply { if (level == NotificationLevel.CRITICAL) setColorized(true) }
            .build()

        nm.notify(id, notification)
        return id
    }

    /** 이전에 보낸 알림 취소 (id는 send 반환값) */
    fun cancel(context: Context, id: Int) {
        val nm = context.applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id)
    }

    // minSdk 28 — 채널은 항상 필요. 이미 있으면 그대로 둔다(멱등).
    private fun ensureChannels(nm: NotificationManager) {
        listOf(
            Triple(NotificationKeys.CHANNEL_INFO, "Info", NotificationManager.IMPORTANCE_LOW),
            Triple(NotificationKeys.CHANNEL_WARN, "Warnings", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(NotificationKeys.CHANNEL_CRITICAL, "Critical", NotificationManager.IMPORTANCE_HIGH),
        ).forEach { (channelId, name, importance) ->
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(NotificationChannel(channelId, name, importance))
            }
        }
    }
}

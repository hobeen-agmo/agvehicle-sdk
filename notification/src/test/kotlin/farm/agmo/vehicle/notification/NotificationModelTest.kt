// NotificationModelTest.kt — 순수 로직 유닛테스트: NotificationLevel.of null/미지 방어 + 채널 매핑 전수.
package farm.agmo.vehicle.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationLevelTest {
    @Test fun of_allDocumentedNames_mapExactly() {
        assertEquals(NotificationLevel.INFO, NotificationLevel.of("INFO"))
        assertEquals(NotificationLevel.WARN, NotificationLevel.of("WARN"))
        assertEquals(NotificationLevel.CRITICAL, NotificationLevel.of("CRITICAL"))
    }

    @Test fun of_null_fallsBackToInfo() {
        assertEquals(NotificationLevel.INFO, NotificationLevel.of(null))
    }

    @Test fun of_unknownString_fallsBackToInfo() {
        assertEquals(NotificationLevel.INFO, NotificationLevel.of("GARBAGE"))
    }

    @Test fun of_emptyString_fallsBackToInfo() {
        assertEquals(NotificationLevel.INFO, NotificationLevel.of(""))
    }

    @Test fun of_lowercaseKnownName_isCaseSensitiveAndFallsBack() {
        assertEquals(NotificationLevel.INFO, NotificationLevel.of("warn"))
    }
}

class NotificationKeysTest {
    @Test fun channelId_mapsEachLevelToItsOwnChannel() {
        assertEquals(NotificationKeys.CHANNEL_INFO, NotificationKeys.channelId(NotificationLevel.INFO))
        assertEquals(NotificationKeys.CHANNEL_WARN, NotificationKeys.channelId(NotificationLevel.WARN))
        assertEquals(NotificationKeys.CHANNEL_CRITICAL, NotificationKeys.channelId(NotificationLevel.CRITICAL))
    }
}

// AgVehicleTest.kt — 순수 로직 유닛테스트: leading-edge 게이트 판정(shouldEmit) 경계값,
// subscribeMessage 콜백 병합(mergeLatest) 스냅샷 내용·할당 회피.
package farm.agmo.vehicle.sdk

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgVehicleShouldEmitTest {
    @Test fun shouldEmit_zeroElapsed_isBlocked() {
        assertFalse(AgVehicle.shouldEmit(lastMs = 1_000L, nowMs = 1_000L, sampleMs = 100L))
    }

    @Test fun shouldEmit_elapsedExactlySampleMs_isAllowed() {
        assertTrue(AgVehicle.shouldEmit(lastMs = 1_000L, nowMs = 1_100L, sampleMs = 100L))
    }

    @Test fun shouldEmit_elapsedSampleMsMinusOne_isBlocked() {
        assertFalse(AgVehicle.shouldEmit(lastMs = 1_000L, nowMs = 1_099L, sampleMs = 100L))
    }

    @Test fun shouldEmit_elapsedBeyondSampleMs_isAllowed() {
        assertTrue(AgVehicle.shouldEmit(lastMs = 1_000L, nowMs = 5_000L, sampleMs = 100L))
    }
}

class AgVehicleMergeLatestTest {
    @Test fun mergeLatest_allowTrue_returnsSnapshotWithAllAccumulatedKeys() {
        val latest = mutableMapOf("RPM" to 1200.0)
        val snapshot = AgVehicle.mergeLatest(latest, "LOAD", 55.0, allow = true)
        assertTrue(snapshot != null)
        assertTrue(snapshot == mapOf("RPM" to 1200.0, "LOAD" to 55.0))
    }

    @Test fun mergeLatest_allowFalse_updatesLatestButReturnsNull() {
        val latest = mutableMapOf("RPM" to 1200.0)
        val snapshot = AgVehicle.mergeLatest(latest, "LOAD", 55.0, allow = false)
        assertNull(snapshot)
        // 게이트가 닫혀도 최신값 누적은 항상 일어나야 함 — 다음 통과 시 최신 스냅샷에 반영
        assertTrue(latest == mapOf("RPM" to 1200.0, "LOAD" to 55.0))
    }

    @Test fun mergeLatest_nullNumber_doesNotOverwriteLatestButStillHonorsAllow() {
        val latest = mutableMapOf("RPM" to 1200.0)
        val snapshot = AgVehicle.mergeLatest(latest, "RPM", null, allow = true)
        assertTrue(snapshot == mapOf("RPM" to 1200.0))
    }

    @Test fun mergeLatest_snapshotIsIndependentCopy_notLiveViewOfLatest() {
        val latest = mutableMapOf("RPM" to 1200.0)
        val snapshot = AgVehicle.mergeLatest(latest, "RPM", 1200.0, allow = true)
        latest["RPM"] = 9999.0
        // 스냅샷은 delivery 시점의 값을 유지해야 함(콜백 계약: 그 시점의 최신값 맵)
        assertTrue(snapshot == mapOf("RPM" to 1200.0))
    }

    @Test fun mergeLatest_consecutiveSnapshots_areDistinctInstances() {
        val latest = mutableMapOf<String, Double>()
        val first = AgVehicle.mergeLatest(latest, "RPM", 100.0, allow = true)
        val second = AgVehicle.mergeLatest(latest, "RPM", 200.0, allow = true)
        assertTrue(first !== second)
        assertTrue(first == mapOf("RPM" to 100.0))
        assertTrue(second == mapOf("RPM" to 200.0))
    }
}

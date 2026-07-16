// AgVehicleTest.kt — 순수 로직 유닛테스트: leading-edge 게이트 판정(shouldEmit) 경계값.
package farm.agmo.vehicle.sdk

import kotlin.test.Test
import kotlin.test.assertFalse
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

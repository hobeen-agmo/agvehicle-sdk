// AutoLiftTest.kt — 순수 로직 유닛테스트: 자동 히치 상승 트리거 판정(shouldTrigger) 경계값.
package farm.agmo.vehicle.oem.tractor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoLiftShouldTriggerTest {
    @Test fun shouldTrigger_steerAtExactThreshold_isNotTriggered() {
        // 경계는 초과(>)만 트리거 — 정확히 임계값이면 미트리거
        assertFalse(AutoLift.shouldTrigger(steerRad = 0.35, reverse = false, onTurn = true, onReverse = false))
    }

    @Test fun shouldTrigger_steerJustAboveThreshold_isTriggered() {
        assertTrue(AutoLift.shouldTrigger(steerRad = 0.351, reverse = false, onTurn = true, onReverse = false))
    }

    @Test fun shouldTrigger_steerJustBelowThreshold_isNotTriggered() {
        assertFalse(AutoLift.shouldTrigger(steerRad = 0.349, reverse = false, onTurn = true, onReverse = false))
    }

    @Test fun shouldTrigger_negativeSteerBeyondThreshold_isTriggered() {
        // |steerRad| 기준이라 음수 조향각도 임계 초과면 트리거
        assertTrue(AutoLift.shouldTrigger(steerRad = -0.36, reverse = false, onTurn = true, onReverse = false))
    }

    @Test fun shouldTrigger_onTurnDisabled_ignoresSteerAngle() {
        assertFalse(AutoLift.shouldTrigger(steerRad = 1.0, reverse = false, onTurn = false, onReverse = false))
    }

    @Test fun shouldTrigger_reverseWithOnReverseEnabled_isTriggered() {
        assertTrue(AutoLift.shouldTrigger(steerRad = null, reverse = true, onTurn = false, onReverse = true))
    }

    @Test fun shouldTrigger_forwardWithOnReverseEnabled_isNotTriggered() {
        assertFalse(AutoLift.shouldTrigger(steerRad = null, reverse = false, onTurn = false, onReverse = true))
    }

    @Test fun shouldTrigger_onReverseDisabled_ignoresReverseGear() {
        assertFalse(AutoLift.shouldTrigger(steerRad = null, reverse = true, onTurn = false, onReverse = false))
    }

    @Test fun shouldTrigger_nullSteerRad_doesNotTriggerTurnCondition() {
        assertFalse(AutoLift.shouldTrigger(steerRad = null, reverse = false, onTurn = true, onReverse = false))
    }

    @Test fun shouldTrigger_bothConditionsMet_isTriggered() {
        assertTrue(AutoLift.shouldTrigger(steerRad = 1.0, reverse = true, onTurn = true, onReverse = true))
    }

    @Test fun shouldTrigger_neitherToggleEnabled_isNotTriggered() {
        assertFalse(AutoLift.shouldTrigger(steerRad = 1.0, reverse = true, onTurn = false, onReverse = false))
    }
}

// SteerModelTest.kt — 순수 로직 유닛테스트: SteerMotorStatus.fault OR 집계 전수 + from() 방어.
package farm.agmo.vehicle.oem.steer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SteerMotorStatusTest {
    private val allClear = SteerMotorStatus(
        hallFailure = false, canDisconnected = false, motorStalled = false, disabled = false,
        overvoltage = false, hardwareProtection = false, e2prom = false, undervoltage = false,
        overcurrent = false, modeFailure = false,
    )

    @Test fun fault_allFlagsFalse_isFalse() {
        assertFalse(allClear.fault)
    }

    @Test fun fault_hallFailureOnly_isTrue() {
        assertTrue(allClear.copy(hallFailure = true).fault)
    }

    @Test fun fault_canDisconnectedOnly_isTrue() {
        assertTrue(allClear.copy(canDisconnected = true).fault)
    }

    @Test fun fault_motorStalledOnly_isTrue() {
        assertTrue(allClear.copy(motorStalled = true).fault)
    }

    @Test fun fault_disabledOnly_isTrue() {
        assertTrue(allClear.copy(disabled = true).fault)
    }

    @Test fun fault_overvoltageOnly_isTrue() {
        assertTrue(allClear.copy(overvoltage = true).fault)
    }

    @Test fun fault_hardwareProtectionOnly_isTrue() {
        assertTrue(allClear.copy(hardwareProtection = true).fault)
    }

    @Test fun fault_e2promOnly_isTrue() {
        assertTrue(allClear.copy(e2prom = true).fault)
    }

    @Test fun fault_undervoltageOnly_isTrue() {
        assertTrue(allClear.copy(undervoltage = true).fault)
    }

    @Test fun fault_overcurrentOnly_isTrue() {
        assertTrue(allClear.copy(overcurrent = true).fault)
    }

    @Test fun fault_modeFailureOnly_isTrue() {
        assertTrue(allClear.copy(modeFailure = true).fault)
    }

    @Test fun from_allTenKeysPresent_assembles() {
        val v = mapOf(
            "keya_ky170:Heartbeat_HallFailure" to 0.0, "keya_ky170:Heartbeat_CANdisconnected" to 0.0,
            "keya_ky170:Heartbeat_MotorStalled" to 0.0, "keya_ky170:Heartbeat_Disabled" to 0.0,
            "keya_ky170:Heartbeat_Overvoltage" to 0.0, "keya_ky170:Heartbeat_HardwareProtection" to 0.0,
            "keya_ky170:Heartbeat_E2PROM" to 0.0, "keya_ky170:Heartbeat_Undervoltage" to 0.0,
            "keya_ky170:Heartbeat_Overcurrent" to 1.0, "keya_ky170:Heartbeat_ModeFailure" to 0.0,
        )
        val s = assertNotNull(SteerMotorStatus.from(v))
        assertTrue(s.overcurrent)
        assertTrue(s.fault)
    }

    @Test fun from_missingOneOfTenKeys_isNull() {
        val v = mapOf(
            "keya_ky170:Heartbeat_HallFailure" to 0.0, "keya_ky170:Heartbeat_CANdisconnected" to 0.0,
            "keya_ky170:Heartbeat_MotorStalled" to 0.0, "keya_ky170:Heartbeat_Disabled" to 0.0,
            "keya_ky170:Heartbeat_Overvoltage" to 0.0, "keya_ky170:Heartbeat_HardwareProtection" to 0.0,
            "keya_ky170:Heartbeat_E2PROM" to 0.0, "keya_ky170:Heartbeat_Undervoltage" to 0.0,
            "keya_ky170:Heartbeat_Overcurrent" to 0.0,
            // ModeFailure 누락
        )
        assertNull(SteerMotorStatus.from(v))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(SteerMotorStatus.from(emptyMap()))
    }
}

class SteerAngleTest {
    @Test fun from_keyPresent_assembles() {
        val a = assertNotNull(SteerAngle.from(mapOf(SteerAngle.KEY to 15.5)))
        kotlin.test.assertEquals(15.5, a.deg)
    }

    @Test fun from_missingKey_isNull() {
        assertNull(SteerAngle.from(emptyMap()))
    }
}

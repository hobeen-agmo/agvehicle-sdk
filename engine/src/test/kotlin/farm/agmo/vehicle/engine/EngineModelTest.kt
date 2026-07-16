// EngineModelTest.kt — 순수 로직 유닛테스트: 대표 메시지(단일/다중 신호) 조립의 null/빈 입력 방어.
package farm.agmo.vehicle.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class Eec1Test {
    @Test fun from_bothKeysPresent_assembles() {
        val e = assertNotNull(Eec1.from(mapOf("ENGRPM" to 1450.0, "ENGLOAD" to 52.0)))
        assertEquals(1450.0, e.rpm)
        assertEquals(52.0, e.loadPercent)
    }

    @Test fun from_missingRpm_isNull() {
        assertNull(Eec1.from(mapOf("ENGLOAD" to 52.0)))
    }

    @Test fun from_missingLoad_isNull() {
        assertNull(Eec1.from(mapOf("ENGRPM" to 1450.0)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(Eec1.from(emptyMap()))
    }
}

class EngineTemperatureTest {
    @Test fun from_singleKeyPresent_assembles() {
        val t = assertNotNull(EngineTemperature.from(mapOf("ENGTEMP" to 90.5)))
        assertEquals(90.5, t.coolantC)
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(EngineTemperature.from(emptyMap()))
    }
}

class EngineTorqueTest {
    @Test fun from_bothKeysPresent_assembles() {
        val t = assertNotNull(
            EngineTorque.from(mapOf("ENGTORQUE" to 60.0, "ENGTORQUE_DMD" to 65.0))
        )
        assertEquals(60.0, t.actualPercent)
        assertEquals(65.0, t.demandPercent)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(EngineTorque.from(mapOf("ENGTORQUE" to 60.0)))
    }
}

class Et1Test {
    private val full = mapOf("ENGTEMP" to 90.0, "FUELTEMP" to 30.0, "ENGOILTEMP" to 95.0)

    @Test fun from_allThreeKeysPresent_assembles() {
        val e = assertNotNull(Et1.from(full))
        assertEquals(90.0, e.coolantC)
        assertEquals(30.0, e.fuelC)
        assertEquals(95.0, e.oilC)
    }

    @Test fun from_missingOneOfThreeKeys_isNull() {
        assertNull(Et1.from(full - "ENGOILTEMP"))
    }
}

class Eflp1Test {
    private val full = mapOf(
        "FUELDLVP" to 1.0, "ENGOILLVL" to 2.0, "ENGOILP" to 3.0, "COOLANTP" to 4.0, "COOLANTLVL" to 5.0,
    )

    @Test fun from_allFiveKeysPresent_assembles() {
        val e = assertNotNull(Eflp1.from(full))
        assertEquals(1.0, e.fuelDeliveryKPa)
        assertEquals(2.0, e.oilLevelPercent)
        assertEquals(3.0, e.oilKPa)
        assertEquals(4.0, e.coolantKPa)
        assertEquals(5.0, e.coolantLevelPercent)
    }

    @Test fun from_missingOneOfFiveKeys_isNull() {
        assertNull(Eflp1.from(full - "COOLANTLVL"))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(Eflp1.from(emptyMap()))
    }
}

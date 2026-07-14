// Model.kt — 앱 개발자에게 노출되는 데이터 모델 (순수 Kotlin — Android 무의존, JVM 테스트 대상)
package farm.agmo.vehicle.sdk

/** 신호 품질 — 데몬 quality 토큰과 1:1. 미지 문자열은 안전하게 DISCONNECTED */
enum class Quality {
    OK, DISCONNECTED, IMPLAUSIBLE;

    companion object {
        fun of(s: String): Quality = entries.firstOrNull { it.name == s } ?: DISCONNECTED
    }
}

/**
 * 구독 신호의 값 1건.
 * text는 데몬이 보낸 그대로("52.4 %", "1450 rpm") — 표시용으로 바로 쓸 수 있고,
 * 계산용으로는 [number]/[unit]으로 분해해 쓴다.
 */
data class SignalValue(val key: String, val text: String, val quality: Quality) {
    /** 값의 수치 부분 (첫 토큰). 수치가 아니면 null */
    val number: Double? get() = text.substringBefore(' ').toDoubleOrNull()

    /** 값의 단위 부분 (수치 뒤). 없으면 null */
    val unit: String? get() = text.substringAfter(' ', "").ifEmpty { null }
}

/**
 * 카탈로그의 신호 메타 1건 — 데몬 .def 21컬럼과 1:1 (서비스 DefValidator와 같은 순서).
 * 앱은 이걸로 "무슨 신호가 있고, 주기·단위·쓰기 가능 여부가 뭔지"를 알아낸다.
 */
data class SignalMeta(
    val owner: String,        // "platform" 또는 정의한 앱의 appId
    val message: String,
    val bus: String,          // "isobus" | "vehicle"
    val idKind: String,       // "can" | "pgn"
    val id: Long,
    val access: String,       // "read" | "write"
    val updateMs: Int,
    val timeoutCount: Int,
    val healCount: Int,
    val byteOrder: String,
    val signal: String,
    val startBit: Int,
    val length: Int,
    val type: String?,
    val signed: Boolean,
    val resolution: Double,
    val offset: Double,
    val na: Long?,
    val npl: Long?,
    val safe: Long?,
    val unit: String?,
) {
    /** subscribe/acquire에 넣는 key. 내장(platform) 신호는 bare 이름, 외부는 "owner:signal" */
    val key: String get() = if (owner == "platform") signal else "$owner:$signal"

    val writable: Boolean get() = access == "write"
}

/** 카탈로그 라인(21컬럼 평문) 파서. 불량 라인은 null — 절대 throw 하지 않는다 */
object CatalogLine {
    fun parse(line: String): SignalMeta? {
        val t = line.trim().split(Regex("\\s+"))
        if (t.size != 21) return null
        fun hexOrNull(s: String): Long? =
            if (s == "-") null else s.removePrefix("0x").removePrefix("0X").toLongOrNull(16)
        return try {
            SignalMeta(
                owner = t[0], message = t[1], bus = t[2], idKind = t[3],
                id = t[4].removePrefix("0x").removePrefix("0X").toLong(16),
                access = t[5], updateMs = t[6].toInt(), timeoutCount = t[7].toInt(),
                healCount = t[8].toInt(), byteOrder = t[9],
                signal = t[10], startBit = t[11].toInt(), length = t[12].toInt(),
                type = t[13].takeIf { it != "-" }, signed = t[14] == "1",
                resolution = t[15].toDouble(), offset = t[16].toDouble(),
                na = hexOrNull(t[17]), npl = hexOrNull(t[18]), safe = hexOrNull(t[19]),
                unit = t[20].takeIf { it != "-" },
            )
        } catch (_: Exception) {
            null
        }
    }
}

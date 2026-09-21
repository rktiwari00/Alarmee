package app.wakewalk.data.local

import app.wakewalk.data.local.converter.Converters
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.model.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AlarmEntityTest {

    private val converters = Converters()

    @Test
    fun testQrCodeChallengeTypeConverter() {
        val converted = converters.fromChallengeType(ChallengeType.QR_CODE)
        assertEquals("QR_CODE", converted)
        val restored = converters.toChallengeType(converted)
        assertEquals(ChallengeType.QR_CODE, restored)
    }

    @Test
    fun testAlarmEntityWithQrCodePayload() {
        val entity = AlarmEntity(
            hour = 7,
            minute = 30,
            label = "Morning",
            isEnabled = true,
            repeatDaysMask = 0,
            challengeType = ChallengeType.QR_CODE,
            targetSteps = 20,
            qrCodePayload = "BARCODE_TOOTHPASTE_12345",
            qrCodeLabel = "Bathroom Sink"
        )
        assertEquals(ChallengeType.QR_CODE, entity.challengeType)
        assertEquals("BARCODE_TOOTHPASTE_12345", entity.qrCodePayload)
        assertEquals("Bathroom Sink", entity.qrCodeLabel)
        assertEquals(20, entity.targetSteps)
    }
}

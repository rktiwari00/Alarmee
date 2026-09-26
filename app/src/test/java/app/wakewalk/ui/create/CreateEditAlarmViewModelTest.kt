package app.wakewalk.ui.create

import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.repository.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditAlarmViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeRepository = object : AlarmRepository {
        var insertedAlarm: AlarmEntity? = null
        override fun getAllAlarmsFlow(): Flow<List<AlarmEntity>> = flowOf(emptyList())
        override suspend fun getEnabledAlarms(): List<AlarmEntity> = emptyList()
        override suspend fun getAlarmById(id: Long): AlarmEntity? = insertedAlarm
        override suspend fun insertAlarm(alarm: AlarmEntity): Long {
            insertedAlarm = alarm.copy(id = 1L)
            return 1L
        }
        override suspend fun updateAlarm(alarm: AlarmEntity) { insertedAlarm = alarm }
        override suspend fun deleteAlarm(alarm: AlarmEntity) {}
        override suspend fun toggleAlarm(alarm: AlarmEntity): Boolean = true
    }

    private val fakeScheduler = object : AlarmScheduler {
        override fun scheduleAlarm(alarm: AlarmEntity, afterEpochMs: Long?) {}
        override fun scheduleSnooze(alarmId: Long, snoozeDurationMinutes: Int) {}
        override fun scheduleTestAlarm(delaySeconds: Int) {}
        override fun cancelAlarm(alarmId: Long) {}
        override suspend fun reconcileAlarms() {}
        override fun canScheduleExactAlarms(): Boolean = true
    }

    private lateinit var viewModel: CreateEditAlarmViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateEditAlarmViewModel(fakeRepository, fakeScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testQrChallengeRequiresScannedReferenceCodeBeforeSaving() = runTest {
        viewModel.setChallengeType(ChallengeType.QR_CODE)
        assertFalse(viewModel.isSaveAllowed())

        viewModel.setQrCodeReference("TOOTHPASTE_UPC_987654", "Bathroom Sink")
        assertTrue(viewModel.isSaveAllowed())
    }

    @Test
    fun testSaveQrCodeAlarmPersistsTargetPayloadAndSteps() = runTest {
        viewModel.setChallengeType(ChallengeType.QR_CODE)
        viewModel.setQrCodeReference("TOOTHPASTE_UPC_987654", "Bathroom Sink")
        viewModel.setTargetSteps(25)

        var saved = false
        viewModel.saveAlarm { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val alarm = fakeRepository.insertedAlarm
        assertEquals(ChallengeType.QR_CODE, alarm?.challengeType)
        assertEquals("TOOTHPASTE_UPC_987654", alarm?.qrCodePayload)
        assertEquals("Bathroom Sink", alarm?.qrCodeLabel)
        assertEquals(25, alarm?.targetSteps)
    }

    @Test
    fun testDefaultSoundUriIsCallRingtone() {
        val state = viewModel.uiState.value
        assertEquals(null, state.soundUri)
        assertEquals("Phone Ringtone (Default)", state.soundTitle)
    }

    @Test
    fun testSetCustomSoundUriUpdatesStateAndPersists() = runTest {
        viewModel.setSoundUri("content://media/internal/audio/media/42", "Morning Breeze")
        assertEquals("content://media/internal/audio/media/42", viewModel.uiState.value.soundUri)
        assertEquals("Morning Breeze", viewModel.uiState.value.soundTitle)

        var saved = false
        viewModel.saveAlarm { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val savedAlarm = fakeRepository.insertedAlarm
        assertEquals("content://media/internal/audio/media/42", savedAlarm?.soundUri)
    }

    @Test
    fun testLowerVolumeWhileWalkingDefaultsToTrueAndCanBeToggled() = runTest {
        assertTrue(viewModel.uiState.value.lowerVolumeWhileWalking)

        viewModel.setLowerVolumeWhileWalking(false)
        assertFalse(viewModel.uiState.value.lowerVolumeWhileWalking)

        var saved = false
        viewModel.saveAlarm { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val savedAlarm = fakeRepository.insertedAlarm
        assertEquals(false, savedAlarm?.lowerVolumeWhileWalking)
    }

    @Test
    fun testSoundRegistryAvailableInViewModel() {
        val harshSounds = viewModel.soundRegistry.getSoundsByCategory(app.wakewalk.domain.sound.SoundCategory.HARSH)
        assertTrue(harshSounds.isNotEmpty())
        val smoothSounds = viewModel.soundRegistry.getSoundsByCategory(app.wakewalk.domain.sound.SoundCategory.SMOOTH)
        assertTrue(smoothSounds.isNotEmpty())
    }
}

package app.wakewalk.domain.movement

import app.wakewalk.domain.model.StepInput
import org.junit.Assert.assertEquals
import org.junit.Test

class StepAccumulatorTest {
    @Test
    fun testCounterInitialBaselineAndDelta() {
        val accumulator = StepAccumulator(initialBaseline = 1000L)
        // First update at baseline
        assertEquals(0, accumulator.processInput(StepInput.CounterUpdate(100L, 1000L)))
        // 15 steps later
        assertEquals(15, accumulator.processInput(StepInput.CounterUpdate(200L, 1015L)))
        // 42 steps later
        assertEquals(42, accumulator.processInput(StepInput.CounterUpdate(300L, 1042L)))
        assertEquals(42, accumulator.currentRawSteps)
    }

    @Test
    fun testCounterResetHandledGracefully() {
        val accumulator = StepAccumulator(initialBaseline = 1000L)
        accumulator.processInput(StepInput.CounterUpdate(100L, 1020L)) // 20 steps
        assertEquals(20, accumulator.currentRawSteps)

        // Sensor reset/reboot: reports 5
        val result = accumulator.processInput(StepInput.CounterUpdate(200L, 5L))
        // Should preserve previous 20 steps and reset baseline to 5
        assertEquals(20, result)
        assertEquals(20, accumulator.currentRawSteps)

        // Sensor steps after reset
        val resultAfter = accumulator.processInput(StepInput.CounterUpdate(300L, 8L))
        assertEquals(23, resultAfter)
    }

    @Test
    fun testDetectorStepIncrements() {
        val accumulator = StepAccumulator(initialBaseline = 0L)
        accumulator.processInput(StepInput.DetectorStep(100L))
        accumulator.processInput(StepInput.DetectorStep(200L))
        accumulator.processInput(StepInput.DetectorStep(300L))
        assertEquals(3, accumulator.currentRawSteps)
    }

    @Test
    fun testCounterOlderOrDecreasingEventIgnored() {
        val accumulator = StepAccumulator(initialBaseline = 500L)
        accumulator.processInput(StepInput.CounterUpdate(100L, 510L)) // 10 steps
        val duplicate = accumulator.processInput(StepInput.CounterUpdate(150L, 508L)) // Out-of-order/jitter
        assertEquals(10, duplicate)
    }
}

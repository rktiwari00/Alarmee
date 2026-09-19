package app.wakewalk.domain.model

sealed interface StepInput {
    data class CounterUpdate(
        val timestampNs: Long,
        val cumulativeSteps: Long
    ) : StepInput

    data class DetectorStep(
        val timestampNs: Long
    ) : StepInput

    data class AccelerometerCandidate(
        val timestampNs: Long,
        val motion: MotionSnapshot
    ) : StepInput
}

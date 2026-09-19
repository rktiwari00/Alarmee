package app.wakewalk.domain.movement

import app.wakewalk.domain.model.StepInput

class StepAccumulator(
    initialBaseline: Long = 0L,
    initialRawSteps: Int = 0
) {
    private var baseline: Long = initialBaseline
    private var lastReportedCumulative: Long = initialBaseline
    var currentRawSteps: Int = initialRawSteps
        private set

    fun processInput(input: StepInput): Int {
        when (input) {
            is StepInput.CounterUpdate -> {
                val cumulative = input.cumulativeSteps
                if (baseline == 0L && lastReportedCumulative == 0L) {
                    baseline = cumulative
                    lastReportedCumulative = cumulative
                } else if (cumulative < lastReportedCumulative) {
                    // Sensor counter reset (device reboot or sensor restart)
                    baseline = cumulative
                    lastReportedCumulative = cumulative
                } else {
                    val delta = (cumulative - lastReportedCumulative).toInt()
                    if (delta > 0) {
                        currentRawSteps += delta
                        lastReportedCumulative = cumulative
                    }
                }
            }
            is StepInput.DetectorStep -> {
                currentRawSteps += 1
            }
            is StepInput.AccelerometerCandidate -> {
                currentRawSteps += 1
            }
        }
        return currentRawSteps
    }
}

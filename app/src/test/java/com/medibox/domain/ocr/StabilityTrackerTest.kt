package com.medibox.domain.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StabilityTrackerTest {

    @Test
    fun `same results become stable`() {
        val tracker = StabilityTracker(requiredStableCount = 3)
        repeat(3) {
            tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        }
        val snapshot = tracker.snapshot()
        assertTrue(snapshot.isStable)
        assertTrue(snapshot.stableCount >= 3)
    }

    @Test
    fun `similar results remain stable`() {
        val tracker = StabilityTracker(requiredStableCount = 2)
        tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        assertTrue(tracker.snapshot().isStable)
    }

    @Test
    fun `single noisy frame does not break all history`() {
        val tracker = StabilityTracker(requiredStableCount = 3)
        tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        tracker.push(candidate("???", ""))
        val snapshot = tracker.snapshot()
        assertFalse(snapshot.isStable)
        assertTrue(snapshot.latestCandidate?.nameCandidates?.firstOrNull()?.value == "???")
    }

    @Test
    fun `reset clears history`() {
        val tracker = StabilityTracker(requiredStableCount = 2)
        tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        tracker.reset()
        assertFalse(tracker.snapshot().isStable)
        assertTrue(tracker.snapshot().latestCandidate == null)
    }

    @Test
    fun `insufficient history is not stable`() {
        val tracker = StabilityTracker(requiredStableCount = 3)
        tracker.push(candidate("布洛芬缓释胶囊", "0.3g*20粒"))
        assertFalse(tracker.snapshot().isStable)
    }

    private fun candidate(name: String, spec: String): OcrCandidate {
        return OcrCandidate(
            nameCandidates = listOf(OcrFieldCandidate(name)),
            specificationCandidates = if (spec.isBlank()) emptyList() else listOf(OcrFieldCandidate(spec)),
        )
    }
}

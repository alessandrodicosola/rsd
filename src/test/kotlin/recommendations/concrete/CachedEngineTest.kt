package recommendations.concrete

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore

internal class CachedEngineTest {

    private class TestEngine : IRSEngine<Int>() {
        override fun getRecommendations(id: Long): List<RSObject<Int>> {
            return listOf(1 hasScore 0.0, 1 hasScore 0.3, 1 hasScore 0.5)
        }

    }

    @Test
    fun getRecommendations() {

        val engine = CachedEngine(TestEngine())
        engine.getRecommendations(1)
        assertTrue(engine.cached(1))

    }
}
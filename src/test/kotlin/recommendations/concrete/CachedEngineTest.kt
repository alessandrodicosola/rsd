package recommendations.concrete

import logging.info
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore
import kotlin.math.exp

internal class CachedEngineTest {

    private class TestEngine : IRSEngine<Int>() {
        override fun getRecommendations(id: Long): List<RSObject<Int>> {
            return listOf(1 hasScore 0.0, 2 hasScore 0.3, 3 hasScore 0.5)
        }

    }

    @Test
    fun getRecommendations() {

        val engine = CachedEngine(TestEngine())
        engine.getRecommendations(1)
        assertTrue(engine.cached(1))
        val expected = listOf(1 hasScore 0.0, 2 hasScore 0.3, 3 hasScore 0.5).associate { it ->
            it.id to (it.id hasScore it.score)
        }

        val list = engine.getRecommendations(1)

        list.forEach {
            assertEquals(expected.get(it.id), it)
        }

    }
}
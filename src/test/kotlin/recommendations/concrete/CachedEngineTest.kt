package recommendations.concrete

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore

internal class CachedEngineTest {

    private class TestEngine : IRSEngine<Long, Int, Double>() {
        override fun getRecommendations(userId: Long, itemId: Int): List<RSObject<Int, Double>> {
            return listOf(1 hasScore 0.0, 2 hasScore 0.3, 3 hasScore 0.5)
        }

        override fun updateRecommendations(userId: Long, itemId: Int, ratingType: Double) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    @Test
    fun getRecommendations() {

        val engine = CachedEngine(TestEngine())
        engine.getRecommendations(1,-1)
        assertTrue(engine.cached(1))
        val expected = listOf(1 hasScore 0.0, 2 hasScore 0.3, 3 hasScore 0.5).associate {
            it.id to (it.id hasScore it.score)
        }

        val list = engine.getRecommendations(1,-1)

        list.forEach {
            assertEquals(expected.get(it.id), it)
        }

    }
}
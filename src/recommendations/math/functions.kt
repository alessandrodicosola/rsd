package math

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue


fun sigmoid(value: Double): Double {
    return 1 / (1 + kotlin.math.exp(value));
}

fun derivateSigmoid(value: Double): Double {
    return sigmoid(value) * (1 - sigmoid(value));
}

/**
 * Calculate the cosine similarity between two user U and V
 * @param intersectU Map<Long,Double> of items rated by both U and V. This map contains ratings information about U.
 * @param intersectV Map<Long,Double> of items rated by both U and V. This map contains ratings information about V.
 * @param ratingsU List<Double> which contains items rated by U
 * @param ratingsV List<Double> which contains items rated by V
 */
fun cosineSimilarity(
    intersectU: Map<Long, Double>,
    intersectV: Map<Long, Double>,
    ratingsU: List<Double>,
    ratingsV: List<Double>
): Double {
    assertEquals(intersectU.size, intersectV.size)
    assertTrue(intersectU.all { intersectV.containsKey(it.key) })


    val sumIntersect = intersectU.map { it.value * intersectV.get(it.key)!! }.sum()

    val sumU = ratingsU.map { it * it }.sum();
    val sumV = ratingsV.map { it * it }.sum();

    return sumIntersect / sqrt(sumU * sumV)
}

/**
 * Apply Persona Correlation for calculating similarity between two user U and V
 */
fun personaCorrelation(
    intersectU: Map<Long, Double>,
    intersectV: Map<Long, Double>,
    avgU: Double,
    avgV: Double
): Double {
    val normalizedIU = intersectU.mapValues { it.value - avgU }
    val normalizedIV = intersectV.mapValues { it.value - avgV }
    val normalizedRU = intersectU.map { it.value - avgU }
    val normalizedRV = intersectV.map { it.value - avgV }
    return cosineSimilarity(normalizedIU, normalizedIV, normalizedRU, normalizedRV);
}

fun weightPersonaCorrelationWithFactorSize(
    intersectU: Map<Long, Double>,
    intersectV: Map<Long, Double>,
    avgU: Double,
    avgV: Double,
    weightFactor: Int
): Double {
    val factor = (min(intersectU.size, weightFactor) / weightFactor).toDouble()
    return personaCorrelation(intersectU, intersectV, avgU, avgV) * factor
}

fun WPC(
    intersectU: Map<Long, Double>,
    intersectV: Map<Long, Double>,
    avgU: Double,
    avgV: Double,
    weights: Map<Long, Double>
): Double {

    assertEquals(intersectU.size, intersectV.size)
    assertEquals(intersectU.size, weights.size)
    assertTrue(intersectU.all { intersectV.containsKey(it.key) })
    assertTrue(weights.all { intersectU.containsKey(it.key) })

    val mapU = intersectU.mapValues { (it.value - avgU) * weights.get(it.key)!! }
    val mapV = intersectV.mapValues { it.value - avgV }
    val sumNumerator = mapU.map { it.value * mapV.get(it.key)!! }.sum()

    val sumU = intersectU.map { Math.pow(it.value - avgU, 2.0) * weights.get(it.key)!! }.sum()
    val sumV = intersectV.map { Math.pow(it.value - avgV, 2.0) * weights.get(it.key)!! }.sum()

    return sumNumerator / sqrt(sumU * sumV)

}
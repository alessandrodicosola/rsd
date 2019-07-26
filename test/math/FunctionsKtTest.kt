package math

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

internal class FunctionsKtTest {


    @Test
    fun cosineSimilarityTest() {
        /*
               user/item   | a | b | c |
                   A       | 1 | 4 | 5 |
                   B       | 2 | 1 | 2 |
                   C       | 3 | 2 | 3 |
           HYPOTHESIS:
               A and B have rated the same items: a and c
               IA is the set of items rated by A
               IB is the set of items rated by B
               II is the set of items rated by A and B

                sum( II ) = 1*2 + 5*2 = 2+10 = 12
                sum( IA ) = 1+16+25 = 42
                sum( IB ) = 4+1+4 = 9

                CV(A,B) = 12/sqrt(42*9)
         */

        val intersectA = listOf(1, 5).mapIndexed { index, value -> Pair(index.toLong(), value.toDouble()) }.toMap()
        val intersectB = listOf(2, 2).mapIndexed { index, value -> Pair(index.toLong(), value.toDouble()) }.toMap()

        val ratingsA = listOf(1, 4, 5).map { it.toDouble() }
        val ratingsB = listOf(2, 1, 2).map { it.toDouble() }

        val cv = math.cosineSimilarity(intersectA, intersectB, ratingsA, ratingsB)

        assertEquals(cv, 12.0 / sqrt(42.0 * 9.0))
    }

    @Test
    fun cosineSimilarityPerformance() {
        val capacities = listOf(1000, 5000, 10000, 50000, 100000, 500000, 1000000);
        capacities.forEach { singleCosineSimilarityPerformance(it, 10) }
    }

    private fun singleCosineSimilarityPerformance(trainingLength: Int, times: Int) {
        var time = 0L

        println("Testing capacity: ${trainingLength}")
        for (i in 0..times) {

            val length = trainingLength     //training length
            val test_length = length / 10   // 10% of training length

            val intersectU = generate_map(length);
            val intersectV = intersectU.mapValues { it.value + 5 }

            val ratingsU = generate_map(test_length).map { it.value }
            val ratingsV = generate_map(test_length).map { it.value }


            val start = System.currentTimeMillis()
            math.cosineSimilarity(intersectU, intersectV, ratingsU, ratingsV);
            val end = System.currentTimeMillis()
            time += end - start;
        }
        println("AVG Time passed : ${(time) / times} ms")

    }

    private fun generate_map(N: Int): Map<Long, Double> {
        val random: Random = Random(System.currentTimeMillis())
        return (1..N).associate { Pair(random.nextLong(), random.nextDouble()) }
    }

    @Test
    fun personaCorrelation() {

        /*
               user/item   | a | b | c |
                   A       | 1 | 4 | 5 |
                   B       | 2 | 1 | 2 |
                   C       | 3 | 2 | 3 |
           HYPOTHESIS:

               A and B have rated the same items: a and c
               IA is the set of items rated by A
               IB is the set of items rated by B
               II is the set of items rated by A and B
               AVG_A = 1+4+5/3 = 10/3
               AVG_B = 2+1+2/3 = 5/3

               sum( II ) = (1-10/3)*(2-5/3) + (5-10/3)*(2-5/3) = -7/9 + 5/9 = -2/9
               sum( IA ) = (1-10/3)^2+(4-10/3)^2+(5-10/3)^2 = 49/9 + 4/9 + 25/9 = 78/9
               sum( IB ) = (2-5/3)^2+(1-5/3)^2+(2-5/3)^2 = 1/9 + 4/9 + 1/9 = 6/9

               CV(A,B) = -2/9 / sqrt(78/9 * 6/9) = -sqrt(13)/39
         */

        val intersectA = listOf(1, 5).mapIndexed { index, value -> Pair(index.toLong(), value.toDouble()) }.toMap()
        val intersectB = listOf(2, 2).mapIndexed { index, value -> Pair(index.toLong(), value.toDouble()) }.toMap()

        val ratingsA = listOf(1, 4, 5).map { it.toDouble() }
        val ratingsB = listOf(2, 1, 2).map { it.toDouble() }

        val avgA = ratingsA.average()
        val avgB = ratingsB.average()
        val cv = math.personaCorrelation(intersectA, intersectB, ratingsA, ratingsB,avgA,avgB)

        assertEquals(cv, -sqrt(13.0)/39, 0.00000001) // epsilon 10^-9

    }
}
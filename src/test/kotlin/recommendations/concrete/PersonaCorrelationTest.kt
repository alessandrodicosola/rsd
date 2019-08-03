package recommendations.concrete

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import recommendations.skel.Neighbor
import recommendations.skel.User
import kotlin.math.sqrt

internal class PersonaCorrelationTest {

    @Test
    fun calculate() {
        /*
              user/item   | a | b | c |
                  A       | 1 | 4 | 5 |
                  B       | 2 | 1 | 2 |
                  C       | 3 | 2 | 3 |
          HYPOTHESIS:

              A and B have rated the same items: a and c
              IA is the set of items rated by A (but also B)
              IB is the set of items rated by B (but also A)
              II is the set of items rated by A and B
              AVG_A = 1+4+5/3 = 10/3
              AVG_B = 2+1+2/3 = 5/3

              sum( II ) = (1-10/3)*(2-5/3) + (5-10/3)*(2-5/3) = -7/9 + 5/9 = -2/9
              sum( IA ) = (1-10/3)^2+(5-10/3)^2 = 74/9
              sum( IB ) = (2-5/3)^2+(2-5/3)^2 = 2/9

              CV(A,B) = -2/9 / sqrt(74/9 * 2/9) = -sqrt(37)/37
        */

        val intersectA =
            HashMap(listOf(1, 5).mapIndexed { index, value -> Pair(index.toLong(), value.toDouble()) }.toMap())
        val intersectB =
            HashMap(listOf(2, 2).mapIndexed { index, value -> Pair(index.toLong(), value.toDouble()) }.toMap())

        val ratingsA = listOf(1, 4, 5).map { it.toDouble() }
        val ratingsB = listOf(2, 1, 2).map { it.toDouble() }

        val avgA = User(0, ratingsA.average(), 0.0)
        val avgB = Neighbor(1, ratingsB.average(), 0.0, 0.0)
        val w = PersonaCorrelation(intersectA, intersectB, avgA, avgB).calculate()

        assertEquals(-sqrt(37.0) / 37, w, 0.00000001) // epsilon 10^-9

    }
}
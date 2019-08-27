package recommendations.concrete

import recommendations.skel.IErrorCalculator
import kotlin.math.pow
import kotlin.test.assertEquals

/**
 * General Root Mean Square Error calculator
 */
class RMSECalculator(private val trueRatings: DoubleArray, private val predictedRatings: DoubleArray) :
    IErrorCalculator<Double> {

    override fun calculate(): Double {
        assertEquals(trueRatings.size, predictedRatings.size)

        return trueRatings.asSequence()
            .mapIndexed { index, value -> (value - predictedRatings[index]).pow(2) } // (real - prediction)^2
            .sum() // summation
            .div(trueRatings.size) //normalize
            .pow(-0.5) //sqrt
    }
}
package recommendations.concrete

import common.dotProduct
import recommendations.skel.IRatingCalculator
import kotlin.math.pow

class LatentFactorsWithImplicit_RatingCalculator(
    private val meanOverall: Double,
    private val biasUser: Double,
    private val biasItem: Double,
    private val factorsUser: DoubleArray,
    private val factorsItem: DoubleArray,
    private val factorsImplicit: MutableMap<Int, DoubleArray>
) : IRatingCalculator<Double> {

    override fun calculate(): Double {
        val firstSize = factorsImplicit.values.elementAt(0).size
        assert(factorsImplicit.all { it.value.count() == firstSize })

        val factorForNormalizing = firstSize.toDouble().pow(-0.5);

        // factorsImplicit.map { it.value[indexFactor] }   For each item get the same factor
        // .sumByDouble()                                  then returns the sum
        // factorsItem.asSequence().mapIndexed { index, value -> value * factorsUser[index] }.sumByDouble { it }
        val sumItemUser = factorsItem.dotProduct(factorsUser)

        val sumItemImplicit =
            // sum for each implicit the factor specified by index then normalize and multiply with factor of item
            factorsItem.asSequence()
                .mapIndexed { indexFactor, valueFactor -> valueFactor * (factorsImplicit.asSequence().map { it.value[indexFactor] }.sumByDouble { it } * factorForNormalizing) }
                .sumByDouble { it }

        return meanOverall + biasUser + biasItem + sumItemUser + sumItemImplicit
    }

}
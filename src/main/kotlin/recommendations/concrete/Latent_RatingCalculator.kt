package recommendations.concrete

import common.dotProduct
import recommendations.skel.IRatingCalculator
import kotlin.math.pow
import kotlin.test.assertEquals

class Latent_RatingCalculator(
    private val meanOverall: Double,
    private val biasUser: Double,
    private val biasItem: Double,
    private val factorsUser: DoubleArray,
    private val factorsItem: DoubleArray,
    private val factorsImplicit: MutableMap<Int, DoubleArray>
) : IRatingCalculator<Double> {

    override fun calculate(): Double {

        val firstSize = factorsUser.size
        assertEquals(firstSize, factorsItem.size)
        assert(factorsImplicit.all { it.value.count() == firstSize })

        val factorForNormalizing = firstSize.toDouble().pow(-0.5);

        val prodItemUser = factorsItem.dotProduct(factorsUser)

        val sumImplicits = DoubleArray(firstSize)

        for (factor in 0 until firstSize) {
            var sum = 0.0
            for (implicit in factorsImplicit) {
                sum += implicit.value[factor]
            }
            sumImplicits[factor] = sum * factorForNormalizing
        }

        val prodItemImplicit = factorsItem.dotProduct(sumImplicits)

        return meanOverall + biasUser + biasItem + prodItemUser + prodItemImplicit
    }

}
package recommendations.concrete

import recommendations.skel.IWeightCalculator
import kotlin.math.min

//Use the number of items, used for calculating the weight, for normalize the weight in order to give more importance to weight obtained with a lot of items
class WeightedCorrelation(val weightCalculator: IWeightCalculator, val numberoOfItems: Int, val factor: Int) : IWeightCalculator {
    override fun calculate(): Double {
        return weightCalculator.calculate() * min(numberoOfItems, factor).toDouble() / factor
    }
}
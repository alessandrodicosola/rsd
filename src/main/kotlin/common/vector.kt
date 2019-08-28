package common

import kotlin.math.pow

/* This file contains vector operations */

/**
 * @return Dot product between two vectors: SUM(x_i * y_i) for each i belonged to [[0,...,N]]
 */
fun DoubleArray.dotProduct(other: DoubleArray): Double {
    assert(this.size == other.size)
    return this.asSequence().mapIndexed { index, value -> value * other[index] }.sum()
}

fun DoubleArray.scalarProduct(scalar: Double): DoubleArray {
    return this.asSequence().map { it * scalar }.toList().toDoubleArray()
}

fun Sequence<Double>.scalarProduct(scalar: Double): Sequence<Double> {
    return this.map { it * scalar }
}

/**
 * @return Euclidean Norm of [this] vector: ||x|| = sqrt(sum(x_i))
 */
fun DoubleArray.norm(): Double {
    return this.asSequence().map { it * it }.sum().pow(0.5)
}

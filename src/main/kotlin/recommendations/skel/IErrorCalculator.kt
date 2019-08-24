package recommendations.skel

interface IErrorCalculator<T> {
    fun calculate(real: T, prediction: T): T
}
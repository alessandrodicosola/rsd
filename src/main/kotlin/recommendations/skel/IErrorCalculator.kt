package recommendations.skel

interface IErrorCalculator<T> {
    fun calculate(): T
}
package recommendations.skel

/**
 * Interfaccia per calcolare l'errore
 */
interface IErrorCalculator<T> {
    fun calculate(): T
}
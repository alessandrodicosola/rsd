package recommendations.skel

/**
 * Interfaccia che definisce le classi che calcolano la somiglianza tra due oggetti o utenti
 */
interface IWeightCalculator {
    fun calculate(): Double
}
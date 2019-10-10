package recommendations.concrete

import recommendations.skel.IWeightCalculator
import recommendations.skel.Neighbor
import recommendations.skel.User
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Calculate weight between two users through Persona Correlation
 * @param ratingsU Rates given by U to items rated also by V
 * @param ratingsV Rates given by V to items rated also by U
 * @param user Utente
 * @param neighbor Vicino
 */
class PersonaCorrelation<Key>(
    private val ratingsU: Map<Key, Double>,
    private val ratingsV: Map<Key, Double>,
    private val user: User,
    private val neighbor: Neighbor
) :
    IWeightCalculator {
    /**
     * @return Weight between U and V
     */
    override fun calculate(): Double {
        assertEquals(ratingsU.size, ratingsV.size)
        assertTrue(ratingsU.all { ratingsV.containsKey(it.key) }, "ratingsU and ratingsV contains different keys")

        //Calcolo le aspettazioni dei rate dell'utente e del vicino: E[U-meanU]; E[V-meanV]
        //Centro i valori dei rate dell'utente rispetto alla media dell'utente stesso
        val normalizedU = ratingsU.mapValues { it.value - user.avg }
        //Centro i valori dei rate del vicino rispetto alla media dell'vicino stesso
        val normalizedV = ratingsV.mapValues { it.value - neighbor.avg }

        //Calcolo la covarianza tra utente e vicino
        val num = normalizedU.mapValues { it.value * normalizedV[it.key]!! }.asSequence().sumByDouble { it.value }
        //Calcolo la varianza dell'utente
        val den1 = normalizedU.mapValues { it.value * it.value }.asSequence().sumByDouble { it.value }
        //Calcolo la varianza del vicino
        val den2 = normalizedV.mapValues { it.value * it.value }.asSequence().sumByDouble { it.value }
        //Eseguo il prodotto tra la varianza dell'utente e del vicino sotto radice per determinare il prdootto della deviazione standard di entrambi
        val prodDen = sqrt(den1 * den2)
        //Restituisco il valore
        return num / prodDen
    }

}

/**
 * Persona Correlation che fa uso di un fattore per incrementare il valore della somiglianze se eseguita con più oggetti.
 * @param ratingsU Rates dell'utente
 * @param ratingsV Rates del vicino
 * @param user Utente
 * @param neighbor Vicino
 * @param factor Fattore che pesa la somiglianza
 * @return Weight between U and V
 */
class WeightedPersonaCorrelation<Key>(
    ratingsU: Map<Key, Double>,
    ratingsV: Map<Key, Double>,
    user: User,
    neighbor: Neighbor,
    factor: Int
) : WeightedCorrelation(PersonaCorrelation(ratingsU, ratingsV, user, neighbor), ratingsU.size, factor)

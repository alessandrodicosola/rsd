package recommendations.concrete

import common.dotProduct
import recommendations.skel.IRatingCalculator
import kotlin.math.pow
import kotlin.test.assertEquals

/**
 * Calcolo il rating di un oggetto attraverso la somma:
 *  mean + biasUser + biasItem + FattoriLatentiItem X (FattoriLatentiUser + FattoriLatentiInfoImplicite)
 *  @param meanOverall
 *  @param biasUser Scostamento della media dell'utente dalla media generale
 *  @param biasItem Scostamento della media dell'oggetto dalla media generale
 *  @param factorsUser Vettore fattori latenti dell'utente
 *  @param factorsItem Vettore fattori latenti dell'oggetto
 *  @param factorsImplicit Vettore fattori latenti delle informazioni implicite da utilizzare
 */
class Latent_RatingCalculator(
    private val meanOverall: Double,
    private val biasUser: Double,
    private val biasItem: Double,
    private val factorsUser: DoubleArray,
    private val factorsItem: DoubleArray,
    private val factorsImplicit: MutableMap<Int, DoubleArray>
) : IRatingCalculator<Double> {

    override fun calculate(): Double {

        //Ottengo il numero massimo di fattori latenti
        val size = factorsUser.size
        //Verifico che tutti i vettori rispettano le dimensioni
        assertEquals(size, factorsItem.size)
        assert(factorsImplicit.all { it.value.count() == size })

        //Valore per normalizzare la somma
        val factorForNormalizing = size.toDouble().pow(-0.5);
        //Calcolo il prodotto tra i fattori latenti dell'oggetto e quelli dell'utente
        val prodItemUser = factorsItem.dotProduct(factorsUser)

        //Calcolo la somma dei fattori latenti
        val sumImplicits = DoubleArray(size)
        for (factor in 0 until size) {
            var sum = 0.0
            for (implicit in factorsImplicit) {
                sum += implicit.value[factor]
            }
            sumImplicits[factor] = sum * factorForNormalizing
        }
        //Calcolo il prodotto tra i fattori latenti dell'oggetto e quelli dei dati impliciti
        val prodItemImplicit = factorsItem.dotProduct(sumImplicits)

        //Restituisco il rate
        return meanOverall + biasUser + biasItem + prodItemUser + prodItemImplicit
    }

}
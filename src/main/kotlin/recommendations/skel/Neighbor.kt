package recommendations.skel

/**
 * Classe che definisce il vicino rispetto a un oggetto o utente
 * @param id Identificativo
 * @param avg Media dei rate rispetto all'oggetto o utente
 * @param std Deviazione standard dei rate rispetto all'oggetto o utente
 * @param weight Somiglianza calcolata tra due oggetti o utenti
 */
data class Neighbor(var id: Long, var avg: Double, var std: Double, var weight: Double) {
    constructor(user: User, weight: Double) : this(user.id, user.avg, user.std, weight)
}
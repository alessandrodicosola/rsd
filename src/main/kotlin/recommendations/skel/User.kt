package recommendations.skel

/**
 * Classe che definisce un utente
 * @param id Identificativo
 * @param avg Media dei rate dell'utente
 * @param std Deviazione standard dei rate dell'utente
 */
data class User(var id: Long, var avg: Double, var std: Double)

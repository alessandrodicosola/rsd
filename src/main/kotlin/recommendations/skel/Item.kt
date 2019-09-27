package recommendations.skel

/**
 * Classe che definisce l'oggetto nel database
 * @param Id identificativo
 * @param avg Media dei rates relativa all'oggetto
 * @param std Deviazione standard dei rate relativa all'oggetto
 */
data class Item(val id: Int, var avg: Double, var std: Double);
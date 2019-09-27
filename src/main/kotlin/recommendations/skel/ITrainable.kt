package recommendations.skel

/**
 * Interfaccia per identificare i sistemi di raccomandazione che fanno usao del learning come operazione per calcolare le preferenze di un utente rispetto a un oggetto
 */
interface ITrainable {
    /**
     * Esegue il training
     */
    fun train()

    /**
     * Carica i dati estrapolati con il training
     */
    fun loadLearn()
    /**
     * Salva i dati estrapolati con il training
     */
    fun saveLearn()
}
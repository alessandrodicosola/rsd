package recommendations.skel

/**
 * Interfaccia per gestire le classi che eseguono il login nel server
 */
interface IAuth{
    /**
     * Metodo per connettersi al server
     * @param username
     * @param password
     */
    fun connect(username:String,password:String) : Long

    /**
     * Metodo per dicsonnettersi dal server
     * @param username
     */
    fun disconnect(username:String) : Boolean
}
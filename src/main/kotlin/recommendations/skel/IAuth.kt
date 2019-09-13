package recommendations.skel

interface IAuth{
    fun connect(username:String,password:String) : Long
    fun disconnect(username:String) : Boolean
}
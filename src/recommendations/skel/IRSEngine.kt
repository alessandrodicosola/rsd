package recommendations

interface IRSEngine
{
    fun getRecommendations(userId:Long) : List<RSObject>
}
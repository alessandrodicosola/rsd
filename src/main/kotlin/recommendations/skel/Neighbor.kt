package recommendations.skel

data class Neighbor(var id: Long, var avg: Double, var std: Double, var weight: Double) {
    constructor(user: User, weight: Double) : this(user.id, user.avg, user.std, weight)
}
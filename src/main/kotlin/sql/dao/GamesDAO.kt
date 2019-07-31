package sql.dao

import org.jetbrains.exposed.sql.Table

/**
 * Main object for accessing games information
 * Possibile values:
 *  - games_daily [ 384M of entry ]
 *  - games_training  [ 100K-500K  of entry ]
 */
object GamesDAO : Table("games_training") {
    val SteamId = long("steamid").primaryKey()
    val AppId = integer("appid").primaryKey()
    val DataRetrieved = datetime("dateretrieved").primaryKey()

    val Playtime2Weeks = integer("playtime_2weeks")
    val PlaytimeForever = integer("playtime_forever")

}
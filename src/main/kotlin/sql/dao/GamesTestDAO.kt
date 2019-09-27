package sql.dao

import org.jetbrains.exposed.sql.Table

/**
 * Rappresenta la tabella games_test nel database
 */
object GamesTestDAO : Table("games_test") {
    val SteamId = long("steamid").primaryKey()
    val AppId = integer("appid").primaryKey()
    val DataRetrieved = datetime("dateretrieved").primaryKey()

    val Playtime2Weeks = integer("playtime_2weeks")

    //val PlaytimeForever = GamesDAO.integer("playtime_forever")
    val PlaytimeForever = integer("log_playtime")

}

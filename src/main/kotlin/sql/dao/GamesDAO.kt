package sql.dao

import org.jetbrains.exposed.sql.Table

object GamesTrainingDAO : Table("games_daily") {
    val SteamId = long("steamid").primaryKey()
    val AppId = integer("appid").primaryKey()
    val DataRetrieved = datetime("dateretrieved").primaryKey()

    val Playtime2Weeks = integer("playtime_2weeks")
    val PlaytimeForever = integer("playtime_forever")

}
package sql.dao

import org.jetbrains.exposed.sql.Table

object GamesCrossValidationDAO : Table("games_crossvalidation") {
    val SteamId = GamesDAO.long("steamid").primaryKey()
    val AppId = GamesDAO.integer("appid").primaryKey()
    val DataRetrieved = GamesDAO.datetime("dateretrieved").primaryKey()

    val Playtime2Weeks = GamesDAO.integer("playtime_2weeks")
    //val PlaytimeForever = GamesDAO.integer("playtime_forever")
    val PlaytimeForever = GamesDAO.integer("log_playtime")

}
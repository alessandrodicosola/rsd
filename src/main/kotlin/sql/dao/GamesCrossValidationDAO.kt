package sql.dao

import org.jetbrains.exposed.sql.Table

object GamesCrossValidationDAO : Table("games_crossvalidation") {
    val SteamId = GamesTrainingDAO.long("steamid").primaryKey()
    val AppId = GamesTrainingDAO.integer("appid").primaryKey()
    val DataRetrieved = GamesTrainingDAO.datetime("dateretrieved").primaryKey()

    val Playtime2Weeks = GamesTrainingDAO.integer("playtime_2weeks")
    val PlaytimeForever = GamesTrainingDAO.integer("playtime_forever")

}
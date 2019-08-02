## DATASET


È stato utilizzato il seguente dataset [https://steam.internet.byu.edu/](https://steam.internet.byu.edu/)

È stato utilizzato l'applicatiovo [lsqls](https://github.com/alessandrodicosola/lsqls) per generare solo le tabelle necessarie e splittarle così da poter gestire l'enorme quantità di dati.

Sono state create le seguenti tabelle:

| Table                 | SQL Query                                       |           |
|-----------------------|-----------------------------------------------  |-----------|
| games_test            |`INSERT INTO games_test SELECT * FROM games_1 AS G1 WHERE RAND() < 0.2 AND playtime_forever > 0 AND EXISTS (SELECT appid FROM app_id_info AS API WHERE Type="game" AND API.appid = G1.appid) LIMIT 10000` |         |
| games_crossvalidation |`INSERT INTO games_crossvalidation SELECT * FROM games_1 AS G1 WHERE (steamid,appid) NOT IN (SELECT steamid,appid FROM games_test) AND RAND() < 0.2 AND playtime_forever > 0 AND EXISTS (SELECT appid FROM app_id_info AS API WHERE Type="game" AND API.appid = G1.appid)  LIMIT 10000`                                |         |
| games_training        |`INSERT INTO games_training SELECT * FROM games_1 AS G1 WHERE NOT EXISTS(SELECT * FROM games_test as GT WHERE GT.steamid = G1.steamid AND GT.appid = G1.steamid) AND NOT EXISTS(SELECT * FROM games_crossvalidation as GC WHERE GC.steamid = G1.steamid AND GC.appid = G1.steamid) AND RAND() < 0.5 AND EXISTS (SELECT appid FROM app_id_info AS API WHERE Type="game" AND API.appid = G1.appid) LIMIT 50000 `                    


Sono state aggiunte le seguenti informazioni ad hoc per l'user in modo tale da avere abbastanza informazioni per gli utenti

| User                  | SQL Query | Comment |
|-----------------------|-----------|---------|
|76561198015082830      |`INSERT IGNORE INTO games_training SELECT * FROM games_1 AS G1 WHERE (playtime_forever > 0 OR playtime_forever IS NOT NULL) AND EXISTS (SELECT appid FROM app_id_info AS API WHERE Type="game" AND API.appid = G1.appid) AND appid IN (SELECT T1.appid FROM (SELECT DISTINCT appid FROM games_training ) AS T1 LEFT JOIN (SELECT * FROM games_training WHERE steamid = 76561198015082830 ) AS T2 ON T1.appid = T2.appid WHERE (playtime_forever = 0 OR playtime_forever IS NULL)) LIMIT 1000000;`|Insert all information in games_training|
|76561198014912110      | Same as before |

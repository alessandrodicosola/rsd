## DATASET

È stato utilizzato il seguente dataset [https://steam.internet.byu.edu/](https://steam.internet.byu.edu/)

È stato utilizzato l'applicatiovo [lsqls](https://github.com/alessandrodicosola/lsqls) per generare solo le tabelle necessarie e splittarle così da poter gestire l'enorme quantità di dati.

Sono state create le seguenti tabelle:

| Table                 |Output                                         | SQL Query |
|-----------------------|-----------------------------------------------|-----------|
| games_test            |Query OK, 10000 rows affected (3 min 28.231 sec)| `INSERT INTO games_test SELECT * FROM games_daily WHERE playtime_forever > 0 GROUP BY steamid HAVING COUNT(steamid)>50 ORDER BY RAND() LIMIT 5000;`     |
| games_crossvalidation |Query OK, 10000 rows affected (4 min 10.667 sec)|`INSERT INTO games_crossvalidation SELECT * FROM games_daily WHERE playtime_forever > 0 AND (steamid,appid) NOT IN (SELECT steamid,appid FROM games_test ) GROUP BY steamid HAVING COUNT(steamid)>50 ORDER BY RAND() LIMIT 10000`      |
| games_training        |Query OK, 100000 rows affected (4 min 37.411 sec)| `INSERT INTO games_training SELECT * FROM games_daily WHERE (steamid,appid) NOT IN (SELECT steamid,appid FROM games_test ) AND (steamid,appid) NOT IN (SELECT steamid,appid FROM games_crossvalidation) GROUP BY steamid HAVING COUNT(steamid)>50 ORDER BY RAND() LIMIT 100000`      |



## DATASET

È stato utilizzato il seguente dataset [https://steam.internet.byu.edu/](https://steam.internet.byu.edu/)

È stato utilizzato l'applicatiovo [lsqls](https://github.com/alessandrodicosola/lsqls) per generare solo le tabelle necessarie e splittarle così da poter gestire l'enorme quantità di dati.

Sono state create le seguenti tabelle:

| Table                 |Rows| SQL Query |
|-----------------------|----|-----------|
| games_crossvalidation |500|`INSERT INTO games_crossvalidation SELECT * FROM games_daily WHERE playtime_forever>0 AND (steamid,appid) NOT IN (SELECT steamid,appid FROM games_test) ORDER BY RAND() LIMIT 500`      |
| games_test            |500| `INSERT INTO games_test SELECT * FROM games_daily WHERE playtime_forever>0 ORDER BY RAND() LIMIT 500`     |
| games_training        |5000| `INSERT INTO games_training SELECT * FROM games_daily WHERE playtime_forever>0 AND (steamid,appid) NOT IN (SELECT steamid,appid FROM games_test) AND (steamid,appid) NOT IN (SELECT steamid,appid FROM games_crossvalidation) ORDER BY RAND() LIMIT 5000`      |



### Recommendations usati:

** Tutte le considerazioni fatte sono basate sullo studio del documento:
#####Title:_Recommender Systems Handbook_
#####Editor(s):_Francesco Ricci · Lior Rokach · Bracha Shapira · Paul B. Kantor_

Le citazioni più precise verranno segnalate nella tesi

## Neighborhood 
- Il rating è calcolato come ln(playtime_forever) il quale è Z-Normalizzato (inizialmente ho usato playtime_forever ma avendo dubbio sulla correttezza ho preferito usare log_10(playtime_forever) ma questo andava ad appiattire troppo i valori.
- User based perchè ogni utente a pochi giochi (nel peggiore dei casi uno solo) di conseguenza un sistema _item based_ non potrebbe calcolare il rating poichè sarebbe impossibile trovare diversi utenti che abbiano valutato un particolare oggetto infatti per calcolare la somiglianza tra due oggetti è necessario che differenti utenti del dataset abbiano valutato l'oggetto con simili valori

- Viene usato un sistema TopN per scegliere i vicini. 
  - Valore: 40
  
- Il peso tra due utenti è calcolato con l'algoritmo Persona Correlation pesato attraverso un fattore=5 (poichè non ci sono molti oggetti per utente)

  - Non uso WPC perchè calcolare il peso tra due oggetti per ogni oggetto diventa lento dal punto di vista computazionale poichè i pesi non sono presenti in un database e devono essere di volta in volta calcolati

## Neighborhood model based
- TODO
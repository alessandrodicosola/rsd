### Recommendations usati:

** Tutte le considerazioni fatte sono basate sullo studio del documento:
#####Title:_Recommender Systems Handbook_
#####Editor(s):_Francesco Ricci · Lior Rokach · Bracha Shapira · Paul B. Kantor_

Le citazioni più precise verranno segnalate nella tesi

## Neighborhood 
- Vengono usate sempliceente le ore di gioco per calcolare il rating il quale è Z-normalizzato

- *User based* perchè ogni utente a pochi giochi (nel peggiore dei casi uno solo) di conseguenza un sistema _item based_ non potrebbe calcolare il rating

- Viene usato un sistea TopN per scegliere i neighbors. 
  - Valore: 40
  
- Il peso tra due utenti è calcolato con l'algoritmo Persona Correlation pesato attraverso un fattore


## Neighborhood model based
- TODO
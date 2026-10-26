---
name: test-regole
description: Verifica che i test dell'ordine coprano le regole R1-R5, i valori limite e la presentazione R6 senza tautologie.
---

Esamina la specifica in `docs/corso/M6-G1.md`, in particolare le regole R1-R6 e le disambiguazioni, quindi confrontala con i test e l'implementazione presenti nel repository.

Non modificare file e non aggiungere test: produci una revisione verificabile che riporti:

1. Per R1-R5, i test che coprono la regola e se gli assert controllano il comportamento richiesto con valori attesi espliciti.
2. Per R1, verifica i tre punti intorno a entrambe le soglie di quantità: 9/10/11 e 49/50/51. Segnala eventuali punti mancanti o percentuali attese errate.
3. Per R2, verifica che esista un caso che distingua `HALF_UP` dagli altri arrotondamenti e che non avvengano arrotondamenti intermedi.
4. Per R3, verifica che due aggiunte dello stesso prodotto da 5 unità diventino una riga da 10 unità con sconto del 5%.
5. Per R4-R5, verifica la somma dei subtotali scontati e i casi di spedizione 149,99 € / 150,00 €, con spedizione e totale pagabile.
6. Per R6, verifica test web che controllino la percentuale e i totali mostrati nel dettaglio, nonché il totale da pagare nell'elenco.
7. Segnala test tautologici, asserzioni troppo deboli, valori che non corrispondono ai calcoli, o coperture mancanti. Non considerare sufficiente il solo esito verde della suite.

Concludi con una tabella: `Regola | Test individuati | Esito (coperta/parziale/mancante) | Problemi`. Se non puoi verificare qualcosa dai file disponibili, dichiaralo invece di presumere che sia coperto. Non dichiarare di aver eseguito test: questo prompt serve per analizzare la copertura.

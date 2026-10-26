# Piano di implementazione — Sconti per quantità

## Obiettivo e specifica

Implementare nel sistema ordini:

- R1: sconto per quantità complessiva della riga: 0% da 1 a 9, 5% da 10 a 49, 10% da 50 in su;
- R2: subtotale di riga calcolato con `BigDecimal`, senza arrotondamenti intermedi e con arrotondamento finale al centesimo `HALF_UP`;
- R3: aggiunte ripetute dello stesso prodotto confluiscono nella stessa riga prima di determinare la fascia;
- R4: totale merce come somma dei subtotali scontati;
- R5: spedizione 9,90 € sotto 150,00 € e gratuita da 150,00 €; totale da pagare pari a merce più spedizione;
- R6: dettaglio con percentuale di sconto per riga e riepilogo merce/spedizione/totale da pagare; elenco con totale da pagare.

Le quantità minori o uguali a zero restano invalide secondo la validazione esistente. Non si aggiungono dipendenze e non si cambia lo stack.

## Piano e interventi effettuati

1. **Baseline e ricognizione**
   - Verificare le istruzioni di repository e test, il worktree e i file coinvolti nel dominio, nelle pagine e nei test.
   - Eseguire `.\mvnw.cmd test` prima dell'implementazione.
   - In questa esecuzione iniziale la compilazione risultava non riuscita a causa di package/import incoerenti tra `xyz.mygor.magazzino...`, `com.epicode.magazzino...` e il package poi uniformato. Dopo l'allineamento del repository a `com.epicode.magazzino`, la baseline è stata ripetuta con esito positivo (22 test).

2. **Dominio**
   - In `RigaOrdine`, aggiungere le fasce di sconto e l'accessore `scontoPercentuale()`.
   - Calcolare il subtotale scontato su importo lordo × percentuale residua, arrotondando solo il risultato finale con `HALF_UP`.
   - In `Ordine`, mantenere `totale()` come totale merce scontato e aggiungere `spedizione()` e `totaleDaPagare()`.
   - Usare la quantità cumulativa già rappresentata nella riga, sfruttando il comportamento preesistente che consolida il medesimo prodotto.

3. **Interfaccia**
   - In `src/main/resources/templates/ordine-dettaglio.html`, mostrare la percentuale per riga e il riepilogo con totale merce, spedizione e totale da pagare.
   - In `src/main/resources/templates/ordini.html`, usare il totale da pagare.
   - Conservare la formattazione italiana e gli importi a due decimali.

4. **Test delle regole**
   - In `OrdineTest`, verificare le fasce R1 ai tre punti delle soglie: 9/10/11 e 49/50/51.
   - Verificare R2 con un importo che richiede arrotondamento `HALF_UP`; R3 con due aggiunte da 5 pezzi; R4 con subtotali scontati e attesi letterali; R5 con importi merce 149,99 € e 150,00 €.
   - Verificare il rifiuto di quantità zero e negativa senza variazione della giacenza o creazione di righe.
   - In `PagineTest`, verificare che dettaglio ed elenco rendano i valori attesi per un ordine.

5. **Verifiche**
   - Eseguire test mirati e la suite completa.
   - Modificare temporaneamente la soglia R1 da 10 a 11 e verificare che i test rilevino la regressione; ripristinare poi la soglia corretta e ripetere la suite.
   - La mutazione ha fatto fallire 4 test di `OrdineTest`; ripristinata la soglia 10, la suite completa è passata con 30 test.

## File interessati dall'implementazione

- `src/main/java/com/epicode/magazzino/model/RigaOrdine.java`
- `src/main/java/com/epicode/magazzino/model/Ordine.java`
- `src/main/resources/templates/ordine-dettaglio.html`
- `src/main/resources/templates/ordini.html`
- `src/test/java/com/epicode/magazzino/model/OrdineTest.java`
- `src/test/java/com/epicode/magazzino/web/PagineTest.java`

## Comandi e risultati verificati

- `.\mvnw.cmd -Dtest=OrdineTest test` — superato dopo i test di dominio.
- `.\mvnw.cmd -Dtest=PagineTest test` — superato dopo l'aggiornamento delle pagine.
- `.\mvnw.cmd test` — superato, 30 test, zero fallimenti ed errori dopo il ripristino della mutazione.
- `.\mvnw.cmd spotless:check` — non superato: segnala violazioni di formattazione in 25 file. Non è stata eseguita una riformattazione globale per evitare modifiche non pertinenti.

## Completamento del flusso

È stato predisposto `.github/prompts/test-regole.prompt.md` e ne è stata applicata manualmente la checklist ai test e all'implementazione correnti:

| Regola | Test individuati | Esito | Problemi |
|---|---|---|---|
| R1 | `loScontoSegueLeFasceConTreValoriIntornoAlleSoglie` | Coperta | I tre punti 9/10/11 e 49/50/51 verificano gli sconti 0%, 5% e 10%. |
| R2 | `ilSubtotaleScontatoArrotondaIlRisultatoFinaleConHalfUp` | Coperta | 0,11 € × 10 con sconto 5% produce 1,045 €, atteso 1,05 €. Il caso distingue `HALF_UP` e fallirebbe se lo sconto 0,055 € fosse arrotondato separatamente prima della sottrazione. |
| R3 | `scontoDelloStessoProdottoSiBasaSullaQuantitaComplessiva` | Coperta | Verifica una riga da 10, 5% e subtotale 65,55 € dopo due aggiunte da 5. |
| R4 | `ilTotaleMerceELaSommaDeiSubtotaliScontati` | Coperta | Verifica il totale letterale 110,55 € per righe scontate da 65,55 € e 45,00 €. |
| R5 | `laSpedizioneSiPagaSottoLaSogliaDiCentoECinquantaEuro`, `laSpedizioneEGratuitaDaCentoECinquantaEuro` | Coperta | Verificati 149,99 €/9,90 €/159,89 € e 150,00 €/0,00 €/150,00 €. |
| R6 | `dettaglioEdElencoMostranoScontiSpedizioneETotaleDaPagare` | Coperta | Verifica il dettaglio e il totale pagabile nell'elenco con valori formattati italiani. |

La checklist non ha rilevato assert tautologici. Questa è una revisione manuale basata sui file letti; il prompt è disponibile per l'invocazione esplicita `/test-regole` nell'interfaccia Copilot. La verifica interattiva in Ask descritta nell'esercizio non è stata eseguita in questo ambiente.

Non sono stati creati branch, commit, push o pull request: queste operazioni remote non fanno parte degli interventi eseguiti.

# Piano dettagliato — Sconti per quantità e spedizione

## Problema e approccio

Implementare la feature descritta in `docs/corso/pre-piano.md`  rispettando anche le regole e le disambiguazioni di `docs/corso/M6-G1.md` (fino alla riga 48). Il lavoro comprende verifica iniziale, calcolo di dominio, presentazione web, test sulle regole, controllo con mutazione e completamento degli artefatti dell'esercizio. Non cambiare stack né aggiungere dipendenze.

La logica monetaria resta nel dominio esistente (`RigaOrdine` e `Ordine`), mantenendo `BigDecimal` e le scale correnti. Riutilizzare la consolidazione delle righe per prodotto già presente: lo sconto deve basarsi sulla quantità complessiva della riga. Le quantità non positive sono già rifiutate dal modello prodotto; conservare questa validazione e non inventare un comportamento di ordine valido senza sconto.

## Interventi previsti

1. **Ricognizione e baseline**
   - Leggere le istruzioni del repository e i componenti coinvolti: `RigaOrdine`, `Ordine`, service/controller ordini, template `ordine-dettaglio.html` e `ordini.html`, test di modello/service/web.
   - Verificare lo stato del repository e non sovrascrivere modifiche preesistenti.
   - Eseguire `.\mvnw.cmd test` prima delle modifiche; distinguere eventuali problemi di baseline da regressioni introdotte.
   - Annotare nel piano di implementazione la struttura effettiva dei test e il punto d'ingresso per mostrare il modello nelle pagine.

2. **Regole di prezzo e sconto**
   - In `RigaOrdine`, derivare la percentuale dalla quantità totale corrente: 0% per 1–9, 5% per 10–49, 10% da 50 in su.
   - Calcolare la base lorda come prezzo unitario × quantità e poi applicare la percentuale, senza arrotondamenti intermedi; arrotondare il subtotale finale a due decimali con `RoundingMode.HALF_UP`.
   - Esporre un accessore della percentuale, adatto alla visualizzazione nel template.
   - Verificare la semantica di `BigDecimal`/scale e mantenere i valori monetari nel formato e nella precisione esistenti.
   - In `Ordine`, mantenere il totale merce come somma dei subtotali scontati; calcolare la spedizione a 9,90 € sotto 150,00 € e a 0,00 € da 150,00 € in su; calcolare il totale da pagare come somma dei due.
   - Scegliere nomi distinti e non ambigui per totale merce, spedizione e totale da pagare, preservando compatibilità con gli usi esistenti oppure aggiornandoli tutti.

3. **Test di dominio e di servizio**
   - Aggiungere o aggiornare test con valori attesi espliciti e letterali:
     - R1: sconti corretti ai confini 9/10 e 49/50 (includere i valori adiacenti se utile a rendere il confine evidente).
     - R2: quantità/prezzo che producano un importo con terza cifra decimale esattamente a metà, per dimostrare `HALF_UP`; verificare il subtotale risultante.
     - R3: stesso prodotto aggiunto due volte da 5 unità, una sola riga da 10 unità e sconto del 5%.
     - R4: totale merce uguale al valore letterale atteso della somma dei subtotali già scontati.
     - R5: costruire ordini il cui totale merce sia esattamente 149,99 € e 150,00 €; verificare spedizione e totale finale per entrambi.
   - Verificare anche che le quantità <= 0 continuino a essere rifiutate dalla validazione esistente, senza ridefinire la regola commerciale.
   - Evitare test tautologici: non derivare l'atteso invocando gli stessi metodi di produzione sotto test.

4. **Pagine e test web**
   - In `ordine-dettaglio.html`, rendere visibile lo sconto percentuale per ciascuna riga e mostrare separatamente totale merce, spedizione e totale da pagare.
   - In `ordini.html`, mostrare il totale da pagare.
   - Mantenere formattazione italiana, simbolo euro e due decimali; controllare che i nomi dei metodi esposti siano accessibili da Thymeleaf.
   - Aggiornare i test web per verificare valori e diciture renderizzati nelle due pagine, non solo che la risposta HTTP abbia successo.

5. **Verifica e mutazione**
   - Eseguire i test mirati mentre si sviluppa e infine `.\mvnw.cmd test`.
   - Rivedere il diff per assicurarsi che le formule, le soglie e i template corrispondano a R1–R6.
   - Effettuare la mutazione richiesta: cambiare temporaneamente la soglia di quantità da 10 a 11 nell'implementazione di R1; verificare che un test fallisca; ripristinare 10 e rieseguire i test.
   - Non lasciare nel diff la mutazione temporanea. Non dichiarare verdi test che non sono stati eseguiti.

6. **Artefatti del flusso formativo**
   - Salvare il piano finale approvato in `docs/piani/sconti-quantita.md`.
   - Creare `.github/prompts/test-regole.prompt.md` e usarlo (`/test-regole`) per controllare copertura di R1–R5, confini e assenza di test tautologici.
   - Seguire i passaggi del documento formativo per istruzioni Copilot/Ask e revisione manuale dei file generati; non trattarli come completati se non eseguiti.
   - Se richiesto come parte dell'esecuzione effettiva, creare il branch `feature/sconti-quantita`, commit, push e PR sul repository dell'utente, descrivendo cambiamenti, contributo dell'agente e correzioni umane. Non eseguire operazioni remote o commit senza l'autorizzazione/contesto necessario.

## Prompt operativo pronto da dare all'agente

```text
Implementa la feature descritta in docs/corso/M6-G1.md, usando docs/corso/pre-piano.md come piano di lavoro e rispettandone i passi fino alla riga 48.

Prima di modificare:
1. leggi le istruzioni del repository e i file coinvolti nel dominio, nei controller/template e nei test;
2. controlla lo stato del worktree senza annullare modifiche preesistenti;
3. esegui .\mvnw.cmd test e riporta eventuali problemi già presenti.

Requisiti funzionali:
- sconto per quantità totale della riga: 0% da 1 a 9, 5% da 10 a 49, 10% da 50 in su;
- consolidare le aggiunte dello stesso prodotto prima di calcolare la fascia di sconto;
- subtotale = prezzo unitario × quantità, meno sconto; usa BigDecimal, nessun arrotondamento intermedio e arrotonda il risultato finale a 2 decimali con HALF_UP;
- totale merce = somma dei subtotali scontati;
- spedizione 9,90 € se il totale merce è sotto 150,00 €, altrimenti 0,00 €; totale da pagare = totale merce + spedizione;
- nel dettaglio mostra la percentuale per riga e totale merce, spedizione e totale da pagare; nell'elenco mostra il totale da pagare.

Mantieni lo stack, le convenzioni, le scale monetarie e la validazione esistenti; non aggiungere dipendenze. Le quantità <= 0 sono già invalide nel dominio: mantieni il rifiuto e non trattarle come ordini validi senza sconto. Prima di introdurre nuovi metodi o modificare API interne, cerca gli usi esistenti e aggiornali coerentemente.

Aggiungi test con valori attesi letterali, non calcolati tramite il codice di produzione:
- confini di sconto 9/10 e 49/50;
- arrotondamento HALF_UP con un caso che produca una frazione di centesimo a metà;
- due aggiunte da 5 pezzi dello stesso prodotto che risultano in una riga da 10 con sconto 5%;
- totale merce dato da subtotali scontati;
- ordini con totale merce 149,99 € e 150,00 €, verificando spedizione e totale pagabile;
- verifica web dei dati mostrati in dettaglio ed elenco.

Esegui i test pertinenti e poi .\mvnw.cmd test. Esegui la mutazione temporanea 10 → 11 nella soglia d'implementazione R1 e dimostra che almeno un test fallisce; ripristina 10 e riesegui i test. Ispeziona il diff finale e riporta file modificati, test eseguiti e risultati. Non fare commit, push o PR in assenza di una richiesta esplicita.
```

## Considerazioni e punti da confermare

- La specifica dice che non si applicano sconti con quantità <= 0, mentre il dominio già rifiuta tali quantità; il piano assume che il rifiuto resti il comportamento corretto.
- La frase sull'arrotondamento è interpretata come nessun arrotondamento intermedio e arrotondamento del subtotale finale a due decimali.
- Il perimetro richiesto in questa pianificazione è il contenuto del pre-piano fino alla riga 48, integrato dalle specifiche/disambiguazioni del documento M6-G1. Il flusso branch/commit/PR è descritto ma non avviato automaticamente.

# Pre-piano — Sconti per quantità e spedizione gratuita

## Obiettivo

Attuare la specifica in `docs/corso/M6-G1.md`: applicare sconti alle righe in base alla quantità, calcolare la spedizione in base al totale merce e mostrare i nuovi importi nei dettagli e nell’elenco degli ordini.

## Passi

1. **Verificare lo stato iniziale**
    - Esaminare il modello degli ordini, il servizio, le pagine Thymeleaf e i test esistenti.
    - Eseguire `.\mvnw.cmd test` prima di modificare il codice e annotare eventuali problemi preesistenti.
    - Mantenere lo stack e la struttura già presenti; non aggiungere dipendenze.

2. **Definire i calcoli di dominio**
    - In `RigaOrdine`, calcolare lo sconto in base alla quantità complessiva della riga: 0% da 1 a 9 pezzi, 5% da 10 a 49, 10% da 50 in su.
    - Assicurarsi che aggiunte ripetute dello stesso prodotto confluiscano nella stessa riga prima di determinare lo sconto.
    - Calcolare il subtotale come prezzo unitario × quantità, applicando lo sconto e arrotondando al centesimo con `HALF_UP`.
    - Esporre la percentuale di sconto in modo che la pagina di dettaglio possa visualizzarla.
    - In `Ordine`, distinguere il totale merce scontato, la spedizione e il totale da pagare. La spedizione è 9,90 € se il totale merce è inferiore a 150,00 €, altrimenti è gratuita.
    - Usare `BigDecimal` per gli importi e mantenere i calcoli coerenti con le scale monetarie esistenti.

3. **Aggiornare le pagine**
    - In `ordine-dettaglio.html`, mostrare la percentuale di sconto per ogni riga e, in fondo, totale merce, spedizione e totale da pagare.
    - In `ordini.html`, mostrare il totale da pagare, non il solo totale merce.
    - Conservare il formato italiano degli importi e verificare che i dati siano disponibili attraverso il modello esposto dal controller.

4. **Aggiungere test per le regole**
    - Estendere i test di dominio e/o di servizio per coprire:
        - R1: quantità 9, 10, 49 e 50, includendo le percentuali attese;
        - R2: subtotale scontato e arrotondamento `HALF_UP`;
        - R3: due aggiunte dello stesso prodotto da 5 pezzi che producono una riga da 10 pezzi con sconto del 5%;
        - R4: totale merce uguale alla somma dei subtotali scontati;
        - R5: soglia di spedizione a 149,99 € e 150,00 €, inclusi totale da pagare e spedizione.
    - Aggiungere test web per verificare che dettaglio ed elenco mostrino gli importi richiesti da R6.
    - Usare valori attesi letterali nei test; evitare verifiche tautologiche.

5. **Verificare e rivedere**
    - Eseguire `.\mvnw.cmd test` e correggere eventuali regressioni.
    - Controllare le modifiche ai calcoli e alle pagine, verificando che quantità, importi e soglie coincidano con la specifica.
    - Cambiare temporaneamente la soglia `10` in `11`: almeno un test deve fallire. Ripristinare `10` e rieseguire i test.

6. **Completare il flusso dell’esercizio**
    - Salvare il piano approvato in `docs/piani/sconti-quantita.md`.
    - Predisporre `.github/prompts/test-regole.prompt.md` per controllare la copertura delle regole e dei confini.
    - Creare il branch `feature/sconti-quantita`, effettuare commit e push e aprire una pull request sul proprio repository.
    - Nella descrizione della PR, indicare cosa è stato implementato e quali correzioni sono state apportate rispetto alla proposta dell’agente.

## Criteri di completamento

- `.\mvnw.cmd test` termina con esito positivo.
- Sono coperti R1–R5, i confini 9/10, 49/50 e 149,99/150,00 e la visualizzazione richiesta da R6.
- La mutazione della soglia da 10 a 11 fa fallire almeno un test.
- Il piano finale è presente in `docs/piani/` e la pull request documenta le modifiche.

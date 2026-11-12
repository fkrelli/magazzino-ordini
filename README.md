# Magazzino & Ordini

L'applicazione del **Modulo 6** del corso *Agentic Software Development*: un piccolo
gestionale di ordini e magazzino su cui lavorano gli agenti di **GitHub Copilot**
(agent mode, plan mode, cloud agent, code review).

È volutamente semplice: il protagonista del modulo non è il codice ma come lo si fa
scrivere, verificare e rivedere agli agenti.

## Stack

| | |
|---|---|
| Linguaggio | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA) |
| Pagine | Thymeleaf (template HTML lato server) |
| Database | H2 in memoria: riparte dal catalogo iniziale a ogni avvio |
| Test | JUnit 5, Spring Boot Test, MockMvc |
| Build | Maven Wrapper (`mvnw`) |

## Metriche e monitoraggio

Micrometer espone le metriche HTTP, JVM e di processo, oltre alle metriche
applicative per ordini, riassortimenti, scorte e tool MCP. In esecuzione web:

- `GET /actuator/metrics` elenca le metriche disponibili e permette di
  consultarne una;
- `GET /actuator/prometheus` espone le metriche per lo scraping Prometheus;
- `GET /actuator/health` espone lo stato di salute senza dettagli interni.

Le metriche applicative non includono clienti, codici prodotto, ID ordine o
messaggi di errore come tag. Il numero di prodotti sotto scorta e il numero
totale dei prodotti sono gauge aggregati aggiornati ogni 60 secondi.

Gli endpoint Actuator non hanno autenticazione configurata dall'applicazione:
in ambienti esposti in rete, limitarli alla rete di monitoraggio o proteggerli
tramite l'infrastruttura prima di renderli raggiungibili.

### Stack locale con Grafana

Servono Docker Engine e Docker Compose. Copiare `.env.example` in `.env`
e compilare `GRAFANA_ADMIN_USER` e `GRAFANA_ADMIN_PASSWORD` con credenziali
locali personali (non inserire segreti nel repository). Per esempio:

```powershell
Copy-Item .env.example .env
# Modificare .env con il proprio editor prima di avviare.
docker compose up --build -d
```

Le porte sono legate a `127.0.0.1`: applicazione
<http://127.0.0.1:8080>, Prometheus <http://127.0.0.1:9090/targets> e
Grafana <http://127.0.0.1:3000>. Accedere a Grafana con le credenziali
impostate e aprire la dashboard **Magazzino & Ordini**; il datasource
Prometheus è provisionato automaticamente. Prometheus interroga
`app:8080/actuator/prometheus` ogni 15 secondi sulla rete Compose.

Visitare le pagine e usare i tool MCP per generare traffico: i contatori
appaiono solo dopo la prima operazione e rate e p95 richiedono almeno due
scrape con dati. Le scorte sono aggiornate ogni 60 secondi. I p95 sono stime
da histogram e `increase` è una stima sull'intervallo, non un conteggio esatto.

```powershell
docker compose down           # arresta i servizi, conserva i volumi
```

I volumi nominati conservano lo storico Prometheus e i dati Grafana.
Le credenziali iniziali Grafana sono applicate solo alla creazione del database
Grafana: modificare `.env` non cambia la password di un utente già esistente.
Il database H2 dell'applicazione è invece effimero: a ogni riavvio del
container riparte dal catalogo iniziale e gli ordini e le modifiche vanno persi.
Il setup è solo per sviluppo locale: non comprende TLS, alerting né
autenticazione adatta alla produzione.

## Avvio

```powershell
.\mvnw.cmd test              # tutti i test devono essere verdi
.\mvnw.cmd spotless:apply    # formatta il codice Java
.\mvnw.cmd verify            # test + stile + soglia JaCoCo
.\mvnw.cmd spring-boot:run   # poi apri http://localhost:8080
```

`verify` genera il report JaCoCo in `target/site/jacoco/index.html` e richiede
almeno il 60% di copertura sia delle righe sia dei rami sul codice di produzione.
Il workflow GitHub Actions esegue `verify`, quindi applica la stessa soglia a
ogni build CI. Il solo comando `test` esegue i test ma non verifica la soglia.

## Come è fatto

```
src/main/java/com/epicode/magazzino
├── model/        entità e regole di business (Prodotto, Ordine, RigaOrdine, StatoOrdine, eccezioni)
├── repository/   Spring Data JPA
├── service/      casi d'uso transazionali (CatalogoService, OrdineService, NuovoOrdine)
├── web/          controller Thymeleaf, form, gestione errori
└── config/       orologio (Clock) e dati iniziali
src/main/resources/templates   le pagine Thymeleaf
src/test/java                  test di dominio (senza Spring) e di integrazione
```

Il flusso di una richiesta: **controller → service → model/repository**. Il controller
non contiene regole; le regole stanno nel model e i service le orchestrano in una
transazione.

## Regole di business già presenti

- Un ordine nasce **CONFERMATO**; può diventare **SPEDITO** oppure **ANNULLATO**, nient'altro.
- Ogni riga preleva i pezzi dal magazzino; se non bastano l'ordine non viene creato.
- Lo stesso prodotto inserito due volte finisce nella stessa riga.
- Il prezzo della riga è quello del prodotto al momento dell'ordine.
- Annullare restituisce i pezzi al magazzino; un ordine spedito non si annulla.
- Importi in `BigDecimal`, arrotondati al centesimo.

## Usarlo come template (docente)

1. Pubblica il repo su GitHub e attiva **Settings → Template repository**.
2. Ogni studente: **Use this template → Create a new repository**, proprietario il
   proprio account, visibilità **Public** (serve per CodeQL e secret scanning gratuiti
   nella giornata 3), senza "Include all branches".
3. Il branch `soluzione/m6-g1` e i tag restano nel repo del docente: un repository
   creato dal template parte dal solo `main`.

Lo scheletro **non** contiene `.github/copilot-instructions.md`: scriverlo è il primo
esercizio della giornata 1.

## Giornate

| Giornata | Consegna |
|---|---|
| M6 · G1 — Feature authoring e test generation | [docs/corso/M6-G1.md](docs/corso/M6-G1.md) · soluzione: branch `soluzione/m6-g1`, tag `m6-g1-end` |
| M6 · G2 — Code review automatizzata e architecture compliance | [docs/corso/M6-G2.md](docs/corso/M6-G2.md) · soluzione: branch `soluzione/m6-g2`, tag `m6-g2-end` |
| M6 · G3 — Security scanning e pre-deploy | [docs/corso/M6-G3.md](docs/corso/M6-G3.md) · codice del fornitore: tag `m6-g3-fornitore` · soluzione: branch `soluzione/m6-g3`, tag `m6-g3-end` |
| M6 · G4 — Composizione e coordinamento di più agenti | [docs/corso/M6-G4.md](docs/corso/M6-G4.md) · soluzione: branch `soluzione/m6-g4`, tag `m6-g4-end` |
| M7 · G1 — Architettura e ruolo degli MCP server | [docs/corso/M7-G1.md](docs/corso/M7-G1.md) · soluzione: branch `soluzione/m7-g1`, tag `m7-g1-end` |
| M7 · G2 — Progettazione e configurazione di un MCP server | [docs/corso/M7-G2.md](docs/corso/M7-G2.md) · soluzione: branch `soluzione/m7-g2`, tag `m7-g2-end` |
| M7 · G3 — Integrazione con la toolchain e la pipeline | [docs/corso/M7-G3.md](docs/corso/M7-G3.md) · soluzione: branch `soluzione/m7-g3`, tag `m7-g3-end` |

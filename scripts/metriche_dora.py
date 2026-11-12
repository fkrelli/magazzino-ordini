#!/usr/bin/env python3
"""Report DORA basato sulle API REST del repository corrente."""

import json
import os
import statistics
import sys
from datetime import datetime, timedelta, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


def data(valore):
    return datetime.fromisoformat(valore.replace("Z", "+00:00"))


class GitHub:
    def __init__(self, base, repository, token):
        self.base = base.rstrip("/") + "/repos/" + repository
        self.token = token

    def get(self, percorso, parametri=None):
        url = self.base + percorso
        if parametri:
            url += "?" + urlencode(parametri)
        richiesta = Request(
            url,
            headers={
                "Authorization": "Bearer " + self.token,
                "Accept": "application/vnd.github+json",
                "X-GitHub-Api-Version": "2022-11-28",
                "User-Agent": "magazzino-metriche-dora",
            },
        )
        with urlopen(richiesta, timeout=30) as risposta:
            return json.load(risposta)

    def elenco(self, percorso, parametri=None):
        parametri = dict(parametri or {})
        parametri["per_page"] = 100
        pagina = 1
        while True:
            parametri["page"] = pagina
            elementi = self.get(percorso, parametri)
            if not isinstance(elementi, list):
                raise ValueError("Risposta GitHub non valida: atteso un elenco")
            yield from elementi
            if len(elementi) < 100:
                return
            pagina += 1


def raccogli(github, fine):
    inizio = fine - timedelta(days=30)
    deploy = []
    for elemento in github.elenco("/deployments", {"environment": "produzione"}):
        stati = github.elenco(f"/deployments/{elemento['id']}/statuses")
        successi = [data(s["created_at"]) for s in stati if s["state"] == "success"]
        if not successi:
            continue
        successo = min(successi)
        if inizio <= successo < fine:
            commit = github.get(f"/commits/{elemento['sha']}")
            commit_data = data(commit["commit"]["committer"]["date"])
            if commit_data > successo:
                raise ValueError("La data del commit è successiva al deploy")
            deploy.append((successo, commit_data))

    incidenti = []
    for issue in github.elenco("/issues", {"state": "all", "since": inizio.isoformat()}):
        if "pull_request" in issue or "incident" not in issue["title"].casefold():
            continue
        chiusura = issue["closed_at"]
        if chiusura and inizio <= data(chiusura) < fine:
            apertura = data(issue["created_at"])
            if apertura > data(chiusura):
                raise ValueError("La data di apertura è successiva alla chiusura")
            incidenti.append((apertura, data(chiusura)))
    return inizio, deploy, incidenti


def media_ore(durate):
    if not durate:
        return "Non disponibile (nessun dato nel periodo)"
    return f"{statistics.mean(d.total_seconds() for d in durate) / 3600:.2f} ore"


def report(inizio, fine, deploy, incidenti):
    lead = media_ore([successo - commit for successo, commit in deploy])
    ripristino = media_ore([chiusura - apertura for apertura, chiusura in incidenti])
    return f"""# Metriche DORA — ultimi 30 giorni

Periodo UTC: da {inizio.isoformat()} incluso a {fine.isoformat()} escluso.

| Domanda | Risultato |
| --- | --- |
| Con quale frequenza rilasciamo? | {len(deploy)} deployment riusciti in 30 giorni ({len(deploy) / 30:.2f} al giorno) |
| Quanto tempo passa dal cambiamento al deploy? | {lead} — proxy: data del commit puntato dal deployment fino al primo stato success |
| Quale percentuale dei cambiamenti causa incidenti? | Non disponibile: le issue non sono collegate ai deployment responsabili |
| Quanto tempo serve per ripristinare il servizio? | {ripristino} — proxy: apertura fino a chiusura della issue; {len(incidenti)} issue chiuse |

**Limiti:** l'environment `produzione` include anche il job che crea release in bozza,
che non effettua un deploy reale. La frequenza e il proxy del lead time possono quindi
includere falsi deploy. Il proxy considera solo il commit puntato, non tutte le
modifiche contenute nel rilascio. Gli incidenti sono issue il cui titolo contiene
`incident`, senza distinzione tra maiuscole e minuscole: apertura e chiusura sono
solo approssimazioni e le issue riaperte ancora aperte non sono incluse.
"""


def main():
    github = GitHub(
        os.environ.get("GITHUB_API_URL", "https://api.github.com"),
        os.environ["GITHUB_REPOSITORY"],
        os.environ["GH_TOKEN"],
    )
    fine = datetime.now(timezone.utc)
    inizio, deploy, incidenti = raccogli(github, fine)
    testo = report(inizio, fine, deploy, incidenti)
    print(testo)
    if "GITHUB_STEP_SUMMARY" in os.environ:
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as file:
            file.write(testo)


if __name__ == "__main__":
    try:
        main()
    except (HTTPError, URLError, OSError, KeyError, ValueError) as errore:
        print(f"Errore nella generazione del report DORA: {errore}", file=sys.stderr)
        sys.exit(1)

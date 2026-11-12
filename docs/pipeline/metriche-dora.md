# Report DORA in GitHub Actions

Il workflow **Metriche DORA** parte ogni giorno alle 15:55 nel fuso
`Europe/Rome`, considerando l'ora legale. È avviabile anche da Actions →
Metriche DORA → Run workflow. Il workflow deve essere presente sul branch
predefinito perché la pianificazione funzioni. GitHub può ritardare o saltare
esecuzioni programmate; nei repository pubblici inattivi può disabilitarle.

Il risultato compare nel riepilogo e nei log dell'esecuzione. Lo script usa
solo le API REST del repository e il `GITHUB_TOKEN` del workflow con permessi
di sola lettura. Eventuali errori API fanno fallire il job.

Il periodo è una finestra mobile di 30 giorni esatti, con inizio incluso e fine
esclusa. Il report usa:

- **Frequenza**: deployment nell'environment `produzione` con un primo stato
  `success` nel periodo, contati una volta per deployment.
- **Lead time, proxy**: media del tempo tra la data del committer del commit
  puntato e il primo stato `success` dei deployment nel periodo. Non misura
  tutti i commit di un rilascio e può contare lo stesso commit più volte.
- **Change failure rate**: non disponibile, perché le issue non identificano
  il deployment responsabile. I fallimenti tecnici del job non sono una misura
  equivalente.
- **Tempo di ripristino, proxy**: media del tempo tra apertura e chiusura delle
  issue con `incident` nel titolo, senza distinguere maiuscole e minuscole,
  chiuse nel periodo. Sono escluse le PR e le issue ancora aperte o riaperte.
  La data della issue non necessariamente coincide con l'incidente reale.

**Attenzione:** il job di release in bozza usa già `produzione`, ma non
distribuisce l'applicazione. I suoi deployment possono quindi essere conteggiati:
frequenza e lead time non sono misure attendibili dei deploy effettivi finché
gli eventi dei deploy reali non saranno distinguibili dalle bozze.
Gli stati storici dei deployment sono soggetti alla conservazione di GitHub:
una storia incompleta può alterare i risultati. Il report non deduce dati
mancanti e non equivale a una valutazione individuale delle persone.

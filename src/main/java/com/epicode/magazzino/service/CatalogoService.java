package com.epicode.magazzino.service;

import com.epicode.magazzino.model.MovimentoMagazzino;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.Riassortimento;
import com.epicode.magazzino.model.RichiestaNonValidaException;
import com.epicode.magazzino.model.RisorsaNonTrovataException;
import com.epicode.magazzino.repository.MovimentoRepository;
import com.epicode.magazzino.repository.ProdottoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogoService {

  private final ProdottoRepository prodotti;
  private final MovimentoRepository movimenti;
  private final Clock clock;
  private final MetricheApplicative metriche;

  public CatalogoService(ProdottoRepository prodotti,
      MovimentoRepository movimenti, Clock clock,
      MetricheApplicative metriche) {
    this.prodotti = prodotti;
    this.movimenti = movimenti;
    this.clock = clock;
    this.metriche = metriche;
  }

  @Transactional(readOnly = true)
  public List<Prodotto> elenco() {
    return metriche.misuraCaso("catalogo.elenco", () -> {
      System.out.println("Elenco prodotti");
      return prodotti.findAllByOrderByNomeAsc();
    });
  }

  /**
   * Prodotti il cui codice o nome contiene il testo, maiuscole indifferenti.
   * Servono almeno 2 caratteri: una ricerca vuota restituirebbe tutto.
   */
  @Transactional(readOnly = true)
  public List<Prodotto> cerca(String testo) {
    return metriche.misuraCaso("catalogo.cerca", () -> {
      String cercato = testo == null
          ? ""
          : testo.strip().toLowerCase(Locale.ROOT);
      if (cercato.length() < 2) {
        throw new RichiestaNonValidaException(
            "Scrivi almeno 2 caratteri da cercare");
      }
      return prodotti.findAllByOrderByNomeAsc().stream()
          .filter(p -> p.getCodice().toLowerCase(Locale.ROOT).contains(cercato)
              || p.getNome().toLowerCase(Locale.ROOT).contains(cercato))
          .toList();
    });
  }

  /**
   * I prodotti da riassortire (giacenza uguale o sotto la scorta minima), dal
   * più scoperto al meno scoperto.
   */
  @Transactional(readOnly = true)
  public List<Prodotto> daRiassortire() {
    return metriche
        .misuraCaso("catalogo.sotto_scorta",
            () -> prodotti.findAllByOrderByNomeAsc().stream()
                .filter(Prodotto::daRiassortire)
                .sorted(Comparator.comparingInt(
                    (Prodotto p) -> p.getScortaMinima() - p.getDisponibili())
                    .reversed().thenComparing(Prodotto::getCodice))
                .toList());
  }

  @Transactional(readOnly = true)
  public Prodotto trova(String codice) {
    return metriche.misuraCaso("catalogo.trova", () -> trovaProdotto(codice));
  }

  private Prodotto trovaProdotto(String codice) {
    return prodotti.findByCodice(codice)
        .orElseThrow(() -> new RisorsaNonTrovataException(
            "Prodotto " + codice + " non trovato"));
  }

  /**
   * R2-R4: la richiesta è già validata da Riassortimento; qui giacenza e
   * movimento insieme.
   */
  public Prodotto riassortisci(String codice, Riassortimento richiesta) {
    return metriche.misuraCaso("catalogo.riassortisci", () -> {
      Prodotto prodotto = trovaProdotto(codice);
      prodotto.riassortisci(richiesta.quantita());
      movimenti.save(MovimentoMagazzino.carico(prodotto, richiesta,
          LocalDateTime.now(clock)));
      metriche.incrementaDopoCommit("magazzino.riassortimenti", "esito",
          "riuscito");
      metriche.incrementaDopoCommit("magazzino.riassortimenti.pezzi",
          richiesta.quantita(), "unita", "pezzi");
      return prodotto;
    });
  }

  /** R6: lo storico, dal movimento più recente. */
  @Transactional(readOnly = true)
  public List<MovimentoMagazzino> movimenti() {
    return metriche.misuraCaso("catalogo.movimenti",
        movimenti::findAllByOrderByCreatoIlDescIdDesc);
  }
}

package com.epicode.magazzino.service;

import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.repository.ProdottoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricheScorte {

  private final ProdottoRepository prodotti;
  private final AtomicInteger prodottiSottoScorta = new AtomicInteger();
  private final AtomicInteger prodottiTotali = new AtomicInteger();

  public MetricheScorte(ProdottoRepository prodotti, MeterRegistry registro) {
    this.prodotti = prodotti;
    Gauge
        .builder("magazzino.prodotti.sotto_scorta", prodottiSottoScorta,
            AtomicInteger::get)
        .description(
            "Numero di prodotti con giacenza uguale o inferiore alla scorta minima")
        .register(registro);
    Gauge
        .builder("magazzino.prodotti.totali", prodottiTotali,
            AtomicInteger::get)
        .description("Numero totale di prodotti a catalogo").register(registro);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void inizializza() {
    aggiorna();
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
  public void aggiorna() {
    List<Prodotto> elenco = prodotti.findAll();
    prodottiTotali.set(elenco.size());
    prodottiSottoScorta
        .set((int) elenco.stream().filter(Prodotto::daRiassortire).count());
  }
}

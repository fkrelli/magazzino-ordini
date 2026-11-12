package com.epicode.magazzino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.repository.ProdottoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MetricheScorteTest {

  private static final String CODICE_TEST = "T-METRICHE-SCORTA";

  @Autowired
  private ProdottoRepository prodotti;

  @Autowired
  private MeterRegistry registro;

  @Autowired
  private MetricheScorte metricheScorte;

  @Test
  void aggiornaIlGaugeAggregatoSenzaEtichettePerProdotto() {
    Gauge gauge = registro.find("magazzino.prodotti.sotto_scorta").gauge();
    double prima = gauge.value();
    Prodotto salvato = prodotti.save(new Prodotto(CODICE_TEST,
        "Prodotto metrico di test", new BigDecimal("1.00"), 0, 1));
    try {
      metricheScorte.aggiorna();

      assertEquals(prima + 1, gauge.value());
      assertTrue(gauge.getId().getTags().isEmpty());
    } finally {
      prodotti.deleteById(salvato.getId());
      metricheScorte.aggiorna();
    }
  }
}

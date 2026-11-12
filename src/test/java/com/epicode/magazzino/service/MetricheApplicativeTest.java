package com.epicode.magazzino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epicode.magazzino.model.RichiestaNonValidaException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MetricheApplicativeTest {

  private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
  private final MetricheApplicative metriche = new MetricheApplicative(
      registro);

  @Test
  void misuraUnCasoDusoRiuscito() {
    int risultato = metriche.misuraCaso("ordine.crea", () -> 3);

    assertEquals(3, risultato);
    assertEquals(1, registro.counter("magazzino.casi_uso", "operazione",
        "ordine.crea", "esito", "successo").count());
    assertEquals(1, registro.timer("magazzino.casi_uso.durata", "operazione",
        "ordine.crea", "esito", "successo").count());
  }

  @Test
  void misuraLErroreDiDominioESollevaLaStessaEccezione() {
    assertThrows(RichiestaNonValidaException.class,
        () -> metriche.misuraCaso("ordine.crea", () -> {
          throw new RichiestaNonValidaException("Richiesta non valida");
        }));

    assertEquals(1, registro.counter("magazzino.casi_uso", "operazione",
        "ordine.crea", "esito", "errore_dominio").count());
    assertEquals(1, registro.timer("magazzino.casi_uso.durata", "operazione",
        "ordine.crea", "esito", "errore_dominio").count());
  }

  @Test
  void incrementaSubitoQuandoNonEsisteUnaTransazione() {
    metriche.incrementaDopoCommit("magazzino.test", "caso",
        "senza_transazione");

    assertEquals(1, registro
        .counter("magazzino.test", "caso", "senza_transazione").count());
  }
}

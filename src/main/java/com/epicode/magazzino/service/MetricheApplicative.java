package com.epicode.magazzino.service;

import com.epicode.magazzino.model.MagazzinoException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class MetricheApplicative {

  private final MeterRegistry registro;

  public MetricheApplicative(MeterRegistry registro) {
    this.registro = registro;
  }

  public <T> T misuraCaso(String operazione, Supplier<T> caso) {
    return misura("magazzino.casi_uso", "magazzino.casi_uso.durata",
        "operazione", operazione, caso);
  }

  public <T> T misuraMcp(String strumento, Supplier<T> chiamata) {
    return misura("magazzino.mcp.chiamate", "magazzino.mcp.durata", "strumento",
        strumento, chiamata);
  }

  public void incrementaDopoCommit(String nome, String... tag) {
    incrementaDopoCommit(nome, 1, tag);
  }

  public void incrementaDopoCommit(String nome, double quantita,
      String... tag) {
    Runnable incremento = () -> registro.counter(nome, tag).increment(quantita);
    if (TransactionSynchronizationManager.isSynchronizationActive()
        && TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager
          .registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              incremento.run();
            }
          });
    } else {
      incremento.run();
    }
  }

  private <T> T misura(String nomeContatore, String nomeTimer, String tagNome,
      String tagValore, Supplier<T> azione) {
    Timer.Sample campione = Timer.start(registro);
    String esito = "successo";
    try {
      return azione.get();
    } catch (MagazzinoException e) {
      esito = "errore_dominio";
      throw e;
    } catch (RuntimeException e) {
      esito = "errore_tecnico";
      throw e;
    } finally {
      if ("successo".equals(esito)) {
        incrementaDopoCommit(nomeContatore, tagNome, tagValore, "esito", esito);
      } else {
        registro.counter(nomeContatore, tagNome, tagValore, "esito", esito)
            .increment();
      }
      campione
          .stop(registro.timer(nomeTimer, tagNome, tagValore, "esito", esito));
    }
  }
}

package com.epicode.magazzino.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** R1: il formato del codice, con i casi di confine scritti a mano. */
class CodiceTrackingTest {

  @Test
  void dueLettereNoveCifreDueLettere() {
    assertEquals("IT123456789IT", new CodiceTracking("IT123456789IT").valore());
  }

  @Test
  void minuscoleESpaziSiNormalizzano() {
    assertEquals("IT123456789IT",
        new CodiceTracking("  it123456789it ").valore());
  }

  @Test
  void ottoODieciCifreNo() {
    assertThrows(RichiestaNonValidaException.class,
        () -> new CodiceTracking("IT12345678IT"));
    assertThrows(RichiestaNonValidaException.class,
        () -> new CodiceTracking("IT1234567890IT"));
  }

  @Test
  void lettereECifreAlPostoSbagliatoNo() {
    assertThrows(RichiestaNonValidaException.class,
        () -> new CodiceTracking("1T123456789IT"));
    assertThrows(RichiestaNonValidaException.class,
        () -> new CodiceTracking("IT12345678AIT"));
  }

  @Test
  void vuotoNo() {
    assertThrows(RichiestaNonValidaException.class,
        () -> new CodiceTracking(""));
    assertThrows(RichiestaNonValidaException.class,
        () -> new CodiceTracking(null));
  }
}

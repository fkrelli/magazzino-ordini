package com.epicode.magazzino.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * R1: il codice di tracking del corriere. Due lettere, nove cifre, due lettere
 * (es. IT123456789IT). Maiuscole o minuscole indifferenti: si salva in
 * maiuscolo.
 */
public record CodiceTracking(String valore) {

  private static final Pattern FORMATO = Pattern
      .compile("[A-Z]{2}[0-9]{9}[A-Z]{2}");

  public CodiceTracking {
    valore = valore == null ? "" : valore.strip().toUpperCase(Locale.ROOT);
    if (!FORMATO.matcher(valore).matches()) {
      throw new RichiestaNonValidaException(
          "Codice di tracking non valido: servono 2 lettere, 9 cifre, 2 lettere");
    }
  }
}

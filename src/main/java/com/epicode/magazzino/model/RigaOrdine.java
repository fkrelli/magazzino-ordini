package com.epicode.magazzino.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un prodotto dentro un ordine, con il prezzo fissato al momento dell'ordine. */
@Entity
public class RigaOrdine {

    private static final int SOGLIA_SCONTO_MEDIO = 10;
    private static final int SOGLIA_SCONTO_MASSIMO = 50;
    private static final int SCONTO_MEDIO_PERCENTUALE = 5;
    private static final int SCONTO_MASSIMO_PERCENTUALE = 10;
    private static final BigDecimal CENTO = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Ordine ordine;

    @ManyToOne(optional = false)
    private Prodotto prodotto;

    private int quantita;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoUnitario;

    protected RigaOrdine() {
        // per JPA
    }

    RigaOrdine(Ordine ordine, Prodotto prodotto, int quantita) {
        this.ordine = ordine;
        this.prodotto = prodotto;
        this.quantita = quantita;
        this.prezzoUnitario = prodotto.getPrezzo();
    }

    void aumenta(int altri) {
        quantita += altri;
    }

    /** Percentuale di sconto determinata dalla quantità totale della riga. */
    public int scontoPercentuale() {
        if (quantita >= SOGLIA_SCONTO_MASSIMO) {
            return SCONTO_MASSIMO_PERCENTUALE;
        }
        if (quantita >= SOGLIA_SCONTO_MEDIO) {
            return SCONTO_MEDIO_PERCENTUALE;
        }
        return 0;
    }

    /** Subtotale scontato della riga, arrotondato al centesimo con HALF_UP. */
    public BigDecimal subtotale() {
        BigDecimal importoLordo = prezzoUnitario.multiply(BigDecimal.valueOf(quantita));
        BigDecimal importoScontato = importoLordo.multiply(
                CENTO.subtract(BigDecimal.valueOf(scontoPercentuale())))
                .divide(CENTO);
        return importoScontato
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public int getQuantita() {
        return quantita;
    }

    public BigDecimal getPrezzoUnitario() {
        return prezzoUnitario;
    }
}

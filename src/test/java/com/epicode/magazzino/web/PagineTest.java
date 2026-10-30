package com.epicode.magazzino.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.repository.ProdottoRepository;
import com.epicode.magazzino.service.CatalogoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/** Le pagine principali si aprono e il form crea davvero un ordine. */
@SpringBootTest
@AutoConfigureMockMvc
class PagineTest {

  @Autowired
  private MockMvc mvc;

  @MockitoSpyBean
  private CatalogoService catalogo;

  @Autowired
  private ProdottoRepository prodotti;

  @Test
  void elencoProdotti() throws Exception {
    mvc.perform(get("/prodotti")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Trapano a batteria 18V")))
        .andExpect(content().string(containsString("Da riassortire")))
        .andExpect(content().string(containsString("sottoscorta"))).andExpect(
            content().string(containsString("/prodotti/GUA-L/riassortimento")));
  }

  @Test
  @Transactional
  void elencoProdottiMostraIdNumericiNellaPrimaColonna() throws Exception {
    prodotti.save(new Prodotto("ID-TEST-1", "Prodotto ID test A",
        new BigDecimal("1.00"), 1, 0));
    prodotti.save(new Prodotto("ID-TEST-2", "Prodotto ID test B",
        new BigDecimal("1.00"), 1, 0));
    List<Prodotto> elencoAtteso = prodotti.findAllByOrderByNomeAsc();

    MvcResult risultato = mvc.perform(get("/prodotti"))
        .andExpect(status().isOk()).andReturn();
    String html = risultato.getResponse().getContentAsString();

    Matcher intestazioniMatcher =
        Pattern.compile("(?s)<th(?:\\s[^>]*)?>(.*?)</th>").matcher(html);
    List<String> intestazioni = new ArrayList<>();
    while (intestazioniMatcher.find()) {
      intestazioni.add(testoVisibile(intestazioniMatcher.group(1)));
    }
    assertEquals(List.of("ID", "Codice", "Nome", "Prezzo", "Disponibili",
        "Scorta minima", ""), intestazioni);

    Matcher righeMatcher =
        Pattern.compile("(?s)<tr(?:\\s[^>]*)?>(.*?)</tr>").matcher(html);
    assertTrue(righeMatcher.find(), "La tabella deve contenere l'intestazione");
    List<List<String>> righe = new ArrayList<>();
    while (righeMatcher.find()) {
      Matcher celleMatcher =
          Pattern.compile("(?s)<td(?:\\s[^>]*)?>(.*?)</td>")
              .matcher(righeMatcher.group(1));
      List<String> celle = new ArrayList<>();
      while (celleMatcher.find()) {
        celle.add(testoVisibile(celleMatcher.group(1)));
      }
      righe.add(celle);
    }

    assertEquals(elencoAtteso.size(), righe.size());
    assertTrue(elencoAtteso.stream().anyMatch(p -> p.getId() >= 10L),
        "Il test deve includere almeno un ID con più cifre");
    for (int indice = 0; indice < elencoAtteso.size(); indice++) {
      Prodotto prodotto = elencoAtteso.get(indice);
      assertEquals(7, righe.get(indice).size());
      assertEquals(prodotto.getId().toString(), righe.get(indice).get(0));
      assertEquals(prodotto.getCodice(), righe.get(indice).get(1));
    }
  }

  @Test
  void elencoProdottiVuotoMostraLeIntestazioniSenzaRighe() throws Exception {
    doReturn(List.of()).when(catalogo).elenco();

    String html = mvc.perform(get("/prodotti")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    Matcher intestazioniMatcher =
        Pattern.compile("(?s)<th(?:\\s[^>]*)?>(.*?)</th>").matcher(html);
    List<String> intestazioni = new ArrayList<>();
    while (intestazioniMatcher.find()) {
      intestazioni.add(testoVisibile(intestazioniMatcher.group(1)));
    }
    assertEquals(List.of("ID", "Codice", "Nome", "Prezzo", "Disponibili",
        "Scorta minima", ""), intestazioni);
    assertFalse(Pattern.compile("(?s)<tbody>\\s*<tr(?:\\s|>)").matcher(html)
        .find(), "Un elenco vuoto non deve contenere righe");
  }

  private String testoVisibile(String html) {
    return html.replaceAll("(?s)<[^>]*>", "").replaceAll("\\s+", " ").trim();
  }

  @Test
  void ilFormCreaUnOrdineEPortaAlDettaglio() throws Exception {
    mvc.perform(post("/ordini").param("cliente", "Ferramenta Bianchi")
        .param("righe[0].codice", "GUA-L").param("righe[0].quantita", "2"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("/ordini/*"));
  }

  @Test
  void ilFormMostraLErroreSeLaScortaNonBasta() throws Exception {
    mvc.perform(post("/ordini").param("cliente", "Ferramenta Bianchi")
        .param("righe[0].codice", "NAS-50").param("righe[0].quantita", "1"))
        .andExpect(status().isOk()).andExpect(content()
            .string(containsString("Scorta insufficiente per NAS-50")));
  }

  @Test
  void riassortimentoNonValidoMostraLErrore() throws Exception {
    int disponibili = catalogo.trova("GUA-L").getDisponibili();

    mvc.perform(post("/prodotti/GUA-L/riassortimento").param("quantita", "0"))
        .andExpect(status().isOk()).andExpect(content().string(
            containsString("La quantità da riassortire va da 1 a 1000")));

    assertEquals(disponibili, catalogo.trova("GUA-L").getDisponibili());
  }

  @Test
  void ilFormRiassortimentoMostraIConfiniAmmessi() throws Exception {
    mvc.perform(get("/prodotti/GUA-L/riassortimento"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("min=\"1\"")))
        .andExpect(content().string(containsString("max=\"1000\"")))
        .andExpect(content().string(containsString("step=\"1\"")))
        .andExpect(content().string(containsString("maxlength=\"250\"")));
  }

  @Test
  void riassortimentoValidoPortaAiMovimenti() throws Exception {
    mvc.perform(post("/prodotti/MET-5/riassortimento").param("quantita", "10")
        .param("nota", "Arrivo")).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/movimenti"));
    mvc.perform(get("/movimenti")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Arrivo")));
  }

  @Test
  void ordineInesistente() throws Exception {
    mvc.perform(get("/ordini/9999")).andExpect(status().isNotFound());
  }

  @Test
  void dettaglioEdElencoMostranoScontiSpedizioneETotaleDaPagare()
      throws Exception {
    MvcResult creazione = mvc
        .perform(post("/ordini").param("cliente", "Ferramenta Bianchi")
            .param("righe[0].codice", "GUA-L").param("righe[0].quantita", "10"))
        .andExpect(status().is3xxRedirection()).andReturn();
    String urlDettaglio = creazione.getResponse().getRedirectedUrl();

    mvc.perform(get(urlDettaglio)).andExpect(status().isOk())
        .andExpect(content().string(containsString("5%")))
        .andExpect(content().string(containsString("30,40 €")))
        .andExpect(content().string(containsString("Totale merce")))
        .andExpect(content().string(containsString("Spedizione")))
        .andExpect(content().string(containsString("9,90 €")))
        .andExpect(content().string(containsString("Totale da pagare")))
        .andExpect(content().string(containsString("40,30 €")));

    mvc.perform(get("/ordini")).andExpect(status().isOk())
        .andExpect(content().string(containsString("40,30 €")));
  }
}

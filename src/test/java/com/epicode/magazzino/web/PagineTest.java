package com.epicode.magazzino.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epicode.magazzino.service.CatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Le pagine principali si aprono e il form crea davvero un ordine. */
@SpringBootTest
@AutoConfigureMockMvc
class PagineTest {

        @Autowired
        private MockMvc mvc;

        @Autowired
        private CatalogoService catalogo;

        @Test
        void elencoProdotti() throws Exception {
                mvc.perform(get("/prodotti"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Trapano a batteria 18V")))
                                .andExpect(content().string(containsString("Da riassortire")))
                                .andExpect(content().string(containsString("sottoscorta")))
                                .andExpect(content().string(containsString("/prodotti/GUA-L/riassortimento")));
        }

        @Test
        void ilFormCreaUnOrdineEPortaAlDettaglio() throws Exception {
                mvc.perform(post("/ordini")
                                .param("cliente", "Ferramenta Bianchi")
                                .param("righe[0].codice", "GUA-L")
                                .param("righe[0].quantita", "2"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrlPattern("/ordini/*"));
        }

        @Test
        void ilFormMostraLErroreSeLaScortaNonBasta() throws Exception {
                mvc.perform(post("/ordini")
                                .param("cliente", "Ferramenta Bianchi")
                                .param("righe[0].codice", "NAS-50")
                                .param("righe[0].quantita", "1"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Scorta insufficiente per NAS-50")));
        }

        @Test
        void riassortimentoNonValidoMostraLErrore() throws Exception {
                int disponibili = catalogo.trova("GUA-L").getDisponibili();

                mvc.perform(post("/prodotti/GUA-L/riassortimento").param("quantita", "0"))
                                .andExpect(status().isOk())
                                .andExpect(content()
                                                .string(containsString("La quantità da riassortire va da 1 a 1000")));

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
                mvc.perform(post("/prodotti/MET-5/riassortimento").param("quantita", "10").param("nota", "Arrivo"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/movimenti"));
                mvc.perform(get("/movimenti"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Arrivo")));
        }

        @Test
        void ordineInesistente() throws Exception {
                mvc.perform(get("/ordini/9999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void dettaglioEdElencoMostranoScontiSpedizioneETotaleDaPagare() throws Exception {
                MvcResult creazione = mvc.perform(post("/ordini")
                                .param("cliente", "Ferramenta Bianchi")
                                .param("righe[0].codice", "GUA-L")
                                .param("righe[0].quantita", "10"))
                                .andExpect(status().is3xxRedirection())
                                .andReturn();
                String urlDettaglio = creazione.getResponse().getRedirectedUrl();

                mvc.perform(get(urlDettaglio))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("5%")))
                                .andExpect(content().string(containsString("30,40 €")))
                                .andExpect(content().string(containsString("Totale merce")))
                                .andExpect(content().string(containsString("Spedizione")))
                                .andExpect(content().string(containsString("9,90 €")))
                                .andExpect(content().string(containsString("Totale da pagare")))
                                .andExpect(content().string(containsString("40,30 €")));

                mvc.perform(get("/ordini"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("40,30 €")));
        }
}

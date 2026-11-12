package com.epicode.magazzino.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epicode.magazzino.service.MetricheApplicative;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MetricheActuatorTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private MeterRegistry registro;

  @Autowired
  private MetricheApplicative metriche;

  @Test
  void actuatorEsponeMetricheEHealth() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk())
        .andExpect(content().string(containsString("\"status\":\"UP\"")));

    mvc.perform(get("/actuator/metrics")).andExpect(status().isOk())
        .andExpect(content().string(containsString("jvm.memory.used")));
  }

  @Test
  void prometheusEsponeLeMetricheMicrometer() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
        .andExpect(content().string(containsString("jvm_memory_used_bytes")));
  }

  @Test
  void prometheusEsponeBucketPerHttpCasiUsoEMcp() throws Exception {
    registro.timer("http.server.requests", "method", "GET", "uri", "/test",
        "status", "200").record(10, TimeUnit.MILLISECONDS);
    metriche.misuraCaso("test.caso", () -> 1);
    metriche.misuraMcp("test_tool", () -> 1);

    mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
        .andExpect(content()
            .string(containsString("http_server_requests_seconds_bucket{")))
        .andExpect(content().string(
            containsString("magazzino_casi_uso_durata_seconds_bucket{")))
        .andExpect(content()
            .string(containsString("magazzino_mcp_durata_seconds_bucket{")));
  }
}

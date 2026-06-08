package com.bogdanov.performance.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.support.BaseIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
class V1GoodsDeclarationControllerIntegrationTest extends BaseIntegrationTest {
  @Test
  void processes10ItemsThroughRestController() throws Exception {
    assertProcessingReport(10);
  }

  @Test
  void processes100ItemsThroughRestController() throws Exception {
    assertProcessingReport(100);
  }

  @Test
  void processes500ItemsThroughRestController() throws Exception {
    assertProcessingReport(500);
  }

  @Test
  @Tag("slow")
  void processes1000ItemsThroughRestController() throws Exception {
    assertProcessingReport(1_000);
  }

  @Test
  @Tag("slow")
  void processes5000ItemsThroughRestController() throws Exception {
    assertProcessingReport(5_000);
  }

  private void assertProcessingReport(int size) throws Exception {
    long expectedQueries = 3L * size * size;

    MvcResult result = mockMvc.perform(post("/api/v1/declarations/process")
        .contentType(MediaType.APPLICATION_JSON)
        .content(declarationsJson(size)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.records").value(size))
      .andExpect(jsonPath("$.validRecords").value(size))
      .andExpect(jsonPath("$.databaseQueries").value(expectedQueries))
      .andReturn();

    ProcessingReport report = readReport(result);

    assertThat(report.totalMillis()).isPositive();
    assertThat(report.averageMillisPerRecord()).isPositive();

    log.info(
      "V1 MockMvc integration result: records={}, dbQueries={}, totalMs={}, avgMsPerRecord={}",
      report.records(),
      report.databaseQueries(),
      String.format("%.2f", report.totalMillis()),
      String.format("%.4f", report.averageMillisPerRecord())
    );
  }
}

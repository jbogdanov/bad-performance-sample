package com.bogdanov.performance.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.support.BaseIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
class V2GoodsDeclarationControllerIntegrationTest extends BaseIntegrationTest {
  @Test
  void processes5000ItemsThroughRestControllerWithConstantDatabaseQueries() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v2/declarations/process")
        .contentType(MediaType.APPLICATION_JSON)
        .content(allDeclarationsJson()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.records").value(5_000))
      .andExpect(jsonPath("$.validRecords").value(5_000))
      .andExpect(jsonPath("$.databaseQueries").value(3))
      .andReturn();

    ProcessingReport report = readReport(result);

    assertThat(report.totalMillis()).isPositive();
    assertThat(report.averageMillisPerRecord()).isPositive();

    log.info(
      "V2 MockMvc integration result: records={}, dbQueries={}, totalMs={}, avgMsPerRecord={}",
      report.records(),
      report.databaseQueries(),
      String.format("%.2f", report.totalMillis()),
      String.format("%.4f", report.averageMillisPerRecord())
    );
  }
}

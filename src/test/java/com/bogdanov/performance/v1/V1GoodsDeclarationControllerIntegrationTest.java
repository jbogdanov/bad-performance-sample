package com.bogdanov.performance.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.support.BaseIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
class V1GoodsDeclarationControllerIntegrationTest extends BaseIntegrationTest {
  @ParameterizedTest(name = "processes {0} items through V1 REST controller")
  @ValueSource(ints = {10, 50, 100, 150, 200, 300, 400, 500})
  void processesItemsThroughRestController(int size) throws Exception {
    assertProcessingReport(size);
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

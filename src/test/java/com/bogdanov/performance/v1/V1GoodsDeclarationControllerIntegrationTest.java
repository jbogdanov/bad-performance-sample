package com.bogdanov.performance.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bogdanov.performance.common.io.GoodsDeclarationCsvReader;
import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class V1GoodsDeclarationControllerIntegrationTest {
  private static final Path INPUT_FILE = Path.of("src/main/resources/data/goods-declarations-5000.csv");
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("customs")
    .withUsername("customs")
    .withPassword("customs");

  private static List<GoodsDeclaration> declarations;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
  }

  @BeforeAll
  static void loadDeclarations() throws Exception {
    declarations = GoodsDeclarationCsvReader.read(INPUT_FILE);
  }

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
    String requestBody = objectMapper.writeValueAsString(declarations.subList(0, size));
    long expectedQueries = 3L * size * size;

    MvcResult result = mockMvc.perform(post("/api/v1/declarations/process")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.records").value(size))
      .andExpect(jsonPath("$.validRecords").value(size))
      .andExpect(jsonPath("$.databaseQueries").value(expectedQueries))
      .andReturn();

    ProcessingReport report = objectMapper.readValue(
      result.getResponse().getContentAsString(),
      ProcessingReport.class
    );

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

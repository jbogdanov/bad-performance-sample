package com.bogdanov.performance.v2;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class V2GoodsDeclarationControllerIntegrationTest {
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
  void processes5000ItemsThroughRestControllerWithConstantDatabaseQueries() throws Exception {
    String requestBody = objectMapper.writeValueAsString(declarations);

    MvcResult result = mockMvc.perform(post("/api/v2/declarations/process")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.records").value(5_000))
      .andExpect(jsonPath("$.validRecords").value(5_000))
      .andExpect(jsonPath("$.databaseQueries").value(3))
      .andReturn();

    ProcessingReport report = objectMapper.readValue(
      result.getResponse().getContentAsString(),
      ProcessingReport.class
    );

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

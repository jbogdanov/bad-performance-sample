package com.bogdanov.performance.support;

import com.bogdanov.performance.common.io.GoodsDeclarationCsvReader;
import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {
  private static final Path INPUT_FILE = Path.of("src/main/resources/data/goods-declarations-5000.csv");
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("customs")
    .withUsername("customs")
    .withPassword("customs");

  protected static List<GoodsDeclaration> declarations;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

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

  protected String declarationsJson(int size) throws Exception {
    return objectMapper.writeValueAsString(declarations.subList(0, size));
  }

  protected String allDeclarationsJson() throws Exception {
    return objectMapper.writeValueAsString(declarations);
  }

  protected ProcessingReport readReport(MvcResult result) throws Exception {
    return objectMapper.readValue(
      result.getResponse().getContentAsString(),
      ProcessingReport.class
    );
  }
}

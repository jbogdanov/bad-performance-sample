package com.bogdanov.performance.v1;

import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.database.ReferenceDatabase;

import java.sql.SQLException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InefficientDeclarationProcessor {
  private final ReferenceDatabase database;

  public synchronized ProcessingReport process(List<GoodsDeclaration> declarations) throws SQLException {
    database.resetQueryCount();
    long started = System.nanoTime();
    int validRecords = 0;

    for (GoodsDeclaration declaration : declarations) {
      boolean senderExists = false;
      boolean receiverExists = false;
      boolean categoryAndRiskExists = false;

      // Bad example 1: repeatedly checks the same sender reference for every input record.
      for (GoodsDeclaration ignored : declarations) {
        senderExists = database.senderExists(declaration.senderReference());
      }

      // Bad example 2: repeatedly checks the same receiver reference for every input record.
      for (GoodsDeclaration ignored : declarations) {
        receiverExists = database.receiverExists(declaration.receiverReference());
      }

      // Bad example 3: repeatedly checks the same goods category and risk assessment pair.
      for (GoodsDeclaration ignored : declarations) {
        categoryAndRiskExists = database.goodsCategoryAndRiskExist(
          declaration.goodsCategoryCode(),
          declaration.riskAssessmentCode()
        );
      }

      if (senderExists && receiverExists && categoryAndRiskExists) {
        validRecords++;
      }
    }

    long elapsedNanos = System.nanoTime() - started;
    ProcessingReport report = ProcessingReport.from(
      declarations.size(),
      validRecords,
      database.queryCount(),
      elapsedNanos
    );
    log.info(
      "V1 processed {} declarations with {} database queries in {} ms; average {} ms/record",
      report.records(),
      report.databaseQueries(),
      String.format("%.2f", report.totalMillis()),
      String.format("%.4f", report.averageMillisPerRecord())
    );
    return report;
  }
}

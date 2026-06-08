package com.bogdanov.performance.v2;

import com.bogdanov.performance.common.model.GoodsCategoryRiskKey;
import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.database.ReferenceDatabase;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedDeclarationProcessor {
  private final ReferenceDatabase database;

  public synchronized ProcessingReport process(List<GoodsDeclaration> declarations) throws SQLException {
    database.resetQueryCount();
    long started = System.nanoTime();

    Set<String> senderReferences = database.findAllSenderReferences();
    Set<String> receiverReferences = database.findAllReceiverReferences();
    Set<GoodsCategoryRiskKey> categoryRiskPairs = database.findAllGoodsCategoryRiskPairs();

    int validRecords = 0;
    for (GoodsDeclaration declaration : declarations) {
      boolean valid = senderReferences.contains(declaration.senderReference())
        && receiverReferences.contains(declaration.receiverReference())
        && categoryRiskPairs.contains(new GoodsCategoryRiskKey(
        declaration.goodsCategoryCode(),
        declaration.riskAssessmentCode()
      ));

      if (valid) {
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
      "V2 processed {} declarations with {} database queries in {} ms; average {} ms/record",
      report.records(),
      report.databaseQueries(),
      String.format("%.2f", report.totalMillis()),
      String.format("%.4f", report.averageMillisPerRecord())
    );
    return report;
  }
}

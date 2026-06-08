package com.bogdanov.performance.v2;

import com.bogdanov.performance.common.model.GoodsCategoryRiskKey;
import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.database.ReferenceDatabase;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedDeclarationProcessor {
  private static final int BATCH_SIZE = 1_000;

  private final ReferenceDatabase database;

  public synchronized ProcessingReport process(List<GoodsDeclaration> declarations) throws SQLException {
    database.resetQueryCount();
    long started = System.nanoTime();

    int validRecords = 0;
    for (List<GoodsDeclaration> batch : ListUtils.partition(declarations, BATCH_SIZE)) {
      validRecords += processBatch(batch);
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

  private int processBatch(List<GoodsDeclaration> declarations) throws SQLException {
    Set<String> requestedSenderReferences = declarations.stream()
      .map(GoodsDeclaration::senderReference)
      .collect(Collectors.toSet());

    Set<String> requestedReceiverReferences = declarations.stream()
      .map(GoodsDeclaration::receiverReference)
      .collect(Collectors.toSet());

    Set<GoodsCategoryRiskKey> requestedCategoryRiskPairs = declarations.stream()
      .map(GoodsCategoryRiskKey::from)
      .collect(Collectors.toSet());

    Set<String> senderReferences = database.findSenderReferences(requestedSenderReferences);
    Set<String> receiverReferences = database.findReceiverReferences(requestedReceiverReferences);
    Set<GoodsCategoryRiskKey> categoryRiskPairs = database.findGoodsCategoryRiskPairs(requestedCategoryRiskPairs);

    int validRecords = 0;
    for (GoodsDeclaration declaration : declarations) {
      boolean valid = senderReferences.contains(declaration.senderReference())
        && receiverReferences.contains(declaration.receiverReference())
        && categoryRiskPairs.contains(GoodsCategoryRiskKey.from(declaration));

      if (valid) {
        validRecords++;
      }
    }
    return validRecords;
  }
}

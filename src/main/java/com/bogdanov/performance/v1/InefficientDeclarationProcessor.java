package com.bogdanov.performance.v1;

import com.bogdanov.performance.common.business.SplitConsignmentPolicy;
import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;
import com.bogdanov.performance.common.model.TradeFlowKey;
import com.bogdanov.performance.database.ReferenceDatabase;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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
    int manualReviewRecords = 0;

    for (GoodsDeclaration declaration : declarations) {
      boolean senderExists = database.senderExists(declaration.senderReference());
      boolean receiverExists = database.receiverExists(declaration.receiverReference());
      boolean categoryAndRiskExists = database.goodsCategoryAndRiskExist(declaration.goodsCategoryCode(), declaration.riskAssessmentCode());
      boolean splitConsignmentCandidate = isSplitConsignmentCandidate(declaration, declarations);

      if (senderExists && receiverExists && categoryAndRiskExists) {
        validRecords++;
      }

      if (splitConsignmentCandidate) {
        manualReviewRecords++;
      }
    }

    long elapsedNanos = System.nanoTime() - started;
    ProcessingReport report = ProcessingReport.from(
      declarations.size(),
      validRecords,
      manualReviewRecords,
      database.queryCount(),
      elapsedNanos
    );
    log.info(
      "V1 processed {} declarations with {} database queries and {} manual review records in {} ms; average {} ms/record",
      report.records(),
      report.databaseQueries(),
      report.manualReviewRecords(),
      String.format("%.2f", report.totalMillis()),
      String.format("%.4f", report.averageMillisPerRecord())
    );
    return report;
  }

  private boolean isSplitConsignmentCandidate(GoodsDeclaration declaration, List<GoodsDeclaration> declarations) throws SQLException {
    TradeFlowKey tradeFlow = TradeFlowKey.from(declaration);
    BigDecimal combinedDeclaredValue = BigDecimal.ZERO;
    BigDecimal manualReviewThreshold = null;

    for (GoodsDeclaration relatedDeclaration : declarations) {
      Optional<BigDecimal> ruleThreshold = database.findSplitConsignmentThreshold(
        relatedDeclaration.goodsCategoryCode(),
        relatedDeclaration.originCountry()
      );

      if (tradeFlow.equals(TradeFlowKey.from(relatedDeclaration))) {
        manualReviewThreshold = ruleThreshold.orElse(null);
        combinedDeclaredValue = combinedDeclaredValue.add(relatedDeclaration.declaredValue());
      }
    }

    return manualReviewThreshold != null
      && SplitConsignmentPolicy.requiresManualReview(combinedDeclaredValue, manualReviewThreshold);
  }
}

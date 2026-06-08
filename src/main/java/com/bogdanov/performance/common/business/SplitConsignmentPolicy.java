package com.bogdanov.performance.common.business;

import java.math.BigDecimal;

public final class SplitConsignmentPolicy {
  private SplitConsignmentPolicy() {
  }

  public static boolean requiresManualReview(BigDecimal combinedDeclaredValue, BigDecimal manualReviewThreshold) {
    return combinedDeclaredValue.compareTo(manualReviewThreshold) > 0;
  }
}

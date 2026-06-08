package com.bogdanov.performance.common.model;

import java.math.BigDecimal;

public record GoodsDeclaration(
  String declarationId,
  String senderReference,
  String receiverReference,
  String goodsCategoryCode,
  String riskAssessmentCode,
  String originCountry,
  BigDecimal declaredValue,
  BigDecimal weightKg
) {
}

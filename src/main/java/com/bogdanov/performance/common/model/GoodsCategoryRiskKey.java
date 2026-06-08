package com.bogdanov.performance.common.model;

public record GoodsCategoryRiskKey(
  String goodsCategoryCode,
  String riskAssessmentCode
) {
  public static GoodsCategoryRiskKey of(String goodsCategoryCode, String riskAssessmentCode) {
    return new GoodsCategoryRiskKey(goodsCategoryCode, riskAssessmentCode);
  }

}

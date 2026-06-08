package com.bogdanov.performance.common.model;

public record SplitConsignmentRuleKey(
  String goodsCategoryCode,
  String originCountry
) {
  public static SplitConsignmentRuleKey from(GoodsDeclaration declaration) {
    return new SplitConsignmentRuleKey(
      declaration.goodsCategoryCode(),
      declaration.originCountry()
    );
  }
}

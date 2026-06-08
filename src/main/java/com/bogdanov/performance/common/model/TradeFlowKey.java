package com.bogdanov.performance.common.model;

public record TradeFlowKey(
  String senderReference,
  String receiverReference,
  String goodsCategoryCode,
  String originCountry
) {
  public static TradeFlowKey from(GoodsDeclaration declaration) {
    return new TradeFlowKey(
      declaration.senderReference(),
      declaration.receiverReference(),
      declaration.goodsCategoryCode(),
      declaration.originCountry()
    );
  }
}

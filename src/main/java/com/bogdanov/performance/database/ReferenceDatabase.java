package com.bogdanov.performance.database;

import com.bogdanov.performance.common.model.GoodsCategoryRiskKey;
import com.bogdanov.performance.common.model.SplitConsignmentRuleKey;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDatabase implements AutoCloseable {
  private final DataSource dataSource;
  private Connection connection;
  private long queryCount;

  @PostConstruct
  void initialize() throws SQLException {
    connection = dataSource.getConnection();
    seedReferenceData();
    log.info("Seeded PostgreSQL customs reference database");
  }

  public void seedReferenceData() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("drop table if exists category_risk_compatibility");
      statement.execute("drop table if exists split_consignment_rule");
      statement.execute("drop table if exists risk_assessment");
      statement.execute("drop table if exists goods_category");
      statement.execute("drop table if exists receiver");
      statement.execute("drop table if exists sender");

      statement.execute("""
        create table sender (
            reference varchar(20) primary key,
            name varchar(100) not null
        )
        """);
      statement.execute("""
        create table receiver (
            reference varchar(20) primary key,
            name varchar(100) not null
        )
        """);
      statement.execute("""
        create table goods_category (
            code varchar(20) primary key,
            description varchar(100) not null
        )
        """);
      statement.execute("""
        create table risk_assessment (
            code varchar(20) primary key,
            score int not null,
            description varchar(100) not null
        )
        """);
      statement.execute("""
        create table category_risk_compatibility (
            goods_category_code varchar(20) not null,
            risk_assessment_code varchar(20) not null,
            primary key (goods_category_code, risk_assessment_code)
        )
        """);
      statement.execute("""
        create table split_consignment_rule (
            goods_category_code varchar(20) not null,
            origin_country varchar(2) not null,
            manual_review_threshold numeric(12, 2) not null,
            primary key (goods_category_code, origin_country)
        )
        """);
    }

    insertSenders();
    insertReceivers();
    insertGoodsCategories();
    insertRiskAssessments();
    insertCategoryRiskCompatibility();
    insertSplitConsignmentRules();
  }

  public boolean senderExists(String reference) throws SQLException {
    queryCount++;
    try (PreparedStatement statement = connection.prepareStatement(
      "select 1 from sender where reference = ?")) {
      statement.setString(1, reference);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  public boolean receiverExists(String reference) throws SQLException {
    queryCount++;
    try (PreparedStatement statement = connection.prepareStatement(
      "select 1 from receiver where reference = ?")) {
      statement.setString(1, reference);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  public boolean goodsCategoryAndRiskExist(String goodsCategoryCode, String riskAssessmentCode) throws SQLException {
    queryCount++;
    try (PreparedStatement statement = connection.prepareStatement("""
      select 1
      from goods_category gc
      join category_risk_compatibility crc
          on crc.goods_category_code = gc.code
      join risk_assessment ra
          on ra.code = crc.risk_assessment_code
      where gc.code = ?
          and ra.code = ?
      """)) {
      statement.setString(1, goodsCategoryCode);
      statement.setString(2, riskAssessmentCode);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  public Set<String> findSenderReferences(Collection<String> references) throws SQLException {
    if (references.isEmpty()) {
      return Set.of();
    }
    queryCount++;
    Set<String> result = new HashSet<>();
    try (PreparedStatement statement = connection.prepareStatement(
      "select reference from sender where reference in (" + placeholders(references.size()) + ")")) {
      int index = 1;
      for (String reference : references) {
        statement.setString(index++, reference);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          result.add(resultSet.getString("reference"));
        }
      }
    }
    return result;
  }

  public Set<String> findReceiverReferences(Collection<String> references) throws SQLException {
    if (references.isEmpty()) {
      return Set.of();
    }
    queryCount++;
    Set<String> result = new HashSet<>();
    try (PreparedStatement statement = connection.prepareStatement(
      "select reference from receiver where reference in (" + placeholders(references.size()) + ")")) {
      int index = 1;
      for (String reference : references) {
        statement.setString(index++, reference);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          result.add(resultSet.getString("reference"));
        }
      }
    }
    return result;
  }

  public Set<GoodsCategoryRiskKey> findGoodsCategoryRiskPairs(Collection<GoodsCategoryRiskKey> pairs) throws SQLException {
    if (pairs.isEmpty()) {
      return Set.of();
    }
    queryCount++;
    Set<GoodsCategoryRiskKey> result = new HashSet<>();
    try (PreparedStatement statement = connection.prepareStatement("""
      select goods_category_code, risk_assessment_code
      from category_risk_compatibility
      where (goods_category_code, risk_assessment_code) in (
      """ + tuplePlaceholders(pairs.size()) + ")")) {
      int index = 1;
      for (GoodsCategoryRiskKey pair : pairs) {
        statement.setString(index++, pair.goodsCategoryCode());
        statement.setString(index++, pair.riskAssessmentCode());
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          result.add(GoodsCategoryRiskKey.of(
            resultSet.getString("goods_category_code"),
            resultSet.getString("risk_assessment_code")
          ));
        }
      }
    }
    return result;
  }

  public Optional<BigDecimal> findSplitConsignmentThreshold(String goodsCategoryCode, String originCountry) throws SQLException {
    queryCount++;
    try (PreparedStatement statement = connection.prepareStatement("""
      select manual_review_threshold
      from split_consignment_rule
      where goods_category_code = ?
          and origin_country = ?
      """)) {
      statement.setString(1, goodsCategoryCode);
      statement.setString(2, originCountry);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(resultSet.getBigDecimal("manual_review_threshold"));
        }
      }
    }
    return Optional.empty();
  }

  public Map<SplitConsignmentRuleKey, BigDecimal> findSplitConsignmentThresholds(Collection<SplitConsignmentRuleKey> keys) throws SQLException {
    if (keys.isEmpty()) {
      return Map.of();
    }
    queryCount++;
    Map<SplitConsignmentRuleKey, BigDecimal> result = new HashMap<>();
    try (PreparedStatement statement = connection.prepareStatement("""
      select goods_category_code, origin_country, manual_review_threshold
      from split_consignment_rule
      where (goods_category_code, origin_country) in (
      """ + tuplePlaceholders(keys.size()) + ")")) {
      int index = 1;
      for (SplitConsignmentRuleKey key : keys) {
        statement.setString(index++, key.goodsCategoryCode());
        statement.setString(index++, key.originCountry());
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          result.put(
            new SplitConsignmentRuleKey(
              resultSet.getString("goods_category_code"),
              resultSet.getString("origin_country")
            ),
            resultSet.getBigDecimal("manual_review_threshold")
          );
        }
      }
    }
    return result;
  }

  public long queryCount() {
    return queryCount;
  }

  public void resetQueryCount() {
    queryCount = 0;
  }

  @PreDestroy
  @Override
  public void close() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  private void insertSenders() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
      "insert into sender(reference, name) values (?, ?)")) {
      for (int i = 1; i <= 250; i++) {
        statement.setString(1, "SND-" + padded(i, 4));
        statement.setString(2, "Sender Company " + i);
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void insertReceivers() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
      "insert into receiver(reference, name) values (?, ?)")) {
      for (int i = 1; i <= 250; i++) {
        statement.setString(1, "RCV-" + padded(i, 4));
        statement.setString(2, "Receiver Company " + i);
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void insertGoodsCategories() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
      "insert into goods_category(code, description) values (?, ?)")) {
      for (int i = 1; i <= 80; i++) {
        statement.setString(1, "CAT-" + padded(i, 3));
        statement.setString(2, "Goods category " + i);
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void insertRiskAssessments() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
      "insert into risk_assessment(code, score, description) values (?, ?, ?)")) {
      for (int i = 1; i <= 20; i++) {
        statement.setString(1, "RISK-" + padded(i, 2));
        statement.setInt(2, i);
        statement.setString(3, "Risk assessment level " + i);
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void insertCategoryRiskCompatibility() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
      insert into category_risk_compatibility(goods_category_code, risk_assessment_code)
      values (?, ?)
      """)) {
      for (int category = 1; category <= 80; category++) {
        for (int risk = 1; risk <= 20; risk++) {
          statement.setString(1, "CAT-" + padded(category, 3));
          statement.setString(2, "RISK-" + padded(risk, 2));
          statement.addBatch();
        }
      }
      statement.executeBatch();
    }
  }

  private void insertSplitConsignmentRules() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
      insert into split_consignment_rule(goods_category_code, origin_country, manual_review_threshold)
      values (?, ?, ?)
      """)) {
      String[] originCountries = {"EE", "LV", "LT", "FI", "SE", "DE", "PL", "NL", "CN", "US"};
      for (int category = 1; category <= 80; category++) {
        for (String originCountry : originCountries) {
          statement.setString(1, "CAT-" + padded(category, 3));
          statement.setString(2, originCountry);
          statement.setBigDecimal(3, new BigDecimal("10000.00"));
          statement.addBatch();
        }
      }
      statement.executeBatch();
    }
  }

  private static String padded(int value, int width) {
    return String.format("%0" + width + "d", value);
  }

  private static String placeholders(int count) {
    return String.join(", ", Collections.nCopies(count, "?"));
  }

  private static String tuplePlaceholders(int count) {
    return Collections.nCopies(count, "(?, ?)")
      .stream()
      .collect(Collectors.joining(", "));
  }
}

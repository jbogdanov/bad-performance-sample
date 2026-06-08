package com.bogdanov.performance.common.model;

public record ProcessingReport(
  int records,
  int validRecords,
  long databaseQueries,
  double totalMillis,
  double averageMillisPerRecord
) {
  public static ProcessingReport from(int records, int validRecords, long databaseQueries, long elapsedNanos) {
    double totalMillis = elapsedNanos / 1_000_000.0;
    return new ProcessingReport(
      records,
      validRecords,
      databaseQueries,
      totalMillis,
      totalMillis / records
    );
  }
}

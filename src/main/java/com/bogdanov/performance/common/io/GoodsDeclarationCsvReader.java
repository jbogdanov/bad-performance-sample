package com.bogdanov.performance.common.io;

import com.bogdanov.performance.common.model.GoodsDeclaration;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GoodsDeclarationCsvReader {
  private GoodsDeclarationCsvReader() {
  }

  public static List<GoodsDeclaration> read(Path inputFile) throws IOException {
    List<String> lines = Files.readAllLines(inputFile);
    List<GoodsDeclaration> declarations = new ArrayList<>(lines.size() - 1);

    for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
      String line = lines.get(lineNumber);
      if (line.isBlank()) {
        continue;
      }

      String[] columns = line.split(",", -1);
      declarations.add(new GoodsDeclaration(
        columns[0],
        columns[1],
        columns[2],
        columns[3],
        columns[4],
        columns[5],
        new BigDecimal(columns[6]),
        new BigDecimal(columns[7])
      ));
    }

    return declarations;
  }
}

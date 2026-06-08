package com.bogdanov.performance.v2;

import com.bogdanov.performance.common.model.GoodsDeclaration;
import com.bogdanov.performance.common.model.ProcessingReport;

import java.sql.SQLException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/declarations")
public class V2GoodsDeclarationController {
  private final OptimizedDeclarationProcessor processor;

  @PostMapping("/process")
  public ProcessingReport process(@RequestBody List<GoodsDeclaration> declarations) throws SQLException {
    log.info("V2 received {} declarations for optimized processing", declarations.size());
    return processor.process(declarations);
  }
}

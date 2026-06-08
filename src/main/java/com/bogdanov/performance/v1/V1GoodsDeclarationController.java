package com.bogdanov.performance.v1;

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
@RequestMapping({"/api/v1/declarations", "/api/declarations"})
public class V1GoodsDeclarationController {
  private final InefficientDeclarationProcessor processor;

  @PostMapping("/process")
  public ProcessingReport process(@RequestBody List<GoodsDeclaration> declarations) throws SQLException {
    log.info("V1 received {} declarations for inefficient processing", declarations.size());
    return processor.process(declarations);
  }
}

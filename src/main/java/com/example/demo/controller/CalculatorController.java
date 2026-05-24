package com.example.demo.controller;

import com.example.demo.service.CalculatorService;
import com.example.demo.service.ScreenCaptureService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calculator")
@AllArgsConstructor
public class CalculatorController {

  private final CalculatorService calculatorService;
  private final ScreenCaptureService service;

  @PostMapping("/calculator-win-or-lose")
  private void calculatorWinOrLose(@RequestParam("ytd") List<Integer> ytd) {
    calculatorService.calculatorWinOrLose(ytd);
  }

  // Tinh 113 226
  @PostMapping("/calculator-real-v5")
  private Map<Integer, Double> calculatorRealV5(@RequestParam("ytd") List<Integer> ytd) {
    return calculatorService.calculatorRealV5(ytd);
  }

  @PostMapping("/get-data")
  private void getData() {
    calculatorService.getData();
  }

  @PostMapping("/capture-v1")
  public int captureV1(
      @RequestParam("x") int x, @RequestParam("y") int y, @RequestParam("size") int size)
      throws Exception {
    return service.captureCircleCenter(x, y, size);
  }
}

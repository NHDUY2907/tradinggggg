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

  // Gọi để tính toán win hay lose
  @PostMapping("/win-or-lose")
  private void calculatorWinOrLose(@RequestParam("ytd") List<Integer> ytd) {
    calculatorService.calculatorWinOrLose(ytd);
  }

  // Tinh 113 226
  @PostMapping("/main")
  private Map<Integer, Double> calculatorMain(@RequestParam("ytd") List<Integer> ytd) {
    return calculatorService.calculatorMain(ytd);
  }

  // Lấy data kiểm tra hiện tại có đúng k
  @PostMapping("/get-data")
  private void getData() {
    calculatorService.getData();
  }
}

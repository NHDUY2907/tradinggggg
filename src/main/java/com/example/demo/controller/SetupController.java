package com.example.demo.controller;

import com.example.demo.service.CalculatorService;
import com.example.demo.service.ScreenCaptureService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/set-up")
@AllArgsConstructor
public class SetupController {

    private final CalculatorService calculatorService;
    private final ScreenCaptureService service;

    @PostMapping("/test-image")
    public void testImage(
            @RequestParam("x") int x, @RequestParam("y") int y, @RequestParam("size") int size)
            throws Exception {
        service.testImage(x, y, size);
    }
}

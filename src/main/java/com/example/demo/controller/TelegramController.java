package com.example.demo.controller;

import com.example.demo.service.TelegramService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
@AllArgsConstructor
public class TelegramController {

  private final TelegramService telegramService;

  @PostMapping("/send")
  public String send(@RequestParam String msg) {
    telegramService.sendMessage(msg);
    return "Sent!";
  }
}

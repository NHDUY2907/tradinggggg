package com.example.demo.controller;

import com.example.demo.service.JobService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")
@AllArgsConstructor
public class JobController {
  private final JobService jobService;

  @PostMapping("/start")
  public String start(
      @RequestParam("x") int x, @RequestParam("y") int y, @RequestParam("size") int size) {
    jobService.startJob(x, y, size);
    return "Started";
  }

  @PostMapping("/stop")
  public String stop() {
    jobService.stopJob();
    return "Stopped";
  }
}

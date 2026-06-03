package com.example.demo.event;

import com.example.demo.data.entity.StatisticalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeSession {
  private int totalMoney = 15;
  private StatisticalEntity openTrade;
  private int vol;
  private int num;
}

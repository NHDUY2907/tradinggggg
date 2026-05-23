package com.example.demo.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "statistical", schema = "duynh5")
public class StatisticalEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "STATISTICAL_ID")
  private Integer statisticalId;

  @Column(name = "YTD")
  private Integer date;

  @Column(name = "RESULT")
  private Integer result;

  @Column(name = "WIN")
  private Integer win;

  @Column(name = "LOSE")
  private Integer lose;

  @Column(name = "WIN_LAST")
  private Integer winLast;

  @Column(name = "LOSE_LAST")
  private Integer loseLast;

  @Column(name = "SO_LAN_CUOC")
  private Integer soLanCuoc;

  @Column(name = "EQ_RESULT")
  private Integer eqResult;

  @Column(name = "LECH_TREN")
  private Integer lechTren;

  @Column(name = "LECH_DUOI")
  private Integer lechDuoi;

  @Column(name = "LENGTH")
  private String length;
}

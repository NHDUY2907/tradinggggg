package com.example.demo.data.repository;

import com.example.demo.data.entity.StatisticalEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatisticalRepository
    extends JpaRepository<StatisticalEntity, Integer>, JpaSpecificationExecutor<StatisticalEntity> {
  List<StatisticalEntity> findAllByDate(Integer ytd);

  List<StatisticalEntity> findTop10OrderByStatisticalIdDesc();

  List<StatisticalEntity> findTop50ByDateOrderByStatisticalIdDesc(Integer ytd);

  List<StatisticalEntity> findTop100ByDateOrderByStatisticalIdDesc(Integer ytd);
}

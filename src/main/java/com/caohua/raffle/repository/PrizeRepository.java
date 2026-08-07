package com.caohua.raffle.repository;

import com.caohua.raffle.model.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Long> {
    List<Prize> findAllByOrderByDisplayOrderAsc();
}

package com.caohua.raffle.repository;

import com.caohua.raffle.model.LotteryEventPrize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotteryEventPrizeRepository extends JpaRepository<LotteryEventPrize, Long> {
    List<LotteryEventPrize> findByEventId(Long eventId);
    List<LotteryEventPrize> findByEventIdAndRemainingGreaterThanOrderByPrizeDisplayOrderAsc(Long eventId, Integer remaining);
    void deleteByEventId(Long eventId);
}

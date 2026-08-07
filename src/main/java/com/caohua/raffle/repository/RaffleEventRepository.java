package com.caohua.raffle.repository;

import com.caohua.raffle.model.RaffleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaffleEventRepository extends JpaRepository<RaffleEvent, Long> {
    List<RaffleEvent> findAllByOrderByCreatedAtDesc();
    Optional<RaffleEvent> findFirstByActiveTrueOrderByCreatedAtDesc();
    List<RaffleEvent> findByActiveTrueOrderByStartTimeAsc();
}

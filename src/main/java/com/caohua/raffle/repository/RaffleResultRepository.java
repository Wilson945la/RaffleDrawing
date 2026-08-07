package com.caohua.raffle.repository;

import com.caohua.raffle.model.RaffleResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface RaffleResultRepository extends JpaRepository<RaffleResult, Long> {
    List<RaffleResult> findAllByOrderByRaffleTimeDesc();
    @Query("SELECT r FROM RaffleResult r JOIN r.user u JOIN r.prize p " +
           "WHERE LOWER(u.accountId) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.realName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY r.raffleTime DESC")
    List<RaffleResult> searchByKeyword(@Param("keyword") String keyword);
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    List<RaffleResult> findByEventIdOrderByRaffleTimeDesc(Long eventId);
}

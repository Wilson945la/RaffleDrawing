package com.caohua.raffle.repository;

import com.caohua.raffle.model.UserEventDrawCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEventDrawCountRepository extends JpaRepository<UserEventDrawCount, Long> {

    Optional<UserEventDrawCount> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserEventDrawCount> findByUserId(Long userId);
}

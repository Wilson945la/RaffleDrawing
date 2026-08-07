package com.caohua.raffle.service;

import com.caohua.raffle.model.User;
import com.caohua.raffle.model.UserEventDrawCount;
import com.caohua.raffle.model.RaffleEvent;
import com.caohua.raffle.repository.UserRepository;
import com.caohua.raffle.repository.UserEventDrawCountRepository;
import com.caohua.raffle.repository.RaffleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserEventDrawCountRepository drawCountRepository;
    private final RaffleEventRepository eventRepository;

    public UserService(UserRepository userRepository,
                       UserEventDrawCountRepository drawCountRepository,
                       RaffleEventRepository eventRepository) {
        this.userRepository = userRepository;
        this.drawCountRepository = drawCountRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Generate a unique account ID in format "chfz-XXXXXXXX".
     */
    public String generateAccountId() {
        String id;
        do {
            int num = ThreadLocalRandom.current().nextInt(100_000_000);
            id = String.format("chfz-%08d", num);
        } while (userRepository.existsByAccountId(id));
        return id;
    }

    /**
     * Login by accountId. Auto-creates user if the accountId doesn't exist.
     * If realName is provided and user already exists, update the name.
     * When a new user is created, initialize draw count (0) for all existing events.
     */
    @Transactional
    public User login(String accountId, String realName) {
        Optional<User> existing = userRepository.findByAccountId(accountId);
        if (existing.isPresent()) {
            User user = existing.get();
            // Update name if provided
            if (realName != null && !realName.isBlank()
                    && !realName.equals(user.getRealName())) {
                user.setRealName(realName.trim());
                userRepository.save(user);
            }
            return user;
        }
        // New user — auto-create
        User user = new User();
        user.setAccountId(accountId);
        user.setRealName(realName != null && !realName.isBlank() ? realName.trim() : null);
        user.setAdmin(false);
        user = userRepository.save(user);

        // Initialize draw count (0) for all existing events
        List<RaffleEvent> allEvents = eventRepository.findAll();
        for (RaffleEvent e : allEvents) {
            UserEventDrawCount uedc = new UserEventDrawCount(user, e, 0);
            drawCountRepository.save(uedc);
        }

        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByAccountId(String accountId) {
        return userRepository.findByAccountId(accountId);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return userRepository.findByAccountIdContainingOrRealNameContainingIgnoreCase(keyword.trim(), keyword.trim());
    }

    @Transactional
    public Map<String, Object> addDrawCount(Long userId, Long eventId) {
        RaffleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Optional<UserEventDrawCount> existing = drawCountRepository.findByUserIdAndEventId(userId, eventId);
        UserEventDrawCount uedc;
        if (existing.isPresent()) {
            uedc = existing.get();
            uedc.setDrawCount(uedc.getDrawCount() + 1);
        } else {
            uedc = new UserEventDrawCount(user, event, 1);
        }
        uedc = drawCountRepository.save(uedc);

        // Build result map inside transaction to avoid lazy-loading issues
        Map<String, Object> result = new HashMap<>();
        result.put("userId", uedc.getUser().getId());
        result.put("eventId", uedc.getEvent().getId());
        result.put("eventTitle", uedc.getEvent().getTitle());
        result.put("drawCount", uedc.getDrawCount());
        return result;
    }

    public int getDrawCount(Long userId, Long eventId) {
        return drawCountRepository.findByUserIdAndEventId(userId, eventId)
                .map(UserEventDrawCount::getDrawCount)
                .orElse(0);
    }

    public List<UserEventDrawCount> getUserDrawCounts(Long userId) {
        return drawCountRepository.findByUserId(userId);
    }
}

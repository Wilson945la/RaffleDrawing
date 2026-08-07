package com.caohua.raffle.service;

import com.caohua.raffle.model.*;
import com.caohua.raffle.repository.LotteryEventPrizeRepository;
import com.caohua.raffle.repository.PrizeRepository;
import com.caohua.raffle.repository.RaffleEventRepository;
import com.caohua.raffle.repository.RaffleResultRepository;
import com.caohua.raffle.repository.UserRepository;
import com.caohua.raffle.repository.UserEventDrawCountRepository;
import com.caohua.raffle.websocket.RaffleWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RaffleService {

    private final RaffleEventRepository eventRepository;
    private final RaffleResultRepository resultRepository;
    private final LotteryEventPrizeRepository eventPrizeRepository;
    private final PrizeRepository prizeRepository;
    private final UserRepository userRepository;
    private final UserEventDrawCountRepository drawCountRepository;
    private final RaffleWebSocketHandler webSocketHandler;
    private final Random random = new Random();

    public RaffleService(RaffleEventRepository eventRepository,
                         RaffleResultRepository resultRepository,
                         LotteryEventPrizeRepository eventPrizeRepository,
                         PrizeRepository prizeRepository,
                         UserRepository userRepository,
                         UserEventDrawCountRepository drawCountRepository,
                         RaffleWebSocketHandler webSocketHandler) {
        this.eventRepository = eventRepository;
        this.resultRepository = resultRepository;
        this.eventPrizeRepository = eventPrizeRepository;
        this.prizeRepository = prizeRepository;
        this.userRepository = userRepository;
        this.drawCountRepository = drawCountRepository;
        this.webSocketHandler = webSocketHandler;
    }

    // ========== Event Management ==========

    public List<RaffleEvent> getAllEvents() {
        return eventRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<RaffleEvent> getActiveEvent() {
        return eventRepository.findFirstByActiveTrueOrderByCreatedAtDesc();
    }

    public RaffleEvent getCurrentEvent() {
        // Return the most recent active event if it's currently running
        Optional<RaffleEvent> active = eventRepository.findFirstByActiveTrueOrderByCreatedAtDesc();
        if (active.isPresent()) {
            RaffleEvent event = active.get();
            if (event.isRunning()) {
                return event;
            }
        }
        return null;
    }

    public boolean isRaffleRunning() {
        return getCurrentEvent() != null;
    }

    public List<RaffleEvent> getRunningEvents() {
        return eventRepository.findByActiveTrueOrderByStartTimeAsc().stream()
                .filter(RaffleEvent::isRunning)
                .collect(Collectors.toList());
    }

    @Transactional
    public RaffleEvent createEvent(String title, LocalDateTime startTime, LocalDateTime endTime,
                                    List<Map<String, Object>> prizeConfigs) {
        RaffleEvent event = new RaffleEvent();
        event.setTitle(title);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setActive(true);
        RaffleEvent saved = eventRepository.save(event);

        // Create prize entries for this event
        for (Map<String, Object> config : prizeConfigs) {
            Long prizeId = config.get("prizeId") instanceof Number
                    ? ((Number) config.get("prizeId")).longValue() : null;
            Integer quantity = config.get("quantity") instanceof Number
                    ? ((Number) config.get("quantity")).intValue() : 1;
            Double probability = config.get("probability") instanceof Number
                    ? ((Number) config.get("probability")).doubleValue() : 0.0;

            if (prizeId == null) continue;

            Prize prize = prizeRepository.findById(prizeId).orElse(null);
            if (prize == null) continue;

            LotteryEventPrize ep = new LotteryEventPrize();
            ep.setEvent(saved);
            ep.setPrize(prize);
            ep.setQuantity(quantity);
            ep.setRemaining(quantity);
            ep.setProbability(probability);
            eventPrizeRepository.save(ep);
        }

        // Initialize draw count (0) for all existing users for this event
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            UserEventDrawCount uedc = new UserEventDrawCount(u, saved, 0);
            drawCountRepository.save(uedc);
        }

        return saved;
    }

    @Transactional
    public RaffleEvent updateEvent(Long eventId, String title, LocalDateTime startTime, LocalDateTime endTime,
                                    Boolean active, List<Map<String, Object>> prizeConfigs) {
        RaffleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        if (title != null) event.setTitle(title);
        if (startTime != null) event.setStartTime(startTime);
        if (endTime != null) event.setEndTime(endTime);
        if (active != null) event.setActive(active);

        // If prize configs provided, replace all
        if (prizeConfigs != null) {
            eventPrizeRepository.deleteByEventId(eventId);
            for (Map<String, Object> config : prizeConfigs) {
                Long prizeId = config.get("prizeId") instanceof Number
                        ? ((Number) config.get("prizeId")).longValue() : null;
                Integer quantity = config.get("quantity") instanceof Number
                        ? ((Number) config.get("quantity")).intValue() : 1;
                Double probability = config.get("probability") instanceof Number
                        ? ((Number) config.get("probability")).doubleValue() : 0.0;

                if (prizeId == null) continue;

                Prize prize = prizeRepository.findById(prizeId).orElse(null);
                if (prize == null) continue;

                LotteryEventPrize ep = new LotteryEventPrize();
                ep.setEvent(event);
                ep.setPrize(prize);
                ep.setQuantity(quantity);
                ep.setRemaining(quantity);
                ep.setProbability(probability);
                eventPrizeRepository.save(ep);
            }
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        eventPrizeRepository.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
    }

    @Transactional
    public RaffleEvent activateEvent(Long eventId) {
        RaffleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        event.setActive(true);
        return eventRepository.save(event);
    }

    @Transactional
    public RaffleEvent deactivateEvent(Long eventId) {
        RaffleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        event.setActive(false);
        return eventRepository.save(event);
    }

    public List<LotteryEventPrize> getEventPrizes(Long eventId) {
        return eventPrizeRepository.findByEventId(eventId);
    }

    // ========== Raffle Drawing ==========

    @Transactional
    public RaffleResult doRaffle(User user, Long eventId) {
        RaffleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        if (!event.isRunning()) {
            throw new RuntimeException("该活动不在进行中");
        }

        // Check draw count for this event
        UserEventDrawCount uedc = drawCountRepository
                .findByUserIdAndEventId(user.getId(), event.getId())
                .orElse(null);
        if (uedc == null || uedc.getDrawCount() <= 0) {
            throw new RuntimeException("您没有抽奖次数了，请联系管理员添加");
        }

        // Check if user already won in this event
        if (resultRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new RuntimeException("您在这个活动中已经抽过奖了");
        }

        // Get available prizes for this event (remaining > 0)
        List<LotteryEventPrize> availablePrizes = eventPrizeRepository
                .findByEventIdAndRemainingGreaterThanOrderByPrizeDisplayOrderAsc(event.getId(), 0);

        if (availablePrizes.isEmpty()) {
            throw new RuntimeException("奖品已被抽完");
        }

        // Weighted random selection based on probability
        double totalWeight = availablePrizes.stream()
                .mapToDouble(p -> p.getProbability() != null ? p.getProbability() : 0.0)
                .sum();

        LotteryEventPrize selected;
        if (totalWeight <= 0) {
            // All probabilities are 0 — fallback to equal chance
            int roll = random.nextInt(availablePrizes.size());
            selected = availablePrizes.get(roll);
        } else {
            double roll = random.nextDouble() * totalWeight;
            double cumulative = 0;
            selected = availablePrizes.get(availablePrizes.size() - 1); // default fallback
            for (LotteryEventPrize ep : availablePrizes) {
                double p = ep.getProbability() != null ? ep.getProbability() : 0.0;
                cumulative += p;
                if (roll < cumulative) {
                    selected = ep;
                    break;
                }
            }
        }

        // Decrease remaining
        selected.setRemaining(selected.getRemaining() - 1);
        eventPrizeRepository.save(selected);

        // Decrement user's draw count for this event
        uedc.setDrawCount(uedc.getDrawCount() - 1);
        drawCountRepository.save(uedc);

        // Save result
        RaffleResult result = new RaffleResult();
        result.setUser(user);
        result.setPrize(selected.getPrize());
        result.setEvent(event);
        RaffleResult saved = resultRepository.save(result);

        // Notify all connected clients
        webSocketHandler.broadcastNewWinner(user.getRealName(), selected.getPrize().getName());

        return saved;
    }

    // ========== Result Management ==========

    public List<RaffleResult> getAllResults() {
        return resultRepository.findAllByOrderByRaffleTimeDesc();
    }

    public List<RaffleResult> searchResults(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllResults();
        }
        return resultRepository.searchByKeyword(keyword.trim());
    }

    @Transactional
    public RaffleResult updateResult(Long resultId, Long userId, Long prizeId, LocalDateTime raffleTime) {
        RaffleResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("中奖记录不存在"));

        if (Boolean.TRUE.equals(result.getProcessed())) {
            throw new RuntimeException("该记录已处理，无法修改");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // Check if new user already has a different result in this event
        if (!result.getUser().getId().equals(userId)
                && result.getEvent() != null
                && resultRepository.existsByUserIdAndEventId(userId, result.getEvent().getId())) {
            throw new RuntimeException("该用户已有中奖记录");
        }

        Prize newPrize = prizeRepository.findById(prizeId)
                .orElseThrow(() -> new RuntimeException("奖品不存在"));

        // Handle prize stock via event_prizes: if prize changed, revert old and deduct new
        Long oldPrizeId = result.getPrize().getId();
        if (!oldPrizeId.equals(prizeId) && result.getEvent() != null) {
            List<LotteryEventPrize> eps = eventPrizeRepository.findByEventId(result.getEvent().getId());
            LotteryEventPrize oldEp = eps.stream()
                    .filter(ep -> ep.getPrize().getId().equals(oldPrizeId))
                    .findFirst().orElse(null);
            LotteryEventPrize newEp = eps.stream()
                    .filter(ep -> ep.getPrize().getId().equals(prizeId))
                    .findFirst().orElse(null);

            if (newEp == null || newEp.getRemaining() <= 0) {
                throw new RuntimeException("该奖品库存不足");
            }

            if (oldEp != null) {
                oldEp.setRemaining(oldEp.getRemaining() + 1);
                eventPrizeRepository.save(oldEp);
            }
            newEp.setRemaining(newEp.getRemaining() - 1);
            eventPrizeRepository.save(newEp);
        }

        result.setUser(targetUser);
        result.setPrize(newPrize);
        if (raffleTime != null) {
            result.setRaffleTime(raffleTime);
        }

        RaffleResult saved = resultRepository.save(result);

        // Notify clients
        webSocketHandler.broadcastResultChanged("update", saved.getId());

        return saved;
    }

    @Transactional
    public void deleteResult(Long resultId) {
        RaffleResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("中奖记录不存在"));

        if (Boolean.TRUE.equals(result.getProcessed())) {
            throw new RuntimeException("该记录已处理，无法删除");
        }

        // Return prize stock via event_prizes
        if (result.getEvent() != null) {
            List<LotteryEventPrize> eps = eventPrizeRepository.findByEventId(result.getEvent().getId());
            LotteryEventPrize ep = eps.stream()
                    .filter(e -> e.getPrize().getId().equals(result.getPrize().getId()))
                    .findFirst().orElse(null);
            if (ep != null) {
                ep.setRemaining(ep.getRemaining() + 1);
                eventPrizeRepository.save(ep);
            }
        }

        resultRepository.delete(result);

        // Notify clients
        webSocketHandler.broadcastResultChanged("delete", resultId);
    }

    public boolean hasUserWon(Long userId, Long eventId) {
        return resultRepository.existsByUserIdAndEventId(userId, eventId);
    }

    @Transactional
    public RaffleResult processResult(Long resultId) {
        RaffleResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("中奖记录不存在"));

        if (Boolean.TRUE.equals(result.getProcessed())) {
            throw new RuntimeException("该记录已经是已处理状态");
        }

        result.setProcessed(true);
        RaffleResult saved = resultRepository.save(result);

        // Notify clients
        webSocketHandler.broadcastResultChanged("process", saved.getId());

        return saved;
    }
}

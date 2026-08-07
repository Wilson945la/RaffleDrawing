package com.caohua.raffle.controller;

import com.caohua.raffle.model.LotteryEventPrize;
import com.caohua.raffle.model.RaffleEvent;
import com.caohua.raffle.model.RaffleResult;
import com.caohua.raffle.model.User;
import com.caohua.raffle.service.RaffleService;
import com.caohua.raffle.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/raffle")
public class RaffleController {

    private final RaffleService raffleService;
    private final UserService userService;

    public RaffleController(RaffleService raffleService, UserService userService) {
        this.raffleService = raffleService;
        this.userService = userService;
    }

    // ========== Event Management ==========

    @GetMapping("/events")
    public ResponseEntity<?> listEvents(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        List<Map<String, Object>> list = raffleService.getAllEvents().stream()
                .map(this::eventToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(success(list));
    }

    @GetMapping("/event/{id}/prizes")
    public ResponseEntity<?> getEventPrizes(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        List<LotteryEventPrize> eps = raffleService.getEventPrizes(id);
        List<Map<String, Object>> list = eps.stream().map(ep -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ep.getId());
            m.put("prizeId", ep.getPrize().getId());
            m.put("prizeName", ep.getPrize().getName());
            m.put("prizeImage", ep.getPrize().getImageBase64());
            m.put("quantity", ep.getQuantity());
            m.put("remaining", ep.getRemaining());
            m.put("probability", ep.getProbability());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(success(list));
    }

    @PostMapping("/event/create")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> body, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            String title = (String) body.get("title");
            String startTimeStr = (String) body.get("startTime");
            String endTimeStr = (String) body.get("endTime");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> prizeConfigs = (List<Map<String, Object>>) body.get("prizes");
            if (prizeConfigs == null) prizeConfigs = Collections.emptyList();

            RaffleEvent event = raffleService.createEvent(title, startTime, endTime, prizeConfigs);
            return ResponseEntity.ok(success(eventToMap(event)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("参数错误: " + e.getMessage()));
        }
    }

    @PutMapping("/event/update/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            String title = (String) body.get("title");
            String startTimeStr = (String) body.get("startTime");
            String endTimeStr = (String) body.get("endTime");
            Boolean active = body.get("active") instanceof Boolean ? (Boolean) body.get("active") : null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime startTime = startTimeStr != null ? LocalDateTime.parse(startTimeStr, formatter) : null;
            LocalDateTime endTime = endTimeStr != null ? LocalDateTime.parse(endTimeStr, formatter) : null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> prizeConfigs = (List<Map<String, Object>>) body.get("prizes");

            RaffleEvent event = raffleService.updateEvent(id, title, startTime, endTime, active, prizeConfigs);
            return ResponseEntity.ok(success(eventToMap(event)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("参数错误: " + e.getMessage()));
        }
    }

    @DeleteMapping("/event/delete/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            raffleService.deleteEvent(id);
            return ResponseEntity.ok(success("删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/event/activate/{id}")
    public ResponseEntity<?> activateEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            RaffleEvent event = raffleService.activateEvent(id);
            return ResponseEntity.ok(success(eventToMap(event)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/event/deactivate/{id}")
    public ResponseEntity<?> deactivateEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            RaffleEvent event = raffleService.deactivateEvent(id);
            return ResponseEntity.ok(success(eventToMap(event)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    // ========== Status (Public) ==========

    @GetMapping("/running-events")
    public ResponseEntity<?> runningEvents() {
        List<RaffleEvent> events = raffleService.getRunningEvents();
        List<Map<String, Object>> list = events.stream()
                .map(this::eventToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(success(list));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> map = new HashMap<>();
        RaffleEvent event = raffleService.getCurrentEvent();
        map.put("running", event != null);
        if (event != null) {
            map.put("title", event.getTitle());
            map.put("startTime", event.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            map.put("endTime", event.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            map.put("active", event.getActive());
        }
        return ResponseEntity.ok(success(map));
    }

    @GetMapping("/event/{id}/prizes-public")
    public ResponseEntity<?> getEventPrizesPublic(@PathVariable Long id) {
        List<LotteryEventPrize> eps = raffleService.getEventPrizes(id);
        List<Map<String, Object>> list = eps.stream().map(ep -> {
            Map<String, Object> m = new HashMap<>();
            m.put("prizeId", ep.getPrize().getId());
            m.put("prizeName", ep.getPrize().getName());
            m.put("prizeImage", ep.getPrize().getImageBase64());
            m.put("prizeDesc", ep.getPrize().getDescription());
            m.put("remaining", ep.getRemaining());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(success(list));
    }

    @PostMapping("/draw")
    public ResponseEntity<?> draw(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(error("请先登录"));
        }
        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(error("用户不存在"));
        }
        Long eventId = body.get("eventId") instanceof Number
                ? ((Number) body.get("eventId")).longValue() : null;
        if (eventId == null) {
            return ResponseEntity.badRequest().body(error("请选择抽奖活动"));
        }
        try {
            RaffleResult result = raffleService.doRaffle(user, eventId);
            Map<String, Object> resultMap = resultToMap(result);
            resultMap.put("userDrawCount", userService.getDrawCount(userId, eventId));
            return ResponseEntity.ok(success(resultMap));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    // ========== Results ==========

    @GetMapping("/results")
    public ResponseEntity<?> results(@RequestParam(required = false) String keyword) {
        List<Map<String, Object>> list = raffleService.searchResults(keyword).stream()
                .map(this::resultToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(success(list));
    }

    @PutMapping("/result/update/{id}")
    public ResponseEntity<?> updateResult(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        Long userId = body.get("userId") instanceof Number ? ((Number) body.get("userId")).longValue() : null;
        Long prizeId = body.get("prizeId") instanceof Number ? ((Number) body.get("prizeId")).longValue() : null;
        String raffleTimeStr = (String) body.get("raffleTime");

        if (userId == null || prizeId == null) {
            return ResponseEntity.badRequest().body(error("缺少必要参数"));
        }

        LocalDateTime raffleTime = null;
        if (raffleTimeStr != null && !raffleTimeStr.isEmpty()) {
            try {
                raffleTime = LocalDateTime.parse(raffleTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(error("时间格式错误"));
            }
        }

        try {
            RaffleResult result = raffleService.updateResult(id, userId, prizeId, raffleTime);
            return ResponseEntity.ok(success(resultToMap(result)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/result/delete/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            raffleService.deleteResult(id);
            return ResponseEntity.ok(success("删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/result/process/{id}")
    public ResponseEntity<?> processResult(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            RaffleResult result = raffleService.processResult(id);
            return ResponseEntity.ok(success(resultToMap(result)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/my-result")
    public ResponseEntity<?> myResult(@RequestParam(required = false) Long eventId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null || eventId == null) {
            return ResponseEntity.ok(success(null));
        }
        boolean hasWon = raffleService.hasUserWon(userId, eventId);
        return ResponseEntity.ok(success(hasWon));
    }

    // ========== Helpers ==========

    private Map<String, Object> eventToMap(RaffleEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("title", event.getTitle());
        map.put("startTime", event.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        map.put("endTime", event.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        map.put("active", event.getActive());
        map.put("running", event.isRunning());
        map.put("createdAt", event.getCreatedAt() != null
                ? event.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
        return map;
    }

    private Map<String, Object> resultToMap(RaffleResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", result.getId());
        map.put("userId", result.getUser().getId());
        map.put("prizeId", result.getPrize().getId());
        map.put("accountId", result.getUser().getAccountId());
        map.put("userName", result.getUser().getRealName());
        map.put("prizeName", result.getPrize().getName());
        map.put("prizeImage", result.getPrize().getImageBase64());
        map.put("raffleTime", result.getRaffleTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        map.put("processed", result.getProcessed() != null && result.getProcessed());
        if (result.getEvent() != null) {
            map.put("eventTitle", result.getEvent().getTitle());
        }
        return map;
    }

    private boolean isAdmin(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin != null && isAdmin;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }
}

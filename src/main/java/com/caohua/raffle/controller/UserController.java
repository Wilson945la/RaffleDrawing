package com.caohua.raffle.controller;

import com.caohua.raffle.model.User;
import com.caohua.raffle.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/generate-id")
    public ResponseEntity<?> generateId() {
        return ResponseEntity.ok(success(userService.generateAccountId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String accountId = body.get("accountId");
        String realName = body.get("realName"); // optional

        if (accountId == null || accountId.isBlank()) {
            return ResponseEntity.badRequest().body(error("账号ID不能为空"));
        }

        try {
            User user = userService.login(accountId.trim(), realName);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getRealName());
            session.setAttribute("isAdmin", user.getAdmin());
            return ResponseEntity.ok(success(userToMap(user)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(error("未登录"));
        }
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(success(userToMap(user))))
                .orElse(ResponseEntity.ok(error("用户不存在")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(success("已退出登录"));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) String keyword, HttpSession session) {
        if (session.getAttribute("isAdmin") == null || !(Boolean) session.getAttribute("isAdmin")) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        List<Map<String, Object>> users = userService.searchUsers(keyword).stream()
                .map(this::userToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(success(users));
    }

    @PostMapping("/add-draw-count")
    public ResponseEntity<?> addDrawCount(@RequestBody Map<String, Object> body, HttpSession session) {
        if (session.getAttribute("isAdmin") == null || !(Boolean) session.getAttribute("isAdmin")) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        Long userId = body.get("userId") instanceof Number
                ? ((Number) body.get("userId")).longValue() : null;
        Long eventId = body.get("eventId") instanceof Number
                ? ((Number) body.get("eventId")).longValue() : null;
        if (userId == null || eventId == null) {
            return ResponseEntity.badRequest().body(error("请提供用户ID和活动ID"));
        }
        try {
            Map<String, Object> result = userService.addDrawCount(userId, eventId);
            return ResponseEntity.ok(success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/draw-count/{eventId}")
    public ResponseEntity<?> getDrawCount(@PathVariable Long eventId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(success(0));
        }
        return ResponseEntity.ok(success(userService.getDrawCount(userId, eventId)));
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("accountId", user.getAccountId());
        map.put("realName", user.getRealName());
        map.put("admin", user.getAdmin());
        return map;
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

package com.caohua.raffle.controller;

import com.caohua.raffle.model.Prize;
import com.caohua.raffle.service.PrizeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prize")
public class PrizeController {

    private final PrizeService prizeService;

    public PrizeController(PrizeService prizeService) {
        this.prizeService = prizeService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        List<Map<String, Object>> list = prizeService.getAllPrizes().stream()
                .map(this::prizeToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(success(list));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            String imageBase64 = null;
            if (image != null && !image.isEmpty()) {
                imageBase64 = "data:" + image.getContentType() + ";base64," +
                        Base64.getEncoder().encodeToString(image.getBytes());
            }
            Prize prize = prizeService.createPrize(name, description, imageBase64);
            return ResponseEntity.ok(success(prizeToMap(prize)));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(error("图片上传失败"));
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        try {
            String imageBase64 = null;
            if (image != null && !image.isEmpty()) {
                imageBase64 = "data:" + image.getContentType() + ";base64," +
                        Base64.getEncoder().encodeToString(image.getBytes());
            }
            Prize prize = prizeService.updatePrize(id, name, description, imageBase64);
            return ResponseEntity.ok(success(prizeToMap(prize)));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(error("图片上传失败"));
        }
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(error("无权操作"));
        }
        prizeService.deletePrize(id);
        return ResponseEntity.ok(success("删除成功"));
    }

    private Map<String, Object> prizeToMap(Prize prize) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", prize.getId());
        map.put("name", prize.getName());
        map.put("description", prize.getDescription());
        map.put("imageBase64", prize.getImageBase64());
        map.put("displayOrder", prize.getDisplayOrder());
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

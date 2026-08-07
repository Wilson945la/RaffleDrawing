package com.caohua.raffle.service;

import com.caohua.raffle.model.Prize;
import com.caohua.raffle.repository.PrizeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrizeService {

    private final PrizeRepository prizeRepository;

    public PrizeService(PrizeRepository prizeRepository) {
        this.prizeRepository = prizeRepository;
    }

    public Prize createPrize(String name, String description, String imageBase64) {
        Prize prize = new Prize();
        prize.setName(name);
        prize.setDescription(description);
        prize.setImageBase64(imageBase64);
        return prizeRepository.save(prize);
    }

    public Prize updatePrize(Long id, String name, String description, String imageBase64) {
        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("奖品不存在"));
        prize.setName(name);
        prize.setDescription(description);
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            prize.setImageBase64(imageBase64);
        }
        return prizeRepository.save(prize);
    }

    public void deletePrize(Long id) {
        prizeRepository.deleteById(id);
    }

    public List<Prize> getAllPrizes() {
        return prizeRepository.findAllByOrderByDisplayOrderAsc();
    }

    public Optional<Prize> findById(Long id) {
        return prizeRepository.findById(id);
    }
}

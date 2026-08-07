package com.caohua.raffle.config;

import com.caohua.raffle.model.User;
import com.caohua.raffle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    @Value("${app.admin.account-id:chfz-00000000}")
    private String adminAccountId;

    @Value("${app.admin.name:管理员}")
    private String adminName;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByAccountId(adminAccountId)) {
            User admin = new User();
            admin.setAccountId(adminAccountId);
            admin.setRealName(adminName);
            admin.setAdmin(true);
            userRepository.save(admin);
            log.info("默认管理员已创建: {} ({})", adminName, adminAccountId);
        } else {
            log.info("管理员账号已存在: {}", adminAccountId);
        }
    }
}

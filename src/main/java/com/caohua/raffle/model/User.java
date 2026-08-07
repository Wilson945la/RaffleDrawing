package com.caohua.raffle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, unique = true, length = 30)
    private String accountId;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(nullable = false)
    private Boolean admin = false;

    @Column(name = "draw_count", nullable = false)
    private Integer drawCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public Boolean getAdmin() { return admin; }
    public void setAdmin(Boolean admin) { this.admin = admin; }

    public Integer getDrawCount() { return drawCount; }
    public void setDrawCount(Integer drawCount) { this.drawCount = drawCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

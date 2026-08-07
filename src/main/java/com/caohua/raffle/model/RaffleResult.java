package com.caohua.raffle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "raffle_results")
public class RaffleResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prize_id", nullable = false)
    private Prize prize;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private RaffleEvent event;

    @Column(name = "raffle_time", nullable = false)
    private LocalDateTime raffleTime;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @PrePersist
    protected void onCreate() {
        raffleTime = LocalDateTime.now();
        if (processed == null) {
            processed = false;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Prize getPrize() { return prize; }
    public void setPrize(Prize prize) { this.prize = prize; }

    public RaffleEvent getEvent() { return event; }
    public void setEvent(RaffleEvent event) { this.event = event; }

    public LocalDateTime getRaffleTime() { return raffleTime; }
    public void setRaffleTime(LocalDateTime raffleTime) { this.raffleTime = raffleTime; }

    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
}

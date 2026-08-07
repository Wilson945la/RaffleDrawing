package com.caohua.raffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "lottery_event_prizes")
public class LotteryEventPrize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private RaffleEvent event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prize_id", nullable = false)
    private Prize prize;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private Integer remaining = 0;

    @Column(nullable = false)
    private Double probability = 0.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RaffleEvent getEvent() { return event; }
    public void setEvent(RaffleEvent event) { this.event = event; }

    public Prize getPrize() { return prize; }
    public void setPrize(Prize prize) { this.prize = prize; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.remaining == null || this.remaining > quantity) {
            this.remaining = quantity;
        }
    }

    public Integer getRemaining() { return remaining; }
    public void setRemaining(Integer remaining) { this.remaining = remaining; }

    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }
}

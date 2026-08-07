package com.caohua.raffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_event_draw_counts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
public class UserEventDrawCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private RaffleEvent event;

    @Column(name = "draw_count", nullable = false)
    private Integer drawCount = 0;

    public UserEventDrawCount() {}

    public UserEventDrawCount(User user, RaffleEvent event, Integer drawCount) {
        this.user = user;
        this.event = event;
        this.drawCount = drawCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public RaffleEvent getEvent() { return event; }
    public void setEvent(RaffleEvent event) { this.event = event; }

    public Integer getDrawCount() { return drawCount; }
    public void setDrawCount(Integer drawCount) { this.drawCount = drawCount; }
}

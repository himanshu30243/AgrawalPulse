package com.agrawalpulse.event.entity;

import com.agrawalpulse.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    // chapters is owned by user-service - plain indexed UUID column, no FK (see
    // docs/microservices-contract.md "no cross-service FKs").
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column
    private String location;
}

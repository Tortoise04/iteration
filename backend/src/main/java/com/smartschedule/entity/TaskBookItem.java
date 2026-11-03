package com.smartschedule.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务书条目（task_book_items）—— 某天要做的一条具体的事。
 * plan_date 会随顺延改变，origin_date 永不改写。
 */
@Entity
@Table(name = "task_book_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaskBookItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "week_id")
    private Long weekId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "origin_date", nullable = false)
    private LocalDate originDate;

    @Column(length = 200)
    private String title;

    @Column(name = "learn_content", columnDefinition = "TEXT")
    private String learnContent;

    @Column(name = "est_minutes")
    private Integer estMinutes;

    @Column(name = "done_criteria", length = 255)
    private String doneCriteria;

    @Column(length = 16)
    private String category = "STUDY";

    @Column(length = 16)
    private String status = "PENDING";

    @Column(name = "defer_count")
    private Integer deferCount = 0;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

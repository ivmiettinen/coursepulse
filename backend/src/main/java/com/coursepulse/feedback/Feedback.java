package com.coursepulse.feedback;

import com.coursepulse.common.persistence.AuditableEntity;
import com.coursepulse.course.CourseOffering;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "feedback")
public class Feedback extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @Size(max = 2000)
    private String comment;

    @NotNull
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected Feedback() {
        // Required by JPA.
    }

    public Feedback(CourseOffering courseOffering, int rating, String comment, Instant submittedAt) {
        this.courseOffering = courseOffering;
        this.rating = rating;
        this.comment = comment;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}

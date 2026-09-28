package com.coursepulse.course;

import com.coursepulse.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "course_offering")
public class CourseOffering extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotBlank(message = "Academic year is required.")
    @Size(max = 20, message = "Academic year must not exceed {max} characters.")
    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @NotBlank(message = "Period is required.")
    @Size(max = 50, message = "Period must not exceed {max} characters.")
    @Column(nullable = false)
    private String period;

    protected CourseOffering() {
        // Required by JPA.
    }

    public CourseOffering(Course course, String academicYear, String period) {
        this.course = course;
        this.academicYear = academicYear;
        this.period = period;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}

package com.coursepulse.course;
import com.coursepulse.common.auditing.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "course")
public class Course extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course code is required.")
    @Size(max = 20, message = "Course code must not exceed {max} characters.")
    @Column(nullable = false, unique = true)
    private String code;

    @NotBlank(message = "Course name is required.")
    @Size(max = 200, message = "Course name must not exceed {max} characters.")
    @Column(nullable = false)
    private String name;

    protected Course() {
        // Required by JPA.
    }

    public Course(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
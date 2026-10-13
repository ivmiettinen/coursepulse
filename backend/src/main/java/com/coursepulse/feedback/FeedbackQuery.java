package com.coursepulse.feedback;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackQuery {

    @NotNull(message = "Page is required.")
    @Min(value = 0, message = "Page must be at least {value}.")
    private Integer page = 0;

    @NotNull(message = "Size is required.")
    @Min(value = 1, message = "Size must be at least {value}.")
    @Max(value = 100, message = "Size must not exceed {value}.")
    private Integer size = 20;

    @Min(value = 1, message = "Rating must be at least {value}.")
    @Max(value = 5, message = "Rating must not exceed {value}.")
    private Integer rating;

    @Size(max = 200, message = "Search must not exceed {max} characters.")
    private String search;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    // JPA accepts an int offset even though PageRequest calculates it as a long.
    @AssertTrue(message = "Requested page is too large.")
    public boolean isOffsetWithinRange() {
        return page == null || size == null || (long) page * size <= Integer.MAX_VALUE;
    }
}

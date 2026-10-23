package com.coursepulse.feedback;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-offerings/{id}")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/feedback")
    public FeedbackPageDto getFeedback(@PathVariable Long id, @Valid @ModelAttribute FeedbackQuery query) {
        return feedbackService.getFeedback(id, query);
    }

    @GetMapping("/feedback-summary")
    public FeedbackSummaryDto getFeedbackSummary(@PathVariable Long id) {
        return feedbackService.getFeedbackSummary(id);
    }

}

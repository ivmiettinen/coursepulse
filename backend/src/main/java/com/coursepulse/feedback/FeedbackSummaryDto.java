package com.coursepulse.feedback;

import java.util.List;

public record FeedbackSummaryDto(long totalFeedback, Double averageRating,
                                 List<FeedbackRatingCountDto> ratingDistribution){
}

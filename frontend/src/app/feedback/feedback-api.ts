import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FeedbackPageResponse, FeedbackQuery, FeedbackSummaryResponse } from './feedback.models';

@Injectable({ providedIn: 'root' })
export class FeedbackApi {
  private readonly http = inject(HttpClient);

  getFeedbackSummary(courseOfferingId: number): Observable<FeedbackSummaryResponse> {
    return this.http.get<FeedbackSummaryResponse>(
      `/api/course-offerings/${courseOfferingId}/feedback-summary`,
    );
  }

  getFeedback(courseOfferingId: number, query: FeedbackQuery = {}): Observable<FeedbackPageResponse> {
    const search = query.search?.trim();
    const params: Record<string, string | number> = {
      page: query.page ?? 0,
      size: query.size ?? 20,
      sort: query.sort ?? 'submittedAt,desc',
    };

    if (search) params['search'] = search;
    if (query.rating != null) params['rating'] = query.rating;

    return this.http.get<FeedbackPageResponse>(
      `/api/course-offerings/${courseOfferingId}/feedback`,
      { params },
    );
  }
}

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FeedbackPageResponse } from './feedback.models';

@Injectable({ providedIn: 'root' })
export class FeedbackApi {
  private readonly http = inject(HttpClient);

  getFeedback(courseOfferingId: number, page = 0, size = 20): Observable<FeedbackPageResponse> {
    return this.http.get<FeedbackPageResponse>(
      `/api/course-offerings/${courseOfferingId}/feedback`,
      { params: { page, size } },
    );
  }
}

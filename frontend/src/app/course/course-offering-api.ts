import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CourseOffering } from './course-offering';

@Injectable({ providedIn: 'root' })
export class CourseOfferingApi {
  private readonly http = inject(HttpClient);

  getCourseOfferings(courseId: number): Observable<readonly CourseOffering[]> {
    return this.http.get<readonly CourseOffering[]>(`/api/courses/${courseId}/offerings`);
  }

  getCourseOffering(courseOfferingId: number): Observable<CourseOffering> {
    return this.http.get<CourseOffering>(`/api/course-offerings/${courseOfferingId}`);
  }
}

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Course } from './course';

@Injectable({ providedIn: 'root' })
export class CourseApi {
  private readonly http = inject(HttpClient);

  getCourses(): Observable<readonly Course[]> {
    return this.http.get<readonly Course[]>('/api/courses');
  }
}

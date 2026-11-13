import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { CourseOffering } from './course/course-offering';
import { FeedbackPage } from './feedback/feedback-page';

describe('Application routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('sets the default offering URL before loading its details once', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', FeedbackPage);
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/feedback?offeringId=1');
    const offering: CourseOffering = {
      id: 1,
      courseId: 1,
      courseCode: 'CS101',
      courseName: 'Introduction to Programming',
      academicYear: '2025-2026',
      period: 'Period 1',
    };

    TestBed.inject(HttpTestingController).expectOne('/api/courses').flush([
      { id: offering.courseId, code: offering.courseCode, name: offering.courseName },
    ]);
    TestBed.inject(HttpTestingController)
      .expectOne({ method: 'GET', url: '/api/course-offerings/1' })
      .flush(offering);
    TestBed.inject(HttpTestingController).expectOne('/api/courses/1/offerings').flush([offering]);
    await harness.fixture.whenStable();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/course-offerings/1/feedback-summary')
      .flush({
        totalFeedback: 0, averageRating: null,
        ratingDistribution: [1, 2, 3, 4, 5].map((rating) => ({ rating, count: 0 })),
      });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/course-offerings/1/feedback?page=0&size=20&sort=submittedAt,desc')
      .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    expect(TestBed.inject(Router).url).toBe('/feedback?offeringId=1');
    TestBed.inject(HttpTestingController).expectNone('/api/course-offerings/1');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(offering.courseName);
  });
});

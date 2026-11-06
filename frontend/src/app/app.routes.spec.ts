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

  it('redirects the root URL to the feedback page and displays course details', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', FeedbackPage);
    const offering: CourseOffering = {
      id: 1,
      courseId: 1,
      courseCode: 'CS101',
      courseName: 'Introduction to Programming',
      academicYear: '2025-2026',
      period: 'Period 1',
    };

    TestBed.inject(HttpTestingController)
      .expectOne({ method: 'GET', url: '/api/course-offerings/1' })
      .flush(offering);
    await harness.fixture.whenStable();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/course-offerings/1/feedback?page=0&size=20')
      .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    expect(TestBed.inject(Router).url).toBe('/feedback');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(offering.courseName);
  });
});

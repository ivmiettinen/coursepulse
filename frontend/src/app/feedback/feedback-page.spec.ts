import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseOffering } from '../course/course-offering';
import { FeedbackPage } from './feedback-page';

describe('FeedbackPage course details', () => {
  const offering: CourseOffering = {
    id: 1,
    courseId: 1,
    courseCode: 'CS101',
    courseName: 'Introduction to Programming',
    academicYear: '2025-2026',
    period: 'Period 1',
  };
  let fixture: ComponentFixture<FeedbackPage>;
  let element: HTMLElement;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FeedbackPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FeedbackPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('shows a loading state and requests only the course details', () => {
    expect(element.querySelector('[role="status"]')?.textContent).toContain(
      'Loading course details',
    );
    expect(element.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(element.querySelector('mat-progress-bar')?.getAttribute('aria-label')).toBe(
      'Loading course details',
    );
    httpTesting.expectNone('/api/course-offerings/1/feedback');
    httpTesting.expectOne({ method: 'GET', url: '/api/course-offerings/1' }).flush(offering);
  });

  it('renders the course name, code, academic year and period from the response', async () => {
    httpTesting.expectOne('/api/course-offerings/1').flush(offering);
    await fixture.whenStable();

    expect(element.querySelector('h1')?.textContent).toBe(offering.courseName);
    expect(Array.from(element.querySelectorAll('dd'), (item) => item.textContent?.trim())).toEqual([
      offering.courseCode,
      offering.academicYear,
      offering.period,
    ]);
    expect(element.querySelector('[aria-busy="true"]')).toBeNull();
    expect(element.querySelector('[role="alert"]')).toBeNull();

    fixture.detectChanges();
    httpTesting.expectNone('/api/course-offerings/1');
  });

  it('explains when the course offering is not found', async () => {
    httpTesting.expectOne('/api/course-offerings/1').flush(null, {
      status: 404,
      statusText: 'Not Found',
    });
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toBe(
      'This course offering could not be found.',
    );
    expect(element.querySelector('button')?.textContent).toContain('Try again');
    expect(element.querySelector('dl')).toBeNull();
  });

  it.each(['server', 'network'])('shows a retry action after a %s error', async (failure) => {
    const request = httpTesting.expectOne('/api/course-offerings/1');
    if (failure === 'server') {
      request.flush(null, { status: 500, statusText: 'Internal Server Error' });
    } else {
      request.error(new ProgressEvent('error'));
    }
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toBe(
      'We could not load the course details. Please try again.',
    );
    expect(element.querySelector('button')?.textContent).toContain('Try again');
    expect(element.querySelector('[aria-busy="true"]')).toBeNull();
  });

  it('can retry repeatedly after failures and then display the course details', async () => {
    for (let attempt = 0; attempt < 2; attempt++) {
      httpTesting.expectOne('/api/course-offerings/1').flush(null, {
        status: 503,
        statusText: 'Service Unavailable',
      });
      await fixture.whenStable();

      const retry = element.querySelector('button');
      expect(retry).not.toBeNull();
      retry!.click();
      await fixture.whenStable();

      expect(element.querySelector('[role="status"]')?.textContent).toContain(
        'Loading course details',
      );
      expect(element.querySelector('[role="alert"]')).toBeNull();
      expect(element.querySelector('button')).toBeNull();
    }

    httpTesting.expectOne('/api/course-offerings/1').flush(offering);
    await fixture.whenStable();

    expect(element.querySelector('h1')?.textContent).toBe(offering.courseName);
  });

  it('cancels an in-flight request when the page is destroyed', () => {
    const request = httpTesting.expectOne('/api/course-offerings/1');

    fixture.destroy();

    expect(request.cancelled).toBe(true);
  });
});

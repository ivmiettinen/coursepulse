import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { provideRouter, Router } from '@angular/router';
import { CourseOffering } from '../course/course-offering';
import { FeedbackPage } from './feedback-page';
import { FeedbackPageResponse } from './feedback.models';

describe('FeedbackPage', () => {
  const feedbackUrl = '/api/course-offerings/1/feedback?page=0&size=20&sort=submittedAt,desc';
  const emptyPage: FeedbackPageResponse = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  };
  const feedback = (id: number) => ({
    id, rating: 5, comment: null, submittedAt: '2026-10-28T10:00:00Z',
  });
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

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [FeedbackPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    await TestBed.inject(Router).navigateByUrl('/?offeringId=1');
    fixture = TestBed.createComponent(FeedbackPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpTesting.expectOne('/api/courses').flush([{ id: 1, code: 'CS101', name: offering.courseName }]);
    await fixture.whenStable();
  });

  afterEach(() => httpTesting.verify());

  it('loads course details before requesting feedback', async () => {
    expect(element.querySelector('[role="status"]')?.textContent).toContain(
      'Loading course details',
    );
    expect(element.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(element.querySelector('mat-progress-bar')?.getAttribute('aria-label')).toBe(
      'Loading course details',
    );
    httpTesting.expectNone('/api/course-offerings/1/feedback?page=0&size=20&sort=submittedAt,desc');
    await loadCourseDetails();
    httpTesting.expectOne(feedbackUrl).flush(emptyPage);
  });

  it('renders the course name, code, academic year and period from the response', async () => {
    await loadCourseDetails();

    expect(element.querySelector('h1')?.textContent).toBe(offering.courseName);
    expect(Array.from(element.querySelectorAll('.offering-details dd'), (item) => item.textContent?.trim())).toEqual([
      offering.courseCode,
      offering.academicYear,
      offering.period,
    ]);
    httpTesting.expectOne(feedbackUrl).flush(emptyPage);
    await fixture.whenStable();
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

    await loadCourseDetails();

    expect(element.querySelector('h1')?.textContent).toBe(offering.courseName);
    httpTesting.expectOne(feedbackUrl).flush(emptyPage);
  });

  async function loadCourseDetails(): Promise<void> {
    httpTesting.expectOne('/api/course-offerings/1').flush(offering);
    httpTesting.expectOne('/api/courses/1/offerings').flush([offering, { ...offering, id: 2 }]);
    await fixture.whenStable();
    httpTesting.expectOne('/api/course-offerings/1/feedback-summary').flush({
      totalFeedback: 1, averageRating: 5,
      ratingDistribution: [
        { rating: 1, count: 0 },
        { rating: 2, count: 0 },
        { rating: 3, count: 0 },
        { rating: 4, count: 0 },
        { rating: 5, count: 1 },
      ],
    });
    await fixture.whenStable();
  }

  async function loadPaginatedFeedback(): Promise<MatPaginatorHarness> {
    await loadCourseDetails();
    httpTesting.expectOne(feedbackUrl).flush({
      ...emptyPage, totalElements: 43, totalPages: 3,
      content: Array.from({ length: 20 }, (_, index) => feedback(index + 1)),
    });
    return TestbedHarnessEnvironment.loader(fixture).getHarness(MatPaginatorHarness);
  }

  it('uses the total count to navigate between page bounds', async () => {
    const paginator = await loadPaginatedFeedback();
    expect(await paginator.getPageSize()).toBe(20);
    expect(await paginator.getRangeLabel()).toBe('1 – 20 of 43');
    expect(await paginator.isPreviousPageDisabled()).toBe(true);
    await paginator.goToLastPage();
    httpTesting.expectOne('/api/course-offerings/1/feedback?page=2&size=20&sort=submittedAt,desc').flush({
      ...emptyPage, page: 2, totalElements: 43, totalPages: 3,
      content: [41, 42, 43].map(feedback),
    });
    await fixture.whenStable();
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Showing 41–43 of 43');
    expect(await paginator.isNextPageDisabled()).toBe(true);
    httpTesting.expectNone('/api/course-offerings/1');
  });


  it('shows feedback loading independently of the course heading', async () => {
    await loadCourseDetails();
    expect(element.querySelector('h1')?.textContent).toBe(offering.courseName);
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading feedback');
    expect(element.querySelector('app-feedback-list [aria-busy="true"]')).not.toBeNull();
    httpTesting.expectOne({ method: 'GET', url: feedbackUrl }).flush(emptyPage);
  });

  it('renders feedback in API order, a null comment and a UTC submission date', async () => {
    await loadCourseDetails();
    const comment = '<strong>Useful exercises</strong>\nClear examples.';
    httpTesting.expectOne(feedbackUrl).flush({
      ...emptyPage,
      content: [
        { id: 12, rating: 4, comment, submittedAt: '2026-10-28T23:30:00Z' },
        { id: 11, rating: 2, comment: null, submittedAt: '2026-10-27T09:00:00Z' },
      ],
      totalElements: 23,
      totalPages: 2,
    } satisfies FeedbackPageResponse);
    await fixture.whenStable();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].querySelector('td')?.textContent).toBe('4 / 5');
    expect(rows[0].querySelector('.comment')?.textContent).toBe(comment);
    expect(rows[0].querySelector('.comment strong')).toBeNull();
    expect(rows[1].querySelector('.comment')?.textContent).toBe('No comment provided.');
    expect(rows[0].querySelector('time')?.textContent).toBe('Oct 28, 2026');
    expect(rows[0].querySelector('time')?.getAttribute('datetime')).toBe('2026-10-28T23:30:00Z');
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Showing 1–2 of 23');
    expect(element.querySelector('caption')?.textContent).toContain('UTC');
    const cards = element.querySelectorAll('.feedback-cards li');
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('4 / 5');
    expect(cards[0].querySelector('.comment')?.textContent).toBe(comment);
    expect(cards[0].querySelector('.comment strong')).toBeNull();
    expect(cards[1].querySelector('.comment')?.textContent).toBe('No comment provided.');
    expect(cards[0].querySelector('time')?.textContent).toMatch(/11:30:00\sPM/);
    expect(cards[0].querySelector('time')?.getAttribute('datetime')).toBe('2026-10-28T23:30:00Z');
    httpTesting.expectNone('/api/course-offerings/1');
  });

  it('shows an empty state instead of an empty table', async () => {
    await loadCourseDetails();
    httpTesting.expectOne(feedbackUrl).flush(emptyPage);
    await fixture.whenStable();
    expect(element.querySelector('[role="status"]')?.textContent).toContain('No feedback yet');
    expect(element.querySelector('table')).toBeNull();
    expect(element.querySelector('[aria-busy="true"]')).toBeNull();
  });

  it.each(['server', 'network', 'not-found'])(
    'retries feedback after repeated %s errors without reloading course details',
    async (failure) => {
      await loadCourseDetails();
      for (let attempt = 0; attempt < 2; attempt++) {
        const request = httpTesting.expectOne(feedbackUrl);
        if (failure === 'network') {
          request.error(new ProgressEvent('error'));
        } else {
          request.flush(null, {
            status: failure === 'not-found' ? 404 : 503,
            statusText: failure === 'not-found' ? 'Not Found' : 'Service Unavailable',
          });
        }
        await fixture.whenStable();
        expect(element.querySelector('[role="alert"]')?.textContent).toBe(
          failure === 'not-found'
            ? 'This course offering could not be found.'
            : 'We could not load the feedback. Please try again.',
        );
        expect(element.querySelector('h1')?.textContent).toBe(offering.courseName);
        element.querySelector<HTMLButtonElement>('app-feedback-list .loading-indicator button')!.click();
        await fixture.whenStable();
        expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading feedback');
        expect(element.querySelector('[role="alert"]')).toBeNull();
        httpTesting.expectNone('/api/course-offerings/1');
      }
      httpTesting.expectOne(feedbackUrl).flush(emptyPage);
      await fixture.whenStable();
      expect(element.querySelector('[role="status"]')?.textContent).toContain('No feedback yet');
    },
  );

  it('cancels feedback loading when the page is destroyed', async () => {
    await loadCourseDetails();
    const request = httpTesting.expectOne(feedbackUrl);
    fixture.destroy();
    expect(request.cancelled).toBe(true);
  });

  it('keeps the summary independent of list filtering and retries it for a new offering', async () => {
    await loadCourseDetails();
    httpTesting.expectOne(feedbackUrl).flush(emptyPage);
    await fixture.whenStable();
    // Filtering affects only the list; the summary still describes the whole offering.
    const search = element.querySelector<HTMLInputElement>('app-feedback-filter-bar input')!;
    search.value = 'clear';
    search.dispatchEvent(new Event('input'));
    element.querySelector<HTMLFormElement>('app-feedback-filter-bar form')!
      .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();
    httpTesting.expectOne((request) => request.url.endsWith('/feedback') && request.params.get('search') === 'clear')
      .flush(emptyPage);
    httpTesting.expectNone('/api/course-offerings/1/feedback-summary');
    expect(element.querySelector('app-feedback-summary')?.textContent).toContain('5.0 / 5');

    // A new offering needs both requests; a summary failure must not hide the list.
    await TestBed.inject(Router).navigateByUrl('/?offeringId=2');
    await fixture.whenStable();
    httpTesting.expectOne('/api/course-offerings/2').flush({ ...offering, id: 2 });
    await fixture.whenStable();
    const summaryRequest = httpTesting.expectOne('/api/course-offerings/2/feedback-summary');
    httpTesting.expectOne('/api/course-offerings/2/feedback?page=0&size=20&sort=submittedAt,desc').flush(emptyPage);
    summaryRequest.flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();
    expect(element.querySelector('app-feedback-list')?.textContent).toContain('No feedback yet');
    // Retrying the summary must not reload the already available list.
    element.querySelector<HTMLButtonElement>('app-feedback-summary button')!.click();
    await fixture.whenStable();
    httpTesting.expectOne('/api/course-offerings/2/feedback-summary').flush({
      totalFeedback: 0, averageRating: null,
      ratingDistribution: [1, 2, 3, 4, 5].map((rating) => ({ rating, count: 0 })),
    });
    httpTesting.expectNone((request) => request.url.endsWith('/feedback'));
  });

  it('cancels an in-flight request when the page is destroyed', () => {
    const request = httpTesting.expectOne('/api/course-offerings/1');

    fixture.destroy();

    expect(request.cancelled).toBe(true);
  });
});

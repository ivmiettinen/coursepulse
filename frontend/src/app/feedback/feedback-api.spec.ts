import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { FeedbackApi } from './feedback-api';
import { FeedbackPageResponse } from './feedback.models';

describe('FeedbackApi', () => {
  let api: FeedbackApi;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(FeedbackApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends the selected zero-based page and page size', async () => {
    const result = firstValueFrom(api.getFeedback(123, 2, 10));
    const request = httpTesting.expectOne({
      method: 'GET', url: '/api/course-offerings/123/feedback?page=2&size=10',
    });
    request.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
    expect((await result).page).toBe(2);
  });

  it('gets feedback using default pagination and preserves comments and page metadata', async () => {
    const page: FeedbackPageResponse = {
      content: [
        { id: 10, rating: 5, comment: 'Clear course.', submittedAt: '2026-10-27T10:00:00Z' },
        { id: 9, rating: 3, comment: null, submittedAt: '2026-10-26T09:00:00Z' },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    };
    const result = firstValueFrom(api.getFeedback(123));

    const request = httpTesting.expectOne({
      method: 'GET',
      url: '/api/course-offerings/123/feedback?page=0&size=20',
    });
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush(page);

    expect(await result).toEqual(page);
  });

  it('returns an empty page as a successful response', async () => {
    const page: FeedbackPageResponse = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };
    const result = firstValueFrom(api.getFeedback(123));

    httpTesting.expectOne('/api/course-offerings/123/feedback?page=0&size=20').flush(page);

    expect(await result).toEqual(page);
  });

  it.each([404, 500])('passes HTTP %s errors instead of returning an empty page', async (status) => {
    const body = { detail: 'Feedback request failed.' };
    const result = firstValueFrom(api.getFeedback(123));
    const assertion = expect(result).rejects.toMatchObject({ status, error: body });

    httpTesting.expectOne('/api/course-offerings/123/feedback?page=0&size=20').flush(body, {
      status,
      statusText: status === 404 ? 'Not Found' : 'Internal Server Error',
    });

    await assertion;
  });

  it('passes network errors to the caller', async () => {
    const result = firstValueFrom(api.getFeedback(123));
    const assertion = expect(result).rejects.toBeInstanceOf(HttpErrorResponse);
    const statusAssertion = expect(result).rejects.toMatchObject({ status: 0 });

    httpTesting.expectOne('/api/course-offerings/123/feedback?page=0&size=20').error(new ProgressEvent('error'));

    await Promise.all([assertion, statusAssertion]);
  });
});

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CourseOfferingApi } from './course-offering-api';
import { CourseOffering } from './course-offering';

describe('CourseOfferingApi', () => {
  let api: CourseOfferingApi;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CourseOfferingApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('gets offerings for the requested course and preserves their details and response order', async () => {
    const offerings: readonly CourseOffering[] = [
      {
        id: 42,
        courseId: 7,
        courseCode: 'CS101',
        courseName: 'Programming Fundamentals',
        academicYear: '2026–2027',
        period: 'Period 1',
      },
      {
        id: 15,
        courseId: 7,
        courseCode: 'CS101',
        courseName: 'Programming Fundamentals',
        academicYear: '2025–2026',
        period: 'Period 2',
      },
    ];
    const result = firstValueFrom(api.getCourseOfferings(7));

    httpTesting.expectOne({ method: 'GET', url: '/api/courses/7/offerings' }).flush(offerings);

    expect(await result).toEqual(offerings);
  });

  it('returns an empty offering list as a successful response', async () => {
    const result = firstValueFrom(api.getCourseOfferings(7));

    httpTesting.expectOne('/api/courses/7/offerings').flush([]);

    expect(await result).toEqual([]);
  });

  it.each([
    { courseId: 0, status: 400, statusText: 'Bad Request' },
    { courseId: 999, status: 404, statusText: 'Not Found' },
    { courseId: 7, status: 500, statusText: 'Internal Server Error' },
  ])('passes HTTP $status errors from the offering list to the caller', async ({ courseId, status, statusText }) => {
    const body = { detail: 'Course offerings request failed.' };
    const result = firstValueFrom(api.getCourseOfferings(courseId));
    const assertion = expect(result).rejects.toMatchObject({ status, error: body });

    httpTesting.expectOne(`/api/courses/${courseId}/offerings`).flush(body, { status, statusText });

    await assertion;
  });

  it('gets the requested course offering and returns its details', async () => {
    const offering: CourseOffering = {
      id: 42,
      courseId: 7,
      courseCode: 'CS101',
      courseName: 'Programming Fundamentals',
      academicYear: '2026–2027',
      period: 'Period 1',
    };
    const result = firstValueFrom(api.getCourseOffering(42));

    httpTesting.expectOne({ method: 'GET', url: '/api/course-offerings/42' }).flush(offering);

    expect(await result).toEqual(offering);
  });

  it.each([404, 500])('passes HTTP %s errors to the caller', async (status) => {
    const body = { detail: 'Course offering request failed.' };
    const result = firstValueFrom(api.getCourseOffering(42));
    const assertion = expect(result).rejects.toMatchObject({ status, error: body });

    httpTesting.expectOne('/api/course-offerings/42').flush(body, {
      status,
      statusText: status === 404 ? 'Not Found' : 'Internal Server Error',
    });

    await assertion;
  });

  it('passes network errors to the caller', async () => {
    const result = firstValueFrom(api.getCourseOffering(42));
    const assertion = expect(result).rejects.toBeInstanceOf(HttpErrorResponse);
    const statusAssertion = expect(result).rejects.toMatchObject({ status: 0 });

    httpTesting.expectOne('/api/course-offerings/42').error(new ProgressEvent('error'));

    await Promise.all([assertion, statusAssertion]);
  });
});

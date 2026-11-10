import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CourseApi } from './course-api';
import { Course } from './course';

describe('CourseApi', () => {
  let api: CourseApi;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CourseApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('gets courses and preserves their details and response order', async () => {
    const courses: readonly Course[] = [
      { id: 7, code: 'CS101', name: 'Programming Fundamentals' },
      { id: 3, code: 'CS202', name: 'Data Structures' },
    ];
    const result = firstValueFrom(api.getCourses());

    httpTesting.expectOne({ method: 'GET', url: '/api/courses' }).flush(courses);

    expect(await result).toEqual(courses);
  });

  it('returns an empty course list as a successful response', async () => {
    const result = firstValueFrom(api.getCourses());

    httpTesting.expectOne('/api/courses').flush([]);

    expect(await result).toEqual([]);
  });

  it('passes HTTP errors instead of returning an empty list', async () => {
    const body = { detail: 'Course request failed.' };
    const result = firstValueFrom(api.getCourses());
    const assertion = expect(result).rejects.toMatchObject({ status: 500, error: body });

    httpTesting.expectOne('/api/courses').flush(body, {
      status: 500,
      statusText: 'Internal Server Error',
    });

    await assertion;
  });
});

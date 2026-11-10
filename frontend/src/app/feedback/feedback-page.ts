import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, combineLatest, distinctUntilChanged, EMPTY, map, Observable, of, startWith, Subject, switchMap, tap } from 'rxjs';
import { Course } from '../course/course';
import { CourseApi } from '../course/course-api';
import { CourseOffering } from '../course/course-offering';
import { CourseOfferingApi } from '../course/course-offering-api';
import { FeedbackList } from './feedback-list';
import { FeedbackRouteSelection, parseSelectionFromQueryParams } from './feedback-route-selection';
import { FeedbackSort } from './feedback.models';

// Keep the original demo offering as the default, even when newer offerings are added.
const DEFAULT_DEMO_OFFERING_ID = 1;

type OfferingState =
  | { status: 'loading' }
  | { status: 'empty' }
  | { status: 'ready'; offering: CourseOffering }
  | { status: 'error'; message: string };
type CoursesState =
  | { status: 'loading' }
  | { status: 'ready'; courses: readonly Course[] }
  | { status: 'error' };

@Component({
  selector: 'app-feedback-page',
  imports: [MatButtonModule, MatProgressBarModule, MatFormFieldModule, MatSelectModule, FeedbackList],
  templateUrl: './feedback-page.html',
  styleUrl: './feedback-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackPage {
  private readonly courseApi = inject(CourseApi);
  private readonly courseOfferingApi = inject(CourseOfferingApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly reloadCourses = new Subject<void>();
  private readonly reloadOffering = new Subject<void>();
  private offeringsCourseId: number | null = null;

  protected readonly state = signal<OfferingState>({ status: 'loading' });
  protected readonly selectedCourseId = signal<number | null>(null);
  protected readonly offerings = signal<readonly CourseOffering[]>([]);
  protected readonly preferences = signal<{ size: number; sort: FeedbackSort }>({
    size: 20, sort: 'submittedAt,desc',
  });

  protected readonly coursesState = toSignal(
    this.reloadCourses.pipe(
      startWith(null),
      switchMap(() => this.courseApi.getCourses().pipe(
        map((courses): CoursesState => ({ status: 'ready', courses })),
        catchError(() => of<CoursesState>({ status: 'error' })),
        startWith<CoursesState>({ status: 'loading' }),
      )),
    ),
    { requireSync: true },
  );

  constructor() {
    const routeSelection = this.route.queryParamMap.pipe(
      map(parseSelectionFromQueryParams),
      distinctUntilChanged((previous, next) => previous.type === next.type && previous.id === next.id),
    );

    combineLatest({ selection: routeSelection, retry: this.reloadOffering.pipe(startWith(null)) }).pipe(
      switchMap(({ selection }) => this.loadSelection(selection)),
      takeUntilDestroyed(),
    ).subscribe((state) => this.state.set(state));
  }

  protected selectCourse(courseId: number): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { courseId } });
  }

  protected navigateToOffering(offeringId: number, replaceUrl = false): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { offeringId }, replaceUrl });
  }

  protected retry(): void {
    this.offeringsCourseId = null;
    this.reloadOffering.next();
  }

  protected retryCourses(): void {
    this.reloadCourses.next();
  }

  private loadSelection(selection: FeedbackRouteSelection): Observable<OfferingState> {
    let request: Observable<OfferingState>;
    if (selection.type === 'course') {
      request = this.loadDefaultCourseOffering(selection.id);
    } else if (selection.id === null) {
      // Set the default URL first. Its route change will start the only offering request.
      this.navigateToOffering(DEFAULT_DEMO_OFFERING_ID, true);
      return of({ status: 'loading' });
    } else {
      request = this.loadSelectedOffering(selection.id);
    }
    return request.pipe(startWith<OfferingState>({ status: 'loading' }));
  }

  private loadDefaultCourseOffering(courseId: number): Observable<OfferingState> {
    this.selectedCourseId.set(courseId);
    this.offeringsCourseId = null;

    return this.loadCourseOfferings(courseId).pipe(
      switchMap((offerings): Observable<OfferingState> => {
        if (offerings.length === 0) return of({ status: 'empty' });
        // A new course needs an offering selection. Use its first offering;
        // the URL change loads its details and feedback. Replace the temporary
        // course URL so Back returns to the previous offering.
        this.navigateToOffering(offerings[0].id, true);
        return EMPTY;
      }),
      catchError(() => of<OfferingState>({
        status: 'error', message: 'We could not load the course offerings. Please try again.',
      })),
    );
  }

  private loadSelectedOffering(offeringId: number): Observable<OfferingState> {
    if (!Number.isSafeInteger(offeringId) || offeringId <= 0) {
      return of({ status: 'error', message: 'This course offering could not be found.' });
    }

    return this.courseOfferingApi.getCourseOffering(offeringId).pipe(
      switchMap((offering) => {
        this.selectedCourseId.set(offering.courseId);
        return this.loadCourseOfferings(offering.courseId).pipe(
          map((): OfferingState => ({ status: 'ready', offering })),
        );
      }),
      catchError((error: unknown) => of<OfferingState>({
        status: 'error',
        message: error instanceof HttpErrorResponse && error.status === 404
          ? 'This course offering could not be found.'
          : 'We could not load the course details. Please try again.',
      })),
    );
  }

  private loadCourseOfferings(courseId: number): Observable<readonly CourseOffering[]> {
    if (this.offeringsCourseId === courseId) {
      return of(this.offerings());
    }

    this.offerings.set([]);
    this.offeringsCourseId = null;

    return this.courseOfferingApi.getCourseOfferings(courseId).pipe(tap((offerings) => {
      this.offeringsCourseId = courseId;
      this.offerings.set(offerings);
    }));
  }

}

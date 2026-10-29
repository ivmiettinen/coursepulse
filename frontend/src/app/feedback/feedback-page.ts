import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import { CourseOffering } from '../course/course-offering';
import { CourseOfferingApi } from '../course/course-offering-api';

// Load this demo course offering by default; course selection will come later.
const DEMO_COURSE_OFFERING_ID = 1;

type CourseOfferingState =
  | { status: 'loading' }
  | { status: 'ready'; offering: CourseOffering }
  | { status: 'error'; message: string };

@Component({
  selector: 'app-feedback-page',
  imports: [MatButtonModule, MatProgressBarModule],
  templateUrl: './feedback-page.html',
  styleUrl: './feedback-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackPage {
  private readonly courseOfferingApi = inject(CourseOfferingApi);
  private readonly loadCourseOffering = new Subject<void>();

  protected readonly state = toSignal(
    this.loadCourseOffering.pipe(
      // Load when the page opens, then again only on explicit retry.
      startWith(undefined),
      switchMap(() =>
        this.courseOfferingApi.getCourseOffering(DEMO_COURSE_OFFERING_ID).pipe(
          map((offering): CourseOfferingState => ({ status: 'ready', offering })),
          // Keep errors inside the request cycle so a later retry still works.
          catchError((error: unknown) =>
            of<CourseOfferingState>({
              status: 'error',
              message:
                error instanceof HttpErrorResponse && error.status === 404
                  ? 'This course offering could not be found.'
                  : 'We could not load the course details. Please try again.',
            }),
          ),
          startWith<CourseOfferingState>({ status: 'loading' }),
        ),
      ),
    ),
    // startWith emits the loading state immediately, before the HTTP response.
    { requireSync: true },
  );

  protected retry(): void {
    this.loadCourseOffering.next();
  }
}

import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { catchError, combineLatest, map, of, startWith, Subject, switchMap } from 'rxjs';
import { FeedbackApi } from './feedback-api';
import { FeedbackPageResponse } from './feedback.models';

type FeedbackState =
  | { status: 'loading' }
  | { status: 'ready'; page: FeedbackPageResponse }
  | { status: 'error'; message: string };

@Component({
  selector: 'app-feedback-list',
  imports: [DatePipe, MatButtonModule, MatProgressBarModule],
  templateUrl: './feedback-list.html',
  styleUrl: './feedback-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackList {
  readonly courseOfferingId = input.required<number>();

  private readonly feedbackApi = inject(FeedbackApi);
  private readonly reload = new Subject<void>();

  // The parent supplies the offering ID; retries affect only this list.
  protected readonly state = toSignal(
    combineLatest([
      toObservable(this.courseOfferingId),
      this.reload.pipe(startWith(null)),
    ]).pipe(
      switchMap(([id]) =>
        this.feedbackApi.getFeedback(id).pipe(
          map((page): FeedbackState => ({ status: 'ready', page })),
          catchError((error: unknown) =>
            of<FeedbackState>({
              status: 'error',
              message:
                error instanceof HttpErrorResponse && error.status === 404
                  ? 'This course offering could not be found.'
                  : 'We could not load the feedback. Please try again.',
            }),
          ),
          startWith<FeedbackState>({ status: 'loading' }),
        ),
      ),
    ),
    // Input signals become observable values asynchronously. Show loading meanwhile.
    { initialValue: { status: 'loading' } as FeedbackState },
  );

  protected retry(): void {
    this.reload.next();
  }
}

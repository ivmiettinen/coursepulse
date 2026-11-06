import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, Subject, switchMap, tap } from 'rxjs';
import { FeedbackApi } from './feedback-api';
import { FeedbackPageResponse } from './feedback.models';

type FeedbackState =
  | { status: 'loading' }
  | { status: 'ready'; page: FeedbackPageResponse }
  | { status: 'error'; message: string };

@Component({
  selector: 'app-feedback-list',
  imports: [DatePipe, MatButtonModule, MatProgressBarModule, MatPaginatorModule],
  templateUrl: './feedback-list.html',
  styleUrl: './feedback-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackList {
  readonly courseOfferingId = input.required<number>();

  private readonly feedbackApi = inject(FeedbackApi);
  private readonly reload = new Subject<void>();
  // Backend page indexes start at 0, so page 0 is the first page.
  private readonly pagination = new BehaviorSubject({ page: 0, size: 20 });
  protected readonly selectedPagination = toSignal(this.pagination, { requireSync: true });
  protected readonly feedbackTotalCount = signal(0);

  // The parent supplies the offering ID; retries affect only this list.
  protected readonly state = toSignal(
    combineLatest({
      id: toObservable(this.courseOfferingId),
      retry: this.reload.pipe(startWith(null)),
      pagination: this.pagination,
    }).pipe(
      switchMap(({ id, pagination }) =>
        this.feedbackApi.getFeedback(id, pagination.page, pagination.size).pipe(
          tap((page) => this.feedbackTotalCount.set(page.totalElements)),
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

  protected changePage(event: PageEvent): void {
    this.pagination.next({
      page: event.pageSize === this.selectedPagination().size ? event.pageIndex : 0,
      size: event.pageSize,
    });
  }
}

import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BehaviorSubject, catchError, combineLatest, distinctUntilChanged, map, of, startWith, Subject, switchMap, tap } from 'rxjs';
import { FeedbackApi } from './feedback-api';
import { FeedbackFilterBar } from './feedback-filter-bar';
import { FeedbackFilters, FeedbackPageResponse, FeedbackQuery, FeedbackSort, feedbackSortLabels } from './feedback.models';

type FeedbackState =
  | { status: 'loading' }
  | { status: 'ready'; page: FeedbackPageResponse }
  | { status: 'error'; message: string };

@Component({
  selector: 'app-feedback-list',
  imports: [DatePipe, MatButtonModule, MatProgressBarModule, MatPaginatorModule, FeedbackFilterBar],
  templateUrl: './feedback-list.html',
  styleUrl: './feedback-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackList implements OnInit {
  readonly initialPreferences = input<{ size: number; sort: FeedbackSort }>({ size: 20, sort: 'submittedAt,desc' });
  readonly preferencesChange = output<{ size: number; sort: FeedbackSort }>();
  readonly courseOfferingId = input.required<number>();

  private readonly feedbackApi = inject(FeedbackApi);
  private readonly reload = new Subject<void>();
  // Backend page indexes start at 0, so page 0 is the first page.
  private readonly feedbackQuery = new BehaviorSubject<Required<FeedbackQuery>>({
    page: 0, size: 20, search: '', rating: null, sort: 'submittedAt,desc',
  });
  protected readonly selectedFeedbackQuery = toSignal(this.feedbackQuery, { requireSync: true });
  protected readonly feedbackTotalCount = signal(0);
  protected readonly sortLabels = feedbackSortLabels;
  protected readonly displayedResult = signal<{
    page: FeedbackPageResponse; isFiltered: boolean; sort: FeedbackSort;
  } | null>(null);

  // The parent supplies the offering ID; retries affect only this list.
  protected readonly state = toSignal(
    combineLatest({
      id: toObservable(this.courseOfferingId),
      retry: this.reload.pipe(startWith(null)),
      feedbackQuery: this.feedbackQuery.pipe(distinctUntilChanged(FeedbackList.isSameFeedbackQuery)),
    }).pipe(
      switchMap(({ id, feedbackQuery }) =>
        this.feedbackApi.getFeedback(id, feedbackQuery).pipe(
          tap((page) => {
            const isFiltered = feedbackQuery.search.trim().length > 0
              || feedbackQuery.rating !== null;
            this.feedbackTotalCount.set(page.totalElements);
            this.displayedResult.set({ page, isFiltered, sort: feedbackQuery.sort });
          }),
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

  ngOnInit(): void {
    this.feedbackQuery.next({ ...this.selectedFeedbackQuery(), ...this.initialPreferences() });
  }

  protected retry(): void {
    this.reload.next();
  }

  protected changePage(event: PageEvent): void {
    this.preferencesChange.emit({ size: event.pageSize, sort: this.selectedFeedbackQuery().sort });
    this.feedbackQuery.next({
      ...this.selectedFeedbackQuery(),
      page: event.pageSize === this.selectedFeedbackQuery().size ? event.pageIndex : 0,
      size: event.pageSize,
    });
  }

  protected applyFilters(filters: FeedbackFilters): void {
    this.feedbackQuery.next({ ...this.selectedFeedbackQuery(), ...filters, page: 0 });
  }

  protected changeSort(sort: FeedbackSort): void {
    this.preferencesChange.emit({ size: this.selectedFeedbackQuery().size, sort });
    this.feedbackQuery.next({ ...this.selectedFeedbackQuery(), sort, page: 0 });
  }

  // Compare every request parameter so repeated Search clicks skip the HTTP request.
  // The separate retry stream can still repeat the same query after an error.
  private static isSameFeedbackQuery(previous: Required<FeedbackQuery>, current: Required<FeedbackQuery>): boolean {
    return previous.page === current.page
      && previous.size === current.size
      && previous.search === current.search
      && previous.rating === current.rating
      && previous.sort === current.sort;
  }
}

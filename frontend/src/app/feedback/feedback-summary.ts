import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, inject, input, viewChild } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { catchError, combineLatest, map, of, startWith, Subject, switchMap } from 'rxjs';
import { FeedbackApi } from './feedback-api';
import { FeedbackSummaryResponse } from './feedback.models';

type SummaryState =
  | { status: 'loading' }
  | { status: 'ready'; summary: FeedbackSummaryResponse }
  | { status: 'error' };

@Component({
  selector: 'app-feedback-summary',
  imports: [DecimalPipe, MatButtonModule],
  templateUrl: './feedback-summary.html',
  styleUrl: './feedback-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackSummary {
  readonly courseOfferingId = input.required<number>();
  private readonly feedbackApi = inject(FeedbackApi);
  private readonly reload = new Subject<void>();
  private readonly summaryTitle = viewChild.required<ElementRef<HTMLHeadingElement>>('summaryTitle');

  // Both an offering change and a retry load the latest offering's summary.
  // Seed retry so the first load does not wait for a button click.
  protected readonly state = toSignal(
    combineLatest({
      id: toObservable(this.courseOfferingId),
      retry: this.reload.pipe(startWith(null)),
    }).pipe(
      switchMap(({ id }) => this.feedbackApi.getFeedbackSummary(id).pipe(
        map((summary): SummaryState => ({ status: 'ready', summary })),
        catchError(() => of<SummaryState>({ status: 'error' })),
        startWith<SummaryState>({ status: 'loading' }),
      )),
    ),
    { initialValue: { status: 'loading' } as SummaryState },
  );

  protected retry(): void {
    // Keep keyboard focus in the summary when the retry button disappears.
    this.summaryTitle().nativeElement.focus();
    this.reload.next();
  }
}

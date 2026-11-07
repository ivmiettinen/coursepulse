import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FeedbackFilters, FeedbackRating, FeedbackSort, feedbackSortLabels } from './feedback.models';

@Component({
  selector: 'app-feedback-filter-bar',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './feedback-filter-bar.html',
  styleUrl: './feedback-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackFilterBar {
  readonly filtersChange = output<FeedbackFilters>();
  readonly sortChange = output<FeedbackSort>();
  protected readonly sortLabels = feedbackSortLabels;
  protected readonly sorts: readonly FeedbackSort[] = [
    'submittedAt,desc', 'submittedAt,asc', 'rating,desc', 'rating,asc',
  ];
  protected readonly sort = new FormControl<FeedbackSort>('submittedAt,desc', { nonNullable: true });
  protected readonly ratings: readonly FeedbackRating[] = [1, 2, 3, 4, 5];
  protected readonly filters = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    rating: new FormControl<FeedbackRating | null>(null),
  });

  constructor() {
    this.sort.valueChanges.pipe(takeUntilDestroyed()).subscribe((sort) => {
      this.sortChange.emit(sort);
    });
    this.filters.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.filtersChange.emit(this.filters.getRawValue());
    });
  }
}

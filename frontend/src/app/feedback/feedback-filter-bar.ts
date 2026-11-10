import { ChangeDetectionStrategy, Component, input, OnInit, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { FeedbackFilters, FeedbackRating, FeedbackSort, feedbackSortLabels } from './feedback.models';

@Component({
  selector: 'app-feedback-filter-bar',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  templateUrl: './feedback-filter-bar.html',
  styleUrl: './feedback-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackFilterBar implements OnInit {
  readonly initialSort = input<FeedbackSort>('submittedAt,desc');
  private appliedSearch = '';
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

  ngOnInit(): void {
    this.sort.setValue(this.initialSort(), { emitEvent: false });
  }

  constructor() {
    this.sort.valueChanges.pipe(takeUntilDestroyed()).subscribe((sort) => {
      this.sortChange.emit(sort);
    });
    this.filters.controls.rating.valueChanges.pipe(takeUntilDestroyed()).subscribe((rating) => {
      this.filtersChange.emit({ search: this.appliedSearch, rating });
    });
  }

  
  protected clearFilters(): void {
    this.appliedSearch = '';
    this.filters.reset({ search: '', rating: null }, { emitEvent: false });
    this.filtersChange.emit({ search: '', rating: null });
  }

  protected submitSearch(): void {
    this.appliedSearch = this.filters.controls.search.value.trim();
    this.filtersChange.emit({ search: this.appliedSearch, rating: this.filters.controls.rating.value });
  }
}

// Matches the backend's FeedbackPageDto response.
export interface FeedbackPageResponse {
  readonly content: readonly Feedback[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

// Matches the backend's FeedbackDto response.
export interface Feedback {
  readonly id: number;
  readonly rating: number;
  readonly comment: string | null;
  // JSON represents the backend's Instant as an ISO 8601 string
  readonly submittedAt: string;
}

export type FeedbackRating = 1 | 2 | 3 | 4 | 5;

export type FeedbackSort = 'submittedAt,desc' | 'submittedAt,asc' | 'rating,desc' | 'rating,asc';

export const feedbackSortLabels: Record<FeedbackSort, string> = {
  'submittedAt,desc': 'Newest first',
  'submittedAt,asc': 'Oldest first',
  'rating,desc': 'Highest rating first',
  'rating,asc': 'Lowest rating first',
};

export interface FeedbackFilters {
  readonly search: string;
  readonly rating: FeedbackRating | null;
}

export interface FeedbackQuery {
  readonly sort?: FeedbackSort;
  readonly page?: number;
  readonly size?: number;
  readonly search?: string;
  readonly rating?: FeedbackRating | null;
}

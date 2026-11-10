import { ParamMap } from '@angular/router';

export type FeedbackRouteSelection =
  | { type: 'offering'; id: number | null }
  | { type: 'course'; id: number };

export function parseSelectionFromQueryParams(params: ParamMap): FeedbackRouteSelection {
  // If the URL contains both offeringId and courseId, use offeringId and ignore courseId.
  if (params.has('offeringId')) {
    return { type: 'offering', id: Number(params.get('offeringId')) };
  }
  if (params.has('courseId')) {
    return { type: 'course', id: Number(params.get('courseId')) };
  }
  // Opening /feedback without either ID asks the page to load its default demo offering.
  return { type: 'offering', id: null };
}

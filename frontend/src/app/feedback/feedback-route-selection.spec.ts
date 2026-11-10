import { convertToParamMap } from '@angular/router';
import { parseSelectionFromQueryParams } from './feedback-route-selection';

describe('Feedback route selection', () => {
  it('requests the default offering when neither ID is present', () => {
    expect(parseSelectionFromQueryParams(convertToParamMap({})))
      .toEqual({ type: 'offering', id: null });
  });

  it('reads the selected course ID as a number', () => {
    expect(parseSelectionFromQueryParams(convertToParamMap({ courseId: '2' })))
      .toEqual({ type: 'course', id: 2 });
  });

  it('uses the offering ID even when the URL also contains a different course ID', () => {
    expect(parseSelectionFromQueryParams(convertToParamMap({ courseId: '2', offeringId: '4' })))
      .toEqual({ type: 'offering', id: 4 });
  });
});

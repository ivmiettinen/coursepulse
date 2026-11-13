import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FeedbackSummary } from './feedback-summary';
import { FeedbackSummaryResponse } from './feedback.models';

describe('FeedbackSummary', () => {
  const summary: FeedbackSummaryResponse = {
    totalFeedback: 3, averageRating: 14 / 3,
    ratingDistribution: [
      { rating: 1, count: 0 }, { rating: 2, count: 0 }, { rating: 3, count: 0 },
      { rating: 4, count: 1 }, { rating: 5, count: 2 },
    ],
  };
  const url = '/api/course-offerings/1/feedback-summary';
  let fixture: ComponentFixture<FeedbackSummary>;
  let element: HTMLElement;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [FeedbackSummary],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FeedbackSummary);
    element = fixture.nativeElement as HTMLElement;
    fixture.componentRef.setInput('courseOfferingId', 1);
    await fixture.whenStable();
  });

  afterEach(() => httpTesting.verify());

  it('shows the average, total and descending distribution with proportional bars', async () => {
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading');
    httpTesting.expectOne(url).flush(summary);
    await fixture.whenStable();

    expect(Array.from(element.querySelectorAll('dd'), (item) => item.textContent)).toEqual(['3', '4.7 / 5']);
    const rows = element.querySelectorAll('li');
    expect(Array.from(rows, (row) => row.firstElementChild?.textContent)).toEqual([
      '5 / 5', '4 / 5', '3 / 5', '2 / 5', '1 / 5',
    ]);
    expect(rows[0].querySelector('.count')?.textContent).toContain('2');
    expect(parseFloat(rows[0].querySelector<HTMLElement>('.bar')!.style.width)).toBeCloseTo(200 / 3);
    expect(element.querySelector('[aria-busy="true"]')).toBeNull();
  });

  it('shows no feedback without presenting a null average as zero', async () => {
    httpTesting.expectOne(url).flush({
      totalFeedback: 0, averageRating: null,
      ratingDistribution: summary.ratingDistribution.map((item) => ({ ...item, count: 0 })),
    });
    await fixture.whenStable();
    expect(element.querySelector('[role="status"]')?.textContent).toBe('No feedback yet.');
    expect(Array.from(element.querySelectorAll('dd'), (item) => item.textContent)).toEqual(['0', 'Not available']);
    expect(Array.from(element.querySelectorAll<HTMLElement>('.bar'), (bar) => bar.style.width))
      .toEqual(['0%', '0%', '0%', '0%', '0%']);
  });

  it('allows repeated retries after errors and then displays the summary', async () => {
    for (let attempt = 0; attempt < 2; attempt++) {
      httpTesting.expectOne(url).flush(null, { status: 503, statusText: 'Service Unavailable' });
      await fixture.whenStable();
      expect(element.querySelector('[role="alert"]')?.textContent).toContain('could not load');
      element.querySelector<HTMLButtonElement>('button')!.click();
      await fixture.whenStable();
      expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading');
    }
    httpTesting.expectOne(url).flush(summary);
    await fixture.whenStable();
    expect(element.textContent).toContain('4.7 / 5');
  });

  it('clears old values and cancels requests when the offering changes or the component is destroyed', async () => {
    httpTesting.expectOne(url).flush(summary);
    await fixture.whenStable();
    fixture.componentRef.setInput('courseOfferingId', 2);
    await fixture.whenStable();
    expect(element.querySelector('dd')).toBeNull();
    const previous = httpTesting.expectOne('/api/course-offerings/2/feedback-summary');
    fixture.componentRef.setInput('courseOfferingId', 3);
    await fixture.whenStable();
    expect(previous.cancelled).toBe(true);
    const current = httpTesting.expectOne('/api/course-offerings/3/feedback-summary');
    fixture.destroy();
    expect(current.cancelled).toBe(true);
  });
});

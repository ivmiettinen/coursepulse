import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('moves focus to the main content without navigating when the skip link is activated', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const click = new MouseEvent('click', { bubbles: true, cancelable: true });
    element.querySelector<HTMLAnchorElement>('.skip-link')!.dispatchEvent(click);
    expect(click.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(element.querySelector('main'));
  });
});

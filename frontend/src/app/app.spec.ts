import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  it('renders the comic container', async () => {
    TestBed.configureTestingModule({ imports: [App], providers: [provideHttpClient()] });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('app-comic-container')).toBeTruthy();
  });
});

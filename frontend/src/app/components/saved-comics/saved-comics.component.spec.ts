import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SavedComicSummary } from '../../models/comic.models';
import { SavedComicsComponent } from './saved-comics.component';

const COMICS: SavedComicSummary[] = [
  { id: 'a', title: 'The Map', panelCount: 1, createdAt: '', updatedAt: '2026-11-11T10:00:00Z' },
  { id: 'b', title: 'The River', panelCount: 6, createdAt: '', updatedAt: '2026-11-10T10:00:00Z' },
];

describe('SavedComicsComponent', () => {
  let fixture: ComponentFixture<SavedComicsComponent>;
  let http: HttpTestingController;
  let root: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [SavedComicsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(SavedComicsComponent);
    http = TestBed.inject(HttpTestingController);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  const buttons = (label: string) =>
    Array.from(root.querySelectorAll('button')).filter((b) => b.textContent!.trim().startsWith(label));

  async function load(comics = COMICS) {
    http.expectOne('/api/saved-comics').flush(comics);
    await fixture.whenStable();
  }

  it('lists saved comics with their size', async () => {
    await load();

    const items = Array.from(root.querySelectorAll('li'));
    expect(items.map((li) => li.querySelector('.title')!.textContent)).toEqual(['The Map', 'The River']);
    expect(items[0].textContent).toContain('1 panel,');
    expect(items[1].textContent).toContain('6 panels');
  });

  it('says so when nothing is saved yet', async () => {
    await load([]);
    expect(root.textContent).toContain('No saved comics yet');
  });

  it('asks before deleting, and can be told to keep it', async () => {
    await load();

    buttons('Delete')[1].click();
    await fixture.whenStable();
    expect(buttons('Delete "The River" for good').length).toBe(1);
    buttons('Keep it')[0].click();
    await fixture.whenStable();
    expect(buttons('Delete "The River" for good').length).toBe(0);

    buttons('Delete')[1].click();
    await fixture.whenStable();
    buttons('Delete "The River" for good')[0].click();
    const request = http.expectOne('/api/saved-comics/b');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
    await fixture.whenStable();

    expect(root.querySelectorAll('li').length).toBe(1);
  });

  it('shows the server’s message when the list cannot load', async () => {
    http
      .expectOne('/api/saved-comics')
      .flush({ detail: 'The database is not reachable.' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();

    expect(root.querySelector('[role="alert"]')!.textContent).toContain('The database is not reachable.');
  });
});

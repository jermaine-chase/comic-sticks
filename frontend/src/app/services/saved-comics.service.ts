import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ComicOptions, ComicStory, SavedComic, SavedComicSummary } from '../models/comic.models';

const API_URL = '/api/saved-comics';

/** Saves comics to the server's database, lists them, opens them and deletes them. */
@Injectable({ providedIn: 'root' })
export class SavedComicsService {
  private readonly http = inject(HttpClient);

  list(): Observable<SavedComicSummary[]> {
    return this.http.get<SavedComicSummary[]>(API_URL);
  }

  get(id: string): Observable<SavedComic> {
    return this.http.get<SavedComic>(`${API_URL}/${encodeURIComponent(id)}`);
  }

  create(comic: ComicStory, options: ComicOptions): Observable<SavedComicSummary> {
    return this.http.post<SavedComicSummary>(API_URL, { comic, options });
  }

  update(id: string, comic: ComicStory, options: ComicOptions): Observable<SavedComicSummary> {
    return this.http.put<SavedComicSummary>(`${API_URL}/${encodeURIComponent(id)}`, { comic, options });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${encodeURIComponent(id)}`);
  }
}

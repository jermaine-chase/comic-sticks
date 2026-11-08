import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CharacterSheet, ComicEstimate, ComicOptions, ComicStory } from '../models/comic.models';

const API_URL = '/api/comics';

/** Must stay in step with spring.servlet.multipart.max-file-size in application.properties. */
export const MAX_FILE_BYTES = 10 * 1024 * 1024;

/** Story text, or an uploaded .txt or .pdf file. */
export type StorySource = string | File;

@Injectable({ providedIn: 'root' })
export class ComicApiService {
  private readonly http = inject(HttpClient);

  /** Predicts the run time. This makes no LLM calls, so it returns straight away. */
  estimate(
    story: StorySource,
    options: ComicOptions,
    sheet: CharacterSheet | null = null,
  ): Observable<ComicEstimate> {
    return this.http.post<ComicEstimate>(`${API_URL}/estimate`, requestBody(story, options, sheet));
  }

  /** Runs the character pass, so the sheet can be reviewed before drawing. */
  characters(story: StorySource): Observable<CharacterSheet> {
    return this.http.post<CharacterSheet>(`${API_URL}/characters`, requestBody(story, null, null));
  }

  generate(
    story: StorySource,
    options: ComicOptions,
    sheet: CharacterSheet | null = null,
  ): Observable<ComicStory> {
    return this.http.post<ComicStory>(`${API_URL}/generate`, requestBody(story, options, sheet));
  }
}

function requestBody(
  story: StorySource,
  options: ComicOptions | null,
  sheet: CharacterSheet | null,
): FormData | object {
  if (typeof story === 'string') {
    return { text: story, ...options, ...(sheet ? { characterSheet: sheet } : {}) };
  }
  const form = new FormData();
  form.append('file', story);
  for (const [name, value] of Object.entries(options ?? {})) {
    form.append(name, String(value));
  }
  if (sheet) {
    form.append('characterSheet', JSON.stringify(sheet));
  }
  return form;
}

/** The backend sends RFC 9457 problem documents, whose `detail` is written for the user. */
export function errorMessage(error: HttpErrorResponse): string {
  return error.error?.detail ?? 'Could not reach the comic server. Is the backend running?';
}

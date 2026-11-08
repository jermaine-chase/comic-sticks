import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ComicEstimate, ComicOptions, ComicStory } from '../models/comic.models';

const API_URL = '/api/comics';

/** Must stay in step with spring.servlet.multipart.max-file-size in application.properties. */
export const MAX_FILE_BYTES = 10 * 1024 * 1024;

/** Story text, or an uploaded .txt or .pdf file. */
export type StorySource = string | File;

@Injectable({ providedIn: 'root' })
export class ComicApiService {
  private readonly http = inject(HttpClient);

  /** Predicts the run time. This makes no LLM calls, so it returns straight away. */
  estimate(story: StorySource, options: ComicOptions): Observable<ComicEstimate> {
    return this.http.post<ComicEstimate>(`${API_URL}/estimate`, requestBody(story, options));
  }

  generate(story: StorySource, options: ComicOptions): Observable<ComicStory> {
    return this.http.post<ComicStory>(`${API_URL}/generate`, requestBody(story, options));
  }
}

function requestBody(story: StorySource, options: ComicOptions): FormData | object {
  if (typeof story === 'string') {
    return { text: story, ...options };
  }
  const form = new FormData();
  form.append('file', story);
  for (const [name, value] of Object.entries(options)) {
    form.append(name, String(value));
  }
  return form;
}

/** The backend sends RFC 9457 problem documents, whose `detail` is written for the user. */
export function errorMessage(error: HttpErrorResponse): string {
  return error.error?.detail ?? 'Could not reach the comic server. Is the backend running?';
}

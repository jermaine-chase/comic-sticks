import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ComicEstimate, ComicStory } from '../models/comic.models';

const API_URL = '/api/comics';

/** Must stay in step with spring.servlet.multipart.max-file-size in application.properties. */
export const MAX_FILE_BYTES = 10 * 1024 * 1024;

/** Story text, or an uploaded .txt or .pdf file. */
export type StorySource = string | File;

@Injectable({ providedIn: 'root' })
export class ComicApiService {
  private readonly http = inject(HttpClient);

  /** Predicts the run time. This makes no LLM calls, so it returns straight away. */
  estimate(story: StorySource): Observable<ComicEstimate> {
    return this.http.post<ComicEstimate>(`${API_URL}/estimate`, requestBody(story));
  }

  generate(story: StorySource): Observable<ComicStory> {
    return this.http.post<ComicStory>(`${API_URL}/generate`, requestBody(story));
  }
}

function requestBody(story: StorySource): FormData | { text: string } {
  if (typeof story === 'string') {
    return { text: story };
  }
  const form = new FormData();
  form.append('file', story);
  return form;
}

/** The backend sends RFC 9457 problem documents, whose `detail` is written for the user. */
export function errorMessage(error: HttpErrorResponse): string {
  return error.error?.detail ?? 'Could not reach the comic server. Is the backend running?';
}

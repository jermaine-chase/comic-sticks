import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SavedComic, SavedComicSummary } from '../../models/comic.models';
import { errorMessage } from '../../services/comic-api.service';
import { SavedComicsService } from '../../services/saved-comics.service';

/**
 * "My comics": every saved comic, most recently saved first. Opening one loads it exactly as it was
 * saved. Deleting needs a second click on a clearly labelled button, so nothing goes by accident.
 */
@Component({
  selector: 'app-saved-comics',
  styles: `
    :host {
      display: block;
      --ink: #1b1b1f;
      font-family: 'Atkinson Hyperlegible', 'Verdana', sans-serif;
    }
    h2 {
      margin: 0 0 0.6rem;
      font: 400 1.8rem/1.1 'Bangers', 'Impact', sans-serif;
      letter-spacing: 0.04em;
    }
    ul {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }
    li {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem 1rem;
      align-items: center;
      justify-content: space-between;
      padding: 0.6rem 0.8rem;
      background: #fff;
      border: 2px solid var(--ink);
      border-radius: 0.4rem;
    }
    .title {
      margin: 0;
      font-size: 1.15rem;
      font-weight: 700;
    }
    .meta {
      margin: 0.1rem 0 0;
      color: #5d5a52;
      font-size: 0.95rem;
    }
    .actions {
      display: flex;
      gap: 0.5rem;
    }
    button {
      padding: 0.4rem 0.9rem;
      font: 700 1rem 'Atkinson Hyperlegible', 'Verdana', sans-serif;
      background: #ffd23f;
      border: 2px solid var(--ink);
      border-radius: 0.3rem;
      cursor: pointer;
    }
    button.quiet {
      background: #fff;
    }
    button.danger {
      color: #fff;
      background: #c8321f;
    }
    .note {
      margin: 0;
      font-size: 1.05rem;
    }
    .alert {
      color: #8b1e12;
      font-weight: 700;
    }
  `,
  template: `
    <section aria-labelledby="saved-comics-title">
      <h2 id="saved-comics-title">My comics</h2>
      @if (error(); as message) {
        <p class="note alert" role="alert">{{ message }}</p>
      }
      @if (loading()) {
        <p class="note">Loading your comics...</p>
      } @else if (!comics().length && !error()) {
        <p class="note">No saved comics yet. Make a comic, then press "Save this comic".</p>
      } @else {
        <ul>
          @for (comic of comics(); track comic.id) {
            <li [attr.data-id]="comic.id">
              <div>
                <p class="title">{{ comic.title }}</p>
                <p class="meta">
                  {{ comic.panelCount }} {{ comic.panelCount === 1 ? 'panel' : 'panels' }}, saved
                  {{ when(comic.updatedAt) }}
                </p>
              </div>
              <div class="actions">
                @if (confirming() === comic.id) {
                  <button type="button" class="danger" (click)="remove(comic)">
                    Delete "{{ comic.title }}" for good
                  </button>
                  <button type="button" class="quiet" (click)="confirming.set(null)">Keep it</button>
                } @else {
                  <button type="button" [disabled]="opening()" (click)="openComic(comic)">Open</button>
                  <button type="button" class="quiet" (click)="confirming.set(comic.id)">Delete</button>
                }
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class SavedComicsComponent implements OnInit {
  private readonly api = inject(SavedComicsService);
  private readonly destroyRef = inject(DestroyRef);

  /** A saved comic, loaded in full, ready to show. */
  readonly open = output<SavedComic>();

  protected readonly comics = signal<SavedComicSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly opening = signal(false);
  protected readonly error = signal<string | null>(null);
  /** The comic whose Delete was pressed once and now asks to be sure. */
  protected readonly confirming = signal<string | null>(null);

  ngOnInit(): void {
    this.api
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (comics) => {
          this.comics.set(comics);
          this.loading.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
  }

  protected openComic(comic: SavedComicSummary): void {
    this.opening.set(true);
    this.error.set(null);
    this.api
      .get(comic.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.opening.set(false);
          this.open.emit(saved);
        },
        error: (e: HttpErrorResponse) => {
          this.opening.set(false);
          this.error.set(errorMessage(e));
        },
      });
  }

  protected remove(comic: SavedComicSummary): void {
    this.error.set(null);
    this.api
      .delete(comic.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.comics.update((list) => list.filter((c) => c.id !== comic.id));
          this.confirming.set(null);
        },
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  protected when(iso: string): string {
    const date = new Date(iso);
    return isNaN(date.getTime())
      ? ''
      : date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
  }
}

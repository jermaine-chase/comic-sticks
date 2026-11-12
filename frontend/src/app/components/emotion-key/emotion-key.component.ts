import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';
import { EXPRESSIONS, Expression } from '../../models/comic.models';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';

/**
 * What each face means, in plain words, and the clues on the face that show it. Every expression
 * in EXPRESSIONS needs a line here; the spec checks that.
 */
export const EMOTION_CLUES: Record<Expression, { meaning: string; clues: string }> = {
  neutral: { meaning: 'Calm. Not feeling anything strongly.', clues: 'Straight mouth.' },
  happy: { meaning: 'Feels good.', clues: 'Curved, smiling eyes and a big open smile.' },
  sad: {
    meaning: 'Feels unhappy.',
    clues: 'Mouth turned down, eyebrows sloping down at the sides, and a tear.',
  },
  angry: {
    meaning: 'Feels cross or mad.',
    clues: 'Eyebrows pulled down in the middle, teeth showing, and lines beside the head.',
  },
  surprised: {
    meaning: 'Did not expect what happened.',
    clues: 'Wide eyes, raised eyebrows, a round open mouth, and "!".',
  },
  scared: {
    meaning: 'Feels afraid.',
    clues: 'Wide eyes, a wobbly mouth, a drop of sweat, and shaking marks.',
  },
  confused: {
    meaning: 'Does not understand something.',
    clues: 'One eyebrow higher than the other, a wiggly mouth, and "?".',
  },
  worried: {
    meaning: 'Thinks something bad might happen.',
    clues: 'Eyebrows sloping down at the sides, a wobbly mouth, and a drop of sweat.',
  },
  excited: {
    meaning: 'Very happy and can hardly wait.',
    clues: 'Wide eyes, a huge open smile, lines around the head, and "!".',
  },
  proud: {
    meaning: 'Feels good about something they did.',
    clues: 'Eyes closed, a gentle smile, and small lines above the head.',
  },
  embarrassed: {
    meaning: 'Feels silly or shy in front of others.',
    clues: 'Pink cheeks, a small wobbly mouth, and a drop of sweat.',
  },
  determined: {
    meaning: 'Has decided to do something and will not give up.',
    clues: 'Eyebrows pulled down, a firm straight mouth, and lines beside the head.',
  },
  tired: { meaning: 'Wants to rest or sleep.', clues: 'Half-closed eyes, a small round mouth, and "z".' },
};

/**
 * The emotion key: every face the comic can draw, with its word, what it means and the clues that
 * show it. Faces in this comic come first, in the order they first appear. The faces are drawn by
 * the same stick figure as the panels, so they always match.
 */
@Component({
  selector: 'app-emotion-key',
  imports: [StickFigureComponent],
  host: { '(document:keydown.escape)': 'close.emit()', '(click)': 'close.emit()' },
  styles: `
    :host {
      --ink: #1b1b1f;
      position: fixed;
      inset: 0;
      z-index: 50;
      display: grid;
      place-items: center;
      padding: 1rem;
      background: #1b1b1f99;
    }
    .key {
      width: min(100%, 52rem);
      max-height: 100%;
      overflow-y: auto;
      padding: 1rem 1.2rem 1.4rem;
      background: #fffdf6;
      border: 4px solid var(--ink);
      border-radius: 0.6rem;
    }
    header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    h2 {
      margin: 0;
      font: 400 2rem/1.1 'Bangers', 'Impact', sans-serif;
      letter-spacing: 0.04em;
    }
    .close {
      padding: 0.5rem 1rem;
      font: 700 1.1rem 'Atkinson Hyperlegible', 'Verdana', sans-serif;
      background: #ffd23f;
      border: 3px solid var(--ink);
      border-radius: 0.4rem;
      cursor: pointer;
    }
    h3 {
      margin: 1.1rem 0 0.5rem;
      font: 700 1.1rem 'Atkinson Hyperlegible', 'Verdana', sans-serif;
    }
    ul {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr));
      gap: 0.7rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }
    li {
      display: grid;
      grid-template-columns: 5.5rem 1fr;
      gap: 0.7rem;
      align-items: center;
      padding: 0.5rem;
      background: #fff;
      border: 2px solid var(--ink);
      border-radius: 0.5rem;
    }
    li.in-comic {
      background: #ece6ff;
      border-color: #6b55b8;
    }
    app-stick-figure {
      width: 5.5rem;
      aspect-ratio: 1;
    }
    .word {
      margin: 0;
      font: 700 1.25rem/1.2 'Atkinson Hyperlegible', 'Verdana', sans-serif;
    }
    .meaning,
    .clues {
      margin: 0.15rem 0 0;
      font: 0.95rem/1.35 'Atkinson Hyperlegible', 'Verdana', sans-serif;
    }
    .clues {
      color: #4a4740;
    }
  `,
  template: `
    <section
      class="key"
      role="dialog"
      aria-modal="true"
      aria-labelledby="emotion-key-title"
      (click)="$event.stopPropagation()"
    >
      <header>
        <h2 id="emotion-key-title">Emotion key</h2>
        <button #closeButton type="button" class="close" (click)="close.emit()">Close</button>
      </header>
      @for (group of groups(); track group.title) {
        @if (group.title) {
          <h3>{{ group.title }}</h3>
        }
        <ul>
          @for (entry of group.entries; track entry.expression) {
            <li [class.in-comic]="group.inComic" [attr.data-expression]="entry.expression">
              <app-stick-figure
                crop="head"
                [expression]="entry.expression"
                [name]="'A ' + entry.expression + ' face'"
              />
              <div>
                <p class="word">{{ entry.expression }}</p>
                <p class="meaning">{{ entry.meaning }}</p>
                <p class="clues">{{ entry.clues }}</p>
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class EmotionKeyComponent {
  /** The expressions this comic uses, in the order they first appear. */
  readonly used = input<Expression[]>([]);
  readonly close = output<void>();

  /** Faces in this comic first, then the rest. With none in the comic, one untitled list. */
  protected readonly groups = computed(() => {
    const used = [...new Set(this.used().filter((e) => EXPRESSIONS.includes(e)))];
    const others = EXPRESSIONS.filter((e) => !used.includes(e));
    const entries = (list: Expression[]) =>
      list.map((expression) => ({ expression, ...EMOTION_CLUES[expression] }));
    if (!used.length) {
      return [{ title: '', inComic: false, entries: entries(others) }];
    }
    return [
      { title: 'Faces in this comic', inComic: true, entries: entries(used) },
      ...(others.length
        ? [{ title: 'Other faces', inComic: false, entries: entries(others) }]
        : []),
    ];
  });

  private readonly closeButton = viewChild.required<ElementRef<HTMLButtonElement>>('closeButton');

  constructor() {
    afterNextRender(() => this.closeButton().nativeElement.focus());
  }
}

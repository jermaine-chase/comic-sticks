import {
  afterRenderEffect,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import {
  CharacterDesign,
  ComicCharacter,
  EXPRESSIONS,
  Panel,
  Place,
  POSES,
} from '../../models/comic.models';
import { PanelBackgroundComponent } from './panel-background.component';
import { EditableTextDirective } from '../../directives/editable-text.directive';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';
import {
  BUBBLE_FITS,
  bubbleLayout,
  bubblesOverflow,
  connector,
  nextFit,
  facings,
  HEAD_RADIUS,
  FIGURE_CANVAS,
  hasTail,
  headCenter,
  Rect,
  slotCenter,
  Tail,
  tailTo,
} from './panel-layout';

export interface CharacterChange {
  index: number;
  change: Partial<ComicCharacter>;
}

interface DrawnTail extends Tail {
  type: string;
}

/**
 * One panel and the book's text under it. Every panel has the same size and layout: a scene-change
 * caption box at the top, bubbles in reading order above their speakers, and the figures in fixed
 * slots along the bottom. Bubble tails are measured from the rendered page, so each one ends at
 * its speaker's head.
 */
@Component({
  selector: 'app-comic-panel',
  imports: [StickFigureComponent, EditableTextDirective, PanelBackgroundComponent],
  templateUrl: './comic-panel.component.html',
  styleUrl: './comic-panel.component.css',
})
export class ComicPanelComponent {
  readonly panel = input.required<Panel>();
  readonly colorOf = input<(name: string) => string>(() => '#1b1b1f');
  readonly editable = input(true);
  /** The character sheet. Every figure is drawn from its design, looked up by name. */
  readonly designs = input<CharacterDesign[]>([]);
  readonly places = input<Place[]>([]);

  protected readonly designByName = computed(
    () => new Map(this.designs().map((d) => [d.name, d] as const)),
  );

  /** The sheet place this panel is at, whose background is drawn. */
  protected readonly place = computed(() =>
    this.places().find((p) => p.name === this.panel().place),
  );

  readonly captionChange = output<string>();
  readonly dialogueChange = output<{ index: number; text: string }>();
  readonly characterChange = output<CharacterChange>();
  readonly rename = output<{ from: string; to: string }>();

  protected readonly poses = POSES;
  protected readonly expressions = EXPRESSIONS;

  private readonly frame = viewChild.required<ElementRef<HTMLElement>>('frame');
  private readonly size = signal(0);
  protected readonly tails = signal<DrawnTail[]>([], { equal: sameJson });
  protected readonly connectors = signal<string[]>([], { equal: sameJson });

  /** The facing the layout enforces, so a speaker always looks at their listener. */
  protected readonly facing = computed(() => facings(this.panel()));

  protected readonly slots = computed(() =>
    this.panel().characters.map((_, i, all) => slotCenter(i, all.length)),
  );

  /** Which of BUBBLE_FITS the bubbles use. It steps up until no text is cut off. */
  protected readonly fitStep = signal(0);
  protected readonly fit = computed(() => BUBBLE_FITS[this.fitStep()]);
  /** The panel and size the current fit was found for. A change starts the search again. */
  private fitKey = '';

  protected readonly bubbles = computed(() => {
    const panel = this.panel();
    const minWidth = this.fit().minWidth;
    return panel.dialogue.map((line, i) => ({
      line,
      layout: bubbleLayout(panel, i, minWidth),
      tail: hasTail(panel.dialogue, i),
    }));
  });

  constructor() {
    const destroyRef = inject(DestroyRef);
    afterRenderEffect({
      earlyRead: () => {
        const key = JSON.stringify(this.panel()) + '|' + this.size();
        this.fitStep();
        return { key, ...this.measure() };
      },
      write: (measured) => {
        const { key, tails, connectors, overflowing } = measured();
        this.tails.set(tails);
        this.connectors.set(connectors);
        if (key !== this.fitKey) {
          // New content or a new size: start again from the largest lettering.
          this.fitKey = key;
          if (this.fitStep() !== 0) {
            this.fitStep.set(0);
            return;
          }
        }
        this.fitStep.set(nextFit(this.fitStep(), overflowing));
      },
    });
    // Tails depend on where text wrapped, so measure again when the panel changes size.
    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(() => this.size.update((n) => n + 1));
      queueMicrotask(() => observer.observe(this.frame().nativeElement));
      destroyRef.onDestroy(() => observer.disconnect());
    }
    document.fonts?.ready.then(() => this.size.update((n) => n + 1));
  }

  protected bubbleLabel(speaker: string, type: string): string {
    const verb = { speech: 'says', shout: 'shouts', whisper: 'whispers', thought: 'thinks' }[type];
    return `${speaker} ${verb ?? 'says'}`;
  }

  private measure(): { tails: DrawnTail[]; connectors: string[]; overflowing: boolean } {
    const frame = this.frame().nativeElement;
    const box = frame.getBoundingClientRect();
    if (box.width === 0) {
      return { tails: [], connectors: [], overflowing: false };
    }
    const local = (r: DOMRect): Rect => ({
      left: r.left - box.left,
      top: r.top - box.top,
      right: r.right - box.left,
      bottom: r.bottom - box.top,
    });
    const panel = this.panel();
    const bubbleEls = Array.from(frame.querySelectorAll<HTMLElement>('.bubble'));
    const figureEls = Array.from(frame.querySelectorAll<HTMLElement>('.figure'));
    const facing = this.facing();
    const tails: DrawnTail[] = [];
    const connectors: string[] = [];

    panel.dialogue.forEach((line, i) => {
      const bubbleEl = bubbleEls[i];
      if (!bubbleEl) {
        return;
      }
      const bubble = local(bubbleEl.getBoundingClientRect());
      if (!hasTail(panel.dialogue, i)) {
        const below = bubbleEls[i + 1];
        if (below) {
          connectors.push(connector(bubble, local(below.getBoundingClientRect())));
        }
        return;
      }
      const speaker = panel.characters.findIndex((c) => c.name === line.speaker);
      const figureEl = figureEls[speaker];
      if (!figureEl) {
        return;
      }
      const figure = local(figureEl.getBoundingClientRect());
      const character = panel.characters[speaker];
      const head = headCenter(figure, character.pose, facing[speaker]);
      const radius = HEAD_RADIUS * ((figure.right - figure.left) / FIGURE_CANVAS.width);
      tails.push({ type: line.type, ...tailTo(bubble, head, radius, line.type === 'thought') });
    });
    const areaEl = frame.querySelector<HTMLElement>('.bubbles');
    const overflowing =
      !!areaEl &&
      !this.fit().scroll &&
      bubblesOverflow(
        local(areaEl.getBoundingClientRect()),
        bubbleEls.map((el) => ({
          box: local(el.getBoundingClientRect()),
          contentHeight: el.scrollHeight,
          boxHeight: el.clientHeight,
        })),
      );
    return { tails, connectors, overflowing };
  }
}

function sameJson(a: unknown, b: unknown): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

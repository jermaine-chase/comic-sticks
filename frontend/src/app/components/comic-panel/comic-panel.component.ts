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
import { ComicCharacter, EXPRESSIONS, Panel, POSES } from '../../models/comic.models';
import { EditableTextDirective } from '../../directives/editable-text.directive';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';
import {
  bubbleLayout,
  connector,
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
  imports: [StickFigureComponent, EditableTextDirective],
  templateUrl: './comic-panel.component.html',
  styleUrl: './comic-panel.component.css',
})
export class ComicPanelComponent {
  readonly panel = input.required<Panel>();
  readonly colorOf = input<(name: string) => string>(() => '#1b1b1f');
  readonly editable = input(true);

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

  protected readonly bubbles = computed(() => {
    const panel = this.panel();
    return panel.dialogue.map((line, i) => ({
      line,
      layout: bubbleLayout(panel, line),
      tail: hasTail(panel.dialogue, i),
    }));
  });

  constructor() {
    const destroyRef = inject(DestroyRef);
    afterRenderEffect({
      earlyRead: () => {
        this.panel();
        this.size();
        return this.measure();
      },
      write: (measured) => {
        const { tails, connectors } = measured();
        this.tails.set(tails);
        this.connectors.set(connectors);
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

  private measure(): { tails: DrawnTail[]; connectors: string[] } {
    const frame = this.frame().nativeElement;
    const box = frame.getBoundingClientRect();
    if (box.width === 0) {
      return { tails: [], connectors: [] };
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
    return { tails, connectors };
  }
}

function sameJson(a: unknown, b: unknown): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

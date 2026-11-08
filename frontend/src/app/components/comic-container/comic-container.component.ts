import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  ComicCharacter,
  ComicEstimate,
  ComicOptions,
  ComicStory,
  DEFAULT_OPTIONS,
  Dialogue,
  EXPRESSIONS,
  Panel,
  POSES,
  SUPPORT_LEVELS,
  SupportLevel,
} from '../../models/comic.models';
import {
  ComicApiService,
  errorMessage,
  MAX_FILE_BYTES,
  StorySource,
} from '../../services/comic-api.service';
import { EditableTextDirective } from '../../directives/editable-text.directive';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';

const CAST_COLORS = ['#1b1b1f', '#c8321f', '#1f5fbf', '#1c8a4a', '#8a3fb8', '#b85f00'];

/** Bubble width as a share of the panel, by the number of figures in it. */
const BUBBLE_WIDTHS = [80, 80, 66, 58, 54];

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

function describeDuration(seconds: number): string {
  if (seconds < 10) {
    return 'a few seconds';
  }
  if (seconds < 90) {
    return `about ${Math.round(seconds / 5) * 5} seconds`;
  }
  return `about ${Math.round(seconds / 30) / 2} minutes`;
}

@Component({
  selector: 'app-comic-container',
  imports: [StickFigureComponent, EditableTextDirective],
  templateUrl: './comic-container.component.html',
  styleUrl: './comic-container.component.css',
})
export class ComicContainerComponent {
  private readonly api = inject(ComicApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly poses = POSES;
  protected readonly expressions = EXPRESSIONS;

  protected readonly storyText = signal('');
  protected readonly file = signal<File | null>(null);
  protected readonly story = signal<ComicStory | null>(null);
  protected readonly checking = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly supportLevels = SUPPORT_LEVELS;
  protected readonly supportLevel = signal<SupportLevel>(DEFAULT_OPTIONS.supportLevel);
  private readonly options = computed<ComicOptions>(() => ({ supportLevel: this.supportLevel() }));

  /** Set between checking the run time and the user's decision, and during the run itself. */
  protected readonly estimate = signal<ComicEstimate | null>(null);

  protected readonly canCheck = computed(
    () =>
      !this.checking() &&
      !this.loading() &&
      this.estimate() === null &&
      (this.file() !== null || this.storyText().trim().length > 0),
  );

  protected readonly estimatedTime = computed(() =>
    describeDuration(this.estimate()?.estimatedSeconds ?? 0),
  );

  /** Each character keeps one colour across the comic, assigned by first appearance. */
  private readonly castColors = computed(() => {
    const colors = new Map<string, string>();
    for (const panel of this.story()?.panels ?? []) {
      for (const { name } of panel.characters) {
        if (!colors.has(name)) {
          colors.set(name, CAST_COLORS[colors.size % CAST_COLORS.length]);
        }
      }
    }
    return colors;
  });

  protected onFileSelected(input: HTMLInputElement): void {
    const file = input.files?.[0] ?? null;
    if (file && file.size > MAX_FILE_BYTES) {
      this.error.set('That file is too large. The limit is 10 MB.');
      input.value = '';
      return;
    }
    this.error.set(null);
    this.estimate.set(null);
    this.file.set(file);
  }

  protected removeFile(input: HTMLInputElement): void {
    input.value = '';
    this.estimate.set(null);
    this.file.set(null);
  }

  protected onStoryInput(text: string): void {
    this.estimate.set(null);
    this.storyText.set(text);
  }

  /** A different level splits the text differently, so any estimate is out of date. */
  protected setSupportLevel(level: SupportLevel): void {
    this.estimate.set(null);
    this.supportLevel.set(level);
  }

  /** First step: shows the expected run time. Nothing is sent to the LLM until it is accepted. */
  protected checkRunTime(): void {
    if (!this.canCheck()) {
      return;
    }
    this.checking.set(true);
    this.error.set(null);
    this.api
      .estimate(this.storySource(), this.options())
      .pipe(
        finalize(() => this.checking.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (estimate) => this.estimate.set(estimate),
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  protected cancelEstimate(): void {
    this.estimate.set(null);
  }

  protected generate(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .generate(this.storySource(), this.options())
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.estimate.set(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (story) => this.story.set(story),
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  private storySource(): StorySource {
    return this.file() ?? this.storyText();
  }

  protected colorOf(name: string): string {
    return this.castColors().get(name) ?? CAST_COLORS[0];
  }

  /** Places a bubble over its speaker's column, keeping it inside the panel. */
  protected bubbleLayout(panel: Panel, line: Dialogue) {
    const count = Math.max(panel.characters.length, 1);
    const index = Math.max(
      panel.characters.findIndex((c) => c.name === line.speaker),
      0,
    );
    const speakerCenter = ((index + 0.5) / count) * 100;
    const width = BUBBLE_WIDTHS[count] ?? BUBBLE_WIDTHS.at(-1)!;
    const left = clamp(speakerCenter - width / 2, 0, 100 - width);
    const tail = clamp(((speakerCenter - left) / width) * 100, 12, 88);
    return { left, width, tail: `${tail}%` };
  }

  protected setTitle(title: string): void {
    this.story.update((story) => story && { ...story, title });
  }

  protected setCaption(panelIndex: number, caption: string): void {
    this.updatePanel(panelIndex, (panel) => ({ ...panel, caption }));
  }

  protected setDialogue(panelIndex: number, lineIndex: number, text: string): void {
    this.updatePanel(panelIndex, (panel) => ({
      ...panel,
      dialogue: panel.dialogue.map((line, i) => (i === lineIndex ? { ...line, text } : line)),
    }));
  }

  protected updateCharacter(
    panelIndex: number,
    characterIndex: number,
    change: Partial<ComicCharacter>,
  ): void {
    this.updatePanel(panelIndex, (panel) => ({
      ...panel,
      characters: panel.characters.map((c, i) => (i === characterIndex ? { ...c, ...change } : c)),
    }));
  }

  /** Renames a character in every panel, so bubbles and colours stay attached to them. */
  protected renameCharacter(oldName: string, newName: string): void {
    if (!newName || this.castColors().has(newName)) {
      return;
    }
    this.story.update(
      (story) =>
        story && {
          ...story,
          panels: story.panels.map((panel) => ({
            ...panel,
            characters: panel.characters.map((c) =>
              c.name === oldName ? { ...c, name: newName } : c,
            ),
            dialogue: panel.dialogue.map((line) =>
              line.speaker === oldName ? { ...line, speaker: newName } : line,
            ),
          })),
        },
    );
  }

  private updatePanel(index: number, change: (panel: Panel) => Panel): void {
    this.story.update(
      (story) =>
        story && {
          ...story,
          panels: story.panels.map((panel, i) => (i === index ? change(panel) : panel)),
        },
    );
  }
}

import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  CharacterSheet,
  ComicCharacter,
  ComicEstimate,
  ComicOptions,
  ComicStory,
  DEFAULT_OPTIONS,
  Panel,
  SavedComic,
  SUPPORT_LEVELS,
  SupportLevel,
} from '../../models/comic.models';
import {
  ComicApiService,
  errorMessage,
  MAX_FILE_BYTES,
  StorySource,
} from '../../services/comic-api.service';
import { SavedComicsService } from '../../services/saved-comics.service';
import { EditableTextDirective } from '../../directives/editable-text.directive';
import { CharacterSheetComponent } from '../character-sheet/character-sheet.component';
import { QuestionsComponent } from '../bookends/questions.component';
import { RecapComponent } from '../bookends/recap.component';
import { WhosWhoComponent } from '../bookends/whos-who.component';
import { ComicPanelComponent } from '../comic-panel/comic-panel.component';
import { EmotionKeyComponent } from '../emotion-key/emotion-key.component';
import { SavedComicsComponent } from '../saved-comics/saved-comics.component';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';

const CAST_COLORS = ['#1b1b1f', '#c8321f', '#1f5fbf', '#1c8a4a', '#8a3fb8', '#b85f00'];

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
  imports: [
    StickFigureComponent,
    EditableTextDirective,
    ComicPanelComponent,
    WhosWhoComponent,
    RecapComponent,
    QuestionsComponent,
    CharacterSheetComponent,
    EmotionKeyComponent,
    SavedComicsComponent,
  ],
  templateUrl: './comic-container.component.html',
  styleUrl: './comic-container.component.css',
})
export class ComicContainerComponent {
  private readonly api = inject(ComicApiService);
  private readonly saved = inject(SavedComicsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly storyText = signal('');
  protected readonly file = signal<File | null>(null);
  protected readonly story = signal<ComicStory | null>(null);
  protected readonly checking = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly finding = signal(false);
  /** The character sheet, from the character pass, while it is reviewed. */
  protected readonly sheet = signal<CharacterSheet | null>(null);

  protected readonly supportLevels = SUPPORT_LEVELS;
  protected readonly supportLevel = signal<SupportLevel>(DEFAULT_OPTIONS.supportLevel);
  protected readonly feelingLabels = signal(DEFAULT_OPTIONS.feelingLabels);
  protected readonly notes = signal(DEFAULT_OPTIONS.notes);
  protected readonly questions = signal(DEFAULT_OPTIONS.questions);
  protected readonly things = signal(DEFAULT_OPTIONS.things);
  private readonly options = computed<ComicOptions>(() => ({
    supportLevel: this.supportLevel(),
    feelingLabels: this.feelingLabels(),
    notes: this.notes(),
    questions: this.questions(),
    things: this.things(),
  }));

  /** The open comic's id in "My comics", once it has been saved. */
  protected readonly savedId = signal<string | null>(null);
  /** The comic as last saved, to tell whether there are changes to save. */
  private readonly savedJson = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly libraryOpen = signal(false);

  protected readonly hasUnsavedChanges = computed(() => {
    const story = this.story();
    return story !== null && JSON.stringify(story) !== this.savedJson();
  });

  protected readonly saveLabel = computed(() => {
    if (this.saving()) {
      return 'Saving...';
    }
    if (!this.savedId()) {
      return 'Save this comic';
    }
    return this.hasUnsavedChanges() ? 'Save changes' : 'Saved';
  });

  /** Shows or hides the feeling tags on the finished comic, without drawing it again. */
  protected readonly showFeelings = signal(true);
  protected readonly keyOpen = signal(false);

  protected readonly hasFeelings = computed(
    () => this.story()?.panels.some((p) => p.characters.some((c) => c.feeling)) ?? false,
  );

  /** The faces this comic shows, in the order they first appear, for the emotion key. */
  protected readonly usedExpressions = computed(() => [
    ...new Set([
      ...(this.story()?.panels ?? []).flatMap((p) => p.characters.map((c) => c.expression)),
      ...(this.story()?.bookends?.recap ?? []).flatMap((r) => (r.expression ? [r.expression] : [])),
    ]),
  ]);

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

  /**
   * Each character keeps one colour across the comic: the sheet's colour, or for anyone not on
   * it, one assigned by first appearance.
   */
  private readonly castColors = computed(() => {
    const colors = new Map<string, string>();
    for (const design of this.story()?.characters ?? []) {
      colors.set(design.name, design.color);
    }
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
    this.sheet.set(null);
    this.file.set(file);
  }

  protected removeFile(input: HTMLInputElement): void {
    input.value = '';
    this.estimate.set(null);
    this.sheet.set(null);
    this.file.set(null);
  }

  /** A different story has different characters, so the sheet is found again. */
  protected onStoryInput(text: string): void {
    this.estimate.set(null);
    this.sheet.set(null);
    this.storyText.set(text);
  }

  /** A different level splits the text differently, so any estimate is out of date. */
  protected setSupportLevel(level: SupportLevel): void {
    this.estimate.set(null);
    this.supportLevel.set(level);
  }

  /** The toggles change which LLM calls run, so any estimate is out of date. */
  protected setToggle(toggle: 'feelingLabels' | 'notes' | 'questions' | 'things', on: boolean): void {
    this.estimate.set(null);
    this[toggle].set(on);
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

  /** Saves the comic as it is now: a new saved comic the first time, the same one after that. */
  protected saveComic(): void {
    const story = this.story();
    if (!story || this.saving()) {
      return;
    }
    const id = this.savedId();
    this.saving.set(true);
    this.error.set(null);
    (id ? this.saved.update(id, story, this.options()) : this.saved.create(story, this.options()))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (summary) => {
          this.savedId.set(summary.id);
          this.savedJson.set(JSON.stringify(story));
        },
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  /** Shows a saved comic exactly as it was saved, with the options it was drawn with. */
  protected openSaved(saved: SavedComic): void {
    this.story.set(saved.comic);
    this.savedId.set(saved.id);
    this.savedJson.set(JSON.stringify(saved.comic));
    this.supportLevel.set(saved.options.supportLevel ?? DEFAULT_OPTIONS.supportLevel);
    this.feelingLabels.set(saved.options.feelingLabels ?? DEFAULT_OPTIONS.feelingLabels);
    this.notes.set(saved.options.notes ?? DEFAULT_OPTIONS.notes);
    this.questions.set(saved.options.questions ?? DEFAULT_OPTIONS.questions);
    this.things.set(saved.options.things ?? DEFAULT_OPTIONS.things);
    this.estimate.set(null);
    this.sheet.set(null);
    this.error.set(null);
    this.libraryOpen.set(false);
  }

  protected cancelEstimate(): void {
    this.estimate.set(null);
    this.sheet.set(null);
  }

  /** Second step: the character pass, whose sheet is shown for review before drawing. */
  protected findCharacters(): void {
    this.finding.set(true);
    this.error.set(null);
    this.api
      .characters(this.storySource())
      .pipe(
        finalize(() => this.finding.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (sheet) => this.sheet.set(sheet),
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  /** Last step: draws every panel from the reviewed sheet. */
  protected generate(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .generate(this.storySource(), this.options(), this.sheet())
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.estimate.set(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (story) => {
          this.story.set(story);
          this.sheet.set(null);
          this.savedId.set(null);
          this.savedJson.set(null);
        },
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  private storySource(): StorySource {
    return this.file() ?? this.storyText();
  }

  protected readonly colorFn = (name: string): string =>
    this.castColors().get(name) ?? CAST_COLORS[0];

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

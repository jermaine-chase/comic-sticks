import { Component, input, signal } from '@angular/core';
import { AnswerChoice, CharacterDesign, Panel, Place, Question } from '../../models/comic.models';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';
import { PanelThumbnailComponent } from './panel-thumbnail.component';

/**
 * Simple questions answered by choosing a picture. Feedback is gentle and literal: the right
 * answer says what it was, and a wrong one invites another try without giving it away.
 */
@Component({
  selector: 'app-questions',
  imports: [PanelThumbnailComponent, StickFigureComponent],
  styles: `
    :host {
      display: block;
    }
    .questions {
      padding: 0.8rem 1rem;
      background: #fff;
      border: 4px solid #1b1b1f;
    }
    h3 {
      margin: 0 0 0.6rem;
      font: 400 1.6rem/1 var(--font-display, sans-serif);
      letter-spacing: 0.04em;
    }
    .question + .question {
      margin-top: 1rem;
    }
    .ask {
      margin: 0 0 0.4rem;
      font: 700 1.2rem/1.4 var(--font-reading, sans-serif);
    }
    .choices {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
    }
    .choice {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.2rem;
      width: 7rem;
      padding: 0.3rem;
      font: 700 0.95rem var(--font-reading, sans-serif);
      background: #fff;
      border: 3px solid #1b1b1f;
      border-radius: 0.4rem;
      cursor: pointer;
    }
    .choice.right {
      border-color: #1c8a4a;
      background: #e8f6ec;
    }
    .choice.wrong {
      border-style: dashed;
      opacity: 0.6;
    }
    .choice app-stick-figure {
      width: 3.4rem;
      aspect-ratio: 120 / 190;
    }
    .choice app-panel-thumbnail {
      width: 100%;
    }
    .feedback {
      margin: 0.4rem 0 0;
      font: 700 1.1rem var(--font-reading, sans-serif);
    }
  `,
  template: `
    <section class="questions" aria-label="Questions">
      <h3>Can you find it?</h3>
      @for (question of questions(); track $index; let q = $index) {
        <div class="question">
          <p class="ask">{{ question.question }}</p>
          <div class="choices">
            @for (choice of question.choices; track $index; let c = $index) {
              <button
                type="button"
                class="choice"
                [class.right]="chosen()[q] === c && c === question.answerIndex"
                [class.wrong]="tried()[q]?.includes(c) && c !== question.answerIndex"
                [attr.aria-label]="choiceLabel(choice)"
                (click)="choose(q, c)"
              >
                @if (choice.character; as name) {
                  <app-stick-figure [name]="name" expression="happy" [design]="designOf(name)" />
                  <span>{{ name }}</span>
                } @else if (panelOf(choice); as panel) {
                  <app-panel-thumbnail [panel]="panel" [designs]="designs()" [places]="places()" />
                }
              </button>
            }
          </div>
          @if (chosen()[q] !== undefined) {
            <p class="feedback" role="status">{{ feedback(question, chosen()[q]!) }}</p>
          }
        </div>
      }
    </section>
  `,
})
export class QuestionsComponent {
  readonly questions = input.required<Question[]>();
  readonly panels = input.required<Panel[]>();
  readonly designs = input<CharacterDesign[]>([]);
  readonly places = input<Place[]>([]);

  /** The last choice made for each question. */
  protected readonly chosen = signal<Record<number, number>>({});
  /** Every choice tried for each question, so wrong ones stay marked. */
  protected readonly tried = signal<Record<number, number[]>>({});

  protected choose(question: number, choice: number): void {
    this.chosen.update((all) => ({ ...all, [question]: choice }));
    this.tried.update((all) => ({ ...all, [question]: [...(all[question] ?? []), choice] }));
  }

  protected feedback(question: Question, choice: number): string {
    if (choice !== question.answerIndex) {
      return 'Not this one. Try again.';
    }
    const answer = question.choices[choice];
    return answer.character
      ? `Yes! It was ${answer.character}.`
      : `Yes! It was picture ${answer.panelNumber}.`;
  }

  protected choiceLabel(choice: AnswerChoice): string {
    return choice.character ?? `Picture ${choice.panelNumber}`;
  }

  protected panelOf(choice: AnswerChoice): Panel | undefined {
    return this.panels().find((p) => p.panelNumber === choice.panelNumber);
  }

  protected designOf(name: string): CharacterDesign | null {
    return this.designs().find((d) => d.name === name) ?? null;
  }
}

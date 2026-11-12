import { Component, computed, input } from '@angular/core';
import { CharacterDesign } from '../../models/comic.models';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';

/**
 * The chapter's opening panel: everyone who appears in it, drawn as they always look and named,
 * with one plain line saying where the chapter starts. It has the same frame as every other panel.
 */
@Component({
  selector: 'app-whos-who',
  imports: [StickFigureComponent],
  styles: `
    :host {
      display: block;
    }
    .frame {
      display: flex;
      flex-direction: column;
      aspect-ratio: 1 / 1;
      padding: 0.6rem;
      background: #fffaf0;
      border: 4px solid #1b1b1f;
    }
    h3 {
      margin: 0;
      font: 400 1.6rem/1 var(--font-display, sans-serif);
      letter-spacing: 0.04em;
      text-align: center;
    }
    .cast {
      display: grid;
      flex: 1;
      grid-template-columns: repeat(var(--columns), 1fr);
      gap: 0.3rem;
      align-content: center;
      margin: 0.4rem 0;
      padding: 0;
      list-style: none;
    }
    .member {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 0;
      text-align: center;
    }
    .figure {
      width: min(100%, 4.6rem);
      aspect-ratio: 120 / 190;
    }
    .name {
      font: 700 1rem/1.2 var(--font-reading, sans-serif);
    }
    .about {
      font: 0.8rem/1.25 var(--font-reading, sans-serif);
      overflow-wrap: anywhere;
    }
    .where {
      margin: 0;
      padding: 0.35rem 0.6rem;
      font: 700 1.05rem/1.3 var(--font-reading, sans-serif);
      background: #ffd23f;
      border: 2px solid #1b1b1f;
    }
  `,
  template: `
    <section class="frame" aria-label="Who's who">
      <h3>Who's who</h3>
      <ul class="cast" [style.--columns]="columns()">
        @for (member of members(); track member.name) {
          <li class="member">
            <app-stick-figure
              class="figure"
              expression="happy"
              [name]="member.name"
              [design]="member.design"
            />
            <span class="name">{{ member.name }}</span>
            @if (member.about) {
              <span class="about">{{ member.about }}</span>
            }
          </li>
        }
      </ul>
      @if (whereWeAre()) {
        <p class="where"><strong>Where we are:</strong> {{ whereWeAre() }}</p>
      }
    </section>
  `,
})
export class WhosWhoComponent {
  readonly characters = input.required<string[]>();
  readonly whereWeAre = input('');
  readonly designs = input<CharacterDesign[]>([]);

  protected readonly members = computed(() =>
    this.characters().map((name) => {
      const design = this.designs().find((d) => d.name === name) ?? null;
      return { name, design, about: about(name, design?.description ?? '') };
    }),
  );

  protected readonly columns = computed(() => Math.min(Math.max(this.characters().length, 1), 3));
}

/** "Mole: a shy, kind animal" becomes "a shy, kind animal", since the name is shown above it. */
function about(name: string, description: string): string {
  const prefix = `${name}:`;
  return description.startsWith(prefix) ? description.slice(prefix.length).trim() : description;
}

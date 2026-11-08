import { Component, input } from '@angular/core';
import { CharacterDesign, Panel, Place, RecapItem } from '../../models/comic.models';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';
import { PanelThumbnailComponent } from './panel-thumbnail.component';

/**
 * "What happened": a few plain sentences, each next to the picture it describes, ending with how
 * the main character feels.
 */
@Component({
  selector: 'app-recap',
  imports: [PanelThumbnailComponent, StickFigureComponent],
  styles: `
    :host {
      display: block;
    }
    .recap {
      padding: 0.8rem 1rem;
      background: #fff;
      border: 4px solid #1b1b1f;
    }
    h3 {
      margin: 0 0 0.6rem;
      font: 400 1.6rem/1 var(--font-display, sans-serif);
      letter-spacing: 0.04em;
    }
    ol {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }
    li {
      display: flex;
      gap: 0.8rem;
      align-items: center;
    }
    .thumb {
      flex: none;
      width: 5.5rem;
    }
    .mini {
      flex: none;
      display: flex;
      justify-content: center;
      width: 5.5rem;
    }
    .mini app-stick-figure {
      width: 3.4rem;
      aspect-ratio: 120 / 190;
    }
    p {
      margin: 0;
      font: 1.2rem/1.45 var(--font-reading, sans-serif);
    }
    .ending p {
      font-weight: 700;
    }
  `,
  template: `
    <section class="recap" aria-label="What happened">
      <h3>What happened</h3>
      <ol>
        @for (item of recap(); track $index) {
          <li [class.ending]="item.character">
            @if (panelOf(item); as panel) {
              <app-panel-thumbnail
                class="thumb"
                [panel]="panel"
                [designs]="designs()"
                [places]="places()"
              />
            } @else if (item.character) {
              <span class="mini">
                <app-stick-figure
                  [name]="item.character"
                  [expression]="item.expression ?? 'neutral'"
                  [design]="designOf(item.character)"
                />
              </span>
            }
            <p>{{ item.sentence }}</p>
          </li>
        }
      </ol>
    </section>
  `,
})
export class RecapComponent {
  readonly recap = input.required<RecapItem[]>();
  readonly panels = input.required<Panel[]>();
  readonly designs = input<CharacterDesign[]>([]);
  readonly places = input<Place[]>([]);

  protected panelOf(item: RecapItem): Panel | undefined {
    return item.panelNumber === null
      ? undefined
      : this.panels().find((p) => p.panelNumber === item.panelNumber);
  }

  protected designOf(name: string): CharacterDesign | null {
    return this.designs().find((d) => d.name === name) ?? null;
  }
}

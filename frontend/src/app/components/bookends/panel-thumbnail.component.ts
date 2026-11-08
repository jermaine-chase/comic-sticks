import { Component, computed, input } from '@angular/core';
import { CharacterDesign, Panel, Place } from '../../models/comic.models';
import { PanelBackgroundComponent } from '../comic-panel/panel-background.component';
import { facings, slotCenter } from '../comic-panel/panel-layout';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';

/**
 * A small copy of a panel, with its place and figures but no bubbles or text, so the reader can
 * match a recap line or an answer to the picture they saw.
 */
@Component({
  selector: 'app-panel-thumbnail',
  imports: [PanelBackgroundComponent, StickFigureComponent],
  styles: `
    :host {
      position: relative;
      display: block;
      aspect-ratio: 1 / 1;
      overflow: hidden;
      background: linear-gradient(#fff 0 78%, #ececec 78%);
      border: 3px solid #1b1b1f;
    }
    :host(.scene-indoor) {
      background: linear-gradient(#fbf3e2 0 78%, #e2c9a0 78%);
    }
    :host(.scene-outdoor) {
      background: linear-gradient(#dff1ff 0 78%, #bfe3a8 78%);
    }
    :host(.scene-night) {
      background: linear-gradient(#c9c7ec 0 78%, #a3a7c7 78%);
    }
    .figure {
      position: absolute;
      bottom: 6%;
      width: 24%;
      aspect-ratio: 120 / 190;
      transform: translateX(-50%);
    }
    .number {
      position: absolute;
      top: 0;
      left: 0;
      padding: 0 0.3rem;
      font: 700 0.8rem/1.4 var(--font-reading, sans-serif);
      background: #fff;
      border: 0 solid #1b1b1f;
      border-width: 0 2px 2px 0;
    }
  `,
  host: {
    role: 'img',
    '[class]': "'scene-' + panel().scene",
    '[attr.aria-label]': 'label()',
  },
  template: `
    @if (place(); as at) {
      <app-panel-background [background]="at.background" [night]="panel().scene === 'night'" />
    }
    @for (character of panel().characters; track $index; let i = $index) {
      <app-stick-figure
        class="figure"
        [style.left.%]="slot(i)"
        [pose]="character.pose"
        [expression]="character.expression"
        [facing]="facing()[i]"
        [name]="character.name"
        [design]="designOf(character.name)"
      />
    }
    <span class="number" aria-hidden="true">{{ panel().panelNumber }}</span>
  `,
})
export class PanelThumbnailComponent {
  readonly panel = input.required<Panel>();
  readonly designs = input<CharacterDesign[]>([]);
  readonly places = input<Place[]>([]);

  protected readonly facing = computed(() => facings(this.panel()));
  protected readonly place = computed(() =>
    this.places().find((p) => p.name === this.panel().place),
  );
  protected readonly label = computed(() => {
    const names = this.panel().characters.map((c) => c.name);
    return `Picture ${this.panel().panelNumber}${names.length ? ': ' + names.join(' and ') : ''}`;
  });

  protected slot(index: number): number {
    return slotCenter(index, this.panel().characters.length);
  }

  protected designOf(name: string): CharacterDesign | null {
    return this.designs().find((d) => d.name === name) ?? null;
  }
}

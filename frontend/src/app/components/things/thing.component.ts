import { Component, computed, input } from '@angular/core';
import { Thing } from '../../models/comic.models';
import { THING_ART } from './things.drawings';

/** One object or background animal, drawn on its own, standing on the bottom of its box. */
@Component({
  selector: 'app-thing',
  host: { '[attr.data-thing]': 'thing()' },
  styles: `
    :host {
      display: block;
    }
    svg {
      display: block;
      width: 100%;
      height: auto;
      overflow: visible;
    }
  `,
  template: `
    <svg
      [attr.viewBox]="'0 0 ' + art().width + ' ' + art().height"
      role="img"
      [attr.aria-label]="label()"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      @for (shape of art().shapes; track $index) {
        @switch (shape.kind) {
          @case ('path') {
            <path
              [attr.d]="shape.d"
              [attr.fill]="shape.fill"
              [attr.stroke]="shape.stroke"
              [attr.stroke-width]="shape.strokeWidth"
            />
          }
          @case ('circle') {
            <circle
              [attr.cx]="shape.cx"
              [attr.cy]="shape.cy"
              [attr.r]="shape.r"
              [attr.fill]="shape.fill"
              [attr.stroke]="shape.stroke"
              [attr.stroke-width]="shape.strokeWidth"
            />
          }
          @case ('ellipse') {
            <ellipse
              [attr.cx]="shape.cx"
              [attr.cy]="shape.cy"
              [attr.rx]="shape.rx"
              [attr.ry]="shape.ry"
              [attr.fill]="shape.fill"
              [attr.stroke]="shape.stroke"
              [attr.stroke-width]="shape.strokeWidth"
            />
          }
          @case ('rect') {
            <rect
              [attr.x]="shape.x"
              [attr.y]="shape.y"
              [attr.width]="shape.width"
              [attr.height]="shape.height"
              [attr.fill]="shape.fill"
              [attr.stroke]="shape.stroke"
              [attr.stroke-width]="shape.strokeWidth"
            />
          }
        }
      }
    </svg>
  `,
})
export class ThingComponent {
  readonly thing = input.required<Thing>();
  /** What a screen reader hears, such as "a dog". */
  readonly label = input<string>('');

  protected readonly art = computed(() => THING_ART[this.thing()]);
}

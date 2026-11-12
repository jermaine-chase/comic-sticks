import { Component, computed, input } from '@angular/core';
import { Background } from '../../models/comic.models';
import { BACKGROUND_ART, BackgroundShape } from './backgrounds';

/** Where the stars go on a night sky: few, small and away from the middle. */
const STARS = [
  [8, 8],
  [30, 6],
  [46, 14],
  [64, 5],
  [94, 26],
  [6, 30],
] as const;

/**
 * A plain, recognizable backdrop for a place. Each preset is a few simple shapes in soft colours
 * (see backgrounds.ts), so the reader can tell where the scene is at a glance without searching
 * through detail. The same place always gets the same picture. At night the colours dim, windows
 * light up, and outdoor scenes get a moon and a few stars.
 */
@Component({
  selector: 'app-panel-background',
  styles: `
    :host {
      position: absolute;
      inset: 0;
      display: block;
    }
    svg {
      display: block;
      width: 100%;
      height: 100%;
    }
  `,
  template: `
    <svg
      viewBox="0 0 100 100"
      preserveAspectRatio="none"
      aria-hidden="true"
      [attr.data-background]="background()"
    >
      @for (shape of shapes(); track $index) {
        @switch (shape.kind) {
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
          @case ('path') {
            <path
              [attr.d]="shape.d"
              [attr.fill]="shape.fill"
              [attr.stroke]="shape.stroke"
              [attr.stroke-width]="shape.strokeWidth"
              stroke-linecap="round"
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
        }
      }
      @if (night()) {
        <rect class="night" width="100" height="100" fill="#1b1f4a" opacity="0.22" />
        @if (art().outdoor) {
          <circle class="moon" cx="86" cy="12" r="6" fill="#fff6c9" />
          @for (star of stars; track $index) {
            <circle class="star" [attr.cx]="star[0]" [attr.cy]="star[1]" r="0.7" fill="#fff6c9" />
          }
        }
      }
    </svg>
  `,
})
export class PanelBackgroundComponent {
  readonly background = input<Background>('plain');
  readonly night = input(false);

  protected readonly stars = STARS;
  protected readonly art = computed(() => BACKGROUND_ART[this.background()] ?? BACKGROUND_ART.plain);

  /** The preset's shapes, with their night colours after dark. Shapes hidden at night are left out. */
  protected readonly shapes = computed((): BackgroundShape[] => {
    const shapes = this.art().shapes;
    if (!this.night()) {
      return shapes;
    }
    return shapes
      .filter((shape) => shape.night !== 'none')
      .map((shape) => (shape.night ? { ...shape, fill: shape.night } : shape));
  });
}

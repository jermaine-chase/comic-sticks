import { Component, Input } from '@angular/core';
import { Expression, Facing, Pose } from '../../models/comic.models';
import {
  EYE_PATHS,
  Face,
  FACES,
  OPEN_MOUTH,
  SKELETONS,
  Skeleton,
  SWEAT_PATH,
  TEAR_PATH,
} from './stick-figure.poses';

const CANVAS_WIDTH = 120;
const FACE_INK = '#1b1b1f';

/** A posable stick figure on an inline SVG canvas. */
@Component({
  selector: 'app-stick-figure',
  styles: `
    :host {
      display: block;
    }
    svg {
      display: block;
      width: 100%;
      height: 100%;
      overflow: visible;
    }
    .symbol {
      font:
        700 30px/1 'Atkinson Hyperlegible',
        sans-serif;
    }
  `,
  template: `
    <svg viewBox="0 0 120 190" role="img" [attr.aria-label]="label">
      <g
        [attr.transform]="mirror"
        [attr.stroke]="color"
        fill="none"
        stroke-width="5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        @if (skeleton.airborne) {
          <ellipse cx="60" cy="182" rx="26" ry="4" fill="#00000026" stroke="none" />
        }
        @if (skeleton.stool) {
          <path d="M34,142 H72 M42,142 L38,180 M64,142 L68,180" stroke="#8a6d3b" />
        }
        <path [attr.d]="bodyPath" />
        <g [attr.transform]="headTransform">
          <circle r="20" fill="#fffdf6" />
          <g class="face" [attr.stroke]="faceInk" stroke-width="2.5">
            @if (face.blush) {
              <ellipse cx="-11" cy="5" rx="4.5" ry="2.5" fill="#f4a3b4" stroke="none" />
              <ellipse cx="14" cy="5" rx="4.5" ry="2.5" fill="#f4a3b4" stroke="none" />
            }
            @switch (face.eyes ?? 'dot') {
              @case ('wide') {
                <circle cx="-6" cy="-4" r="5" fill="#fff" />
                <circle cx="9" cy="-4" r="5" fill="#fff" />
                <circle cx="-5" cy="-4" r="1.8" [attr.fill]="faceInk" stroke="none" />
                <circle cx="10" cy="-4" r="1.8" [attr.fill]="faceInk" stroke="none" />
              }
              @case ('dot') {
                <circle cx="-6" cy="-4" r="2.6" [attr.fill]="faceInk" stroke="none" />
                <circle cx="9" cy="-4" r="2.6" [attr.fill]="faceInk" stroke="none" />
              }
              @default {
                <path class="eyes" [attr.d]="eyePath" />
              }
            }
            @if (face.brows) {
              <path class="brows" [attr.d]="face.brows" stroke-width="3" />
            }
            <path class="mouth" [attr.d]="mouth" [attr.fill]="mouthFill" />
            @if (face.tear) {
              <path class="tear" [attr.d]="tearPath" fill="#6db9ee" stroke="none" />
            }
            @if (face.sweat) {
              <path class="sweat" [attr.d]="sweatPath" fill="#6db9ee" stroke="none" />
            }
            @if (face.marks) {
              <path class="marks" [attr.d]="face.marks" stroke-width="2.5" />
            }
          </g>
        </g>
      </g>
      @if (face.symbol) {
        <text
          class="symbol"
          text-anchor="middle"
          [attr.x]="symbolX"
          [attr.y]="symbolY"
          [attr.fill]="faceInk"
        >
          {{ face.symbol }}
        </text>
      }
    </svg>
  `,
})
export class StickFigureComponent {
  @Input() pose: Pose = 'standing';
  @Input() expression: Expression = 'neutral';
  @Input() facing: Facing = 'right';
  @Input() color = '#1b1b1f';
  @Input() name = 'Stick figure';

  protected readonly faceInk = FACE_INK;
  protected readonly tearPath = TEAR_PATH;
  protected readonly sweatPath = SWEAT_PATH;

  protected get skeleton(): Skeleton {
    return SKELETONS[this.pose] ?? SKELETONS.standing;
  }

  protected get face(): Face {
    return FACES[this.expression] ?? FACES.neutral;
  }

  protected get eyePath(): string {
    const eyes = this.face.eyes;
    return eyes === 'happy' || eyes === 'closed' || eyes === 'half' ? EYE_PATHS[eyes] : '';
  }

  protected get label(): string {
    return `${this.name}, ${this.pose}, looking ${this.expression}`;
  }

  protected get mirror(): string | null {
    return this.facing === 'left' ? `translate(${CANVAS_WIDTH},0) scale(-1,1)` : null;
  }

  protected get headTransform(): string {
    const [x, y] = this.skeleton.head;
    return `translate(${x},${y})`;
  }

  /** Above the head, on the side the figure faces, and never mirrored so it stays readable. */
  protected get symbolX(): number {
    const x = this.skeleton.head[0] + 22;
    return this.facing === 'left' ? CANVAS_WIDTH - x : x;
  }

  protected get symbolY(): number {
    return this.skeleton.head[1] - 18;
  }

  protected get bodyPath(): string {
    const { shoulder, hip, backArm, frontArm, backLeg, frontLeg, head } = this.skeleton;
    const line = (...points: readonly (readonly [number, number])[]) =>
      'M' + points.map(([x, y]) => `${x},${y}`).join(' L');
    return [
      line(head, shoulder, hip),
      line(shoulder, ...backArm),
      line(shoulder, ...frontArm),
      line(hip, ...backLeg),
      line(hip, ...frontLeg),
    ].join(' ');
  }

  protected get mouth(): string {
    return this.skeleton.openMouth ? OPEN_MOUTH : this.face.mouth;
  }

  protected get mouthFill(): string {
    return this.skeleton.openMouth || this.face.filledMouth ? FACE_INK : 'none';
  }
}

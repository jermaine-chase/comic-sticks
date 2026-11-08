import { Component, Input } from '@angular/core';
import { Expression, Facing, Pose } from '../../models/comic.models';
import { FACES, OPEN_MOUTH, SKELETONS, Skeleton } from './stick-figure.poses';

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
          <g [attr.stroke]="faceInk" stroke-width="2.5">
            @if (face.wideEyes) {
              <circle cx="-6" cy="-4" r="4.5" fill="#fff" />
              <circle cx="9" cy="-4" r="4.5" fill="#fff" />
              <circle cx="-5" cy="-4" r="1.5" [attr.fill]="faceInk" stroke="none" />
              <circle cx="10" cy="-4" r="1.5" [attr.fill]="faceInk" stroke="none" />
            } @else {
              <circle cx="-6" cy="-4" r="2.4" [attr.fill]="faceInk" stroke="none" />
              <circle cx="9" cy="-4" r="2.4" [attr.fill]="faceInk" stroke="none" />
            }
            @if (face.brows) {
              <path [attr.d]="face.brows" />
            }
            <path [attr.d]="mouth" [attr.fill]="mouthFill" />
            @if (face.extra) {
              <path [attr.d]="face.extra" fill="#6db9ee" stroke="none" />
            }
          </g>
        </g>
      </g>
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

  protected get skeleton(): Skeleton {
    return SKELETONS[this.pose] ?? SKELETONS.standing;
  }

  protected get face() {
    return FACES[this.expression] ?? FACES.neutral;
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
    return this.skeleton.openMouth ? FACE_INK : 'none';
  }
}

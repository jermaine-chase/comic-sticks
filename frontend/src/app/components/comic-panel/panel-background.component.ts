import { Component, input } from '@angular/core';
import { Background } from '../../models/comic.models';

/**
 * A plain, recognizable backdrop for a place. Each preset is a few simple shapes in soft colours,
 * so the reader can tell where the scene is at a glance without searching through detail. The same
 * place always gets the same picture. At night the colours are dimmed and a moon appears.
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
      @switch (background()) {
        @case ('room') {
          <rect width="100" height="78" fill="#fbf0dc" />
          <rect y="78" width="100" height="22" fill="#d9b98a" />
          <rect
            x="62"
            y="16"
            width="22"
            height="20"
            fill="#cfeaff"
            stroke="#8a6d3b"
            stroke-width="1.2"
          />
          <path d="M73,16 V36 M62,26 H84" stroke="#8a6d3b" stroke-width="1" />
        }
        @case ('burrow') {
          <rect width="100" height="100" fill="#a77b52" />
          <ellipse cx="50" cy="60" rx="46" ry="38" fill="#e7cfa8" />
          <rect y="80" width="100" height="20" fill="#c99f6e" />
          <circle cx="16" cy="14" r="2" fill="#7a5532" />
          <circle cx="86" cy="22" r="2.5" fill="#7a5532" />
        }
        @case ('cave') {
          <rect width="100" height="100" fill="#6d6a75" />
          <path d="M4,100 V40 Q50,-4 96,40 V100 Z" fill="#9c99a6" />
          <rect y="82" width="100" height="18" fill="#57545e" />
        }
        @case ('forest') {
          <rect width="100" height="78" fill="#e3f3ff" />
          <rect y="78" width="100" height="22" fill="#9fcf86" />
          <path d="M8,78 L16,40 L24,78 Z M78,78 L87,34 L96,78 Z" fill="#3f8a4a" />
          <path d="M15,78 V84 M86,78 V84" stroke="#6b4423" stroke-width="2" />
        }
        @case ('field') {
          <rect width="100" height="70" fill="#dff1ff" />
          <path d="M0,70 Q50,58 100,70 V100 H0 Z" fill="#b8e09a" />
          <circle cx="84" cy="16" r="7" fill="#ffe27a" />
        }
        @case ('riverbank') {
          <rect width="100" height="64" fill="#dff1ff" />
          <rect y="64" width="100" height="12" fill="#7cc0ef" />
          <path d="M6,68 H18 M44,71 H58 M76,67 H90" stroke="#fff" stroke-width="1.2" />
          <rect y="76" width="100" height="24" fill="#a8d98f" />
        }
        @case ('garden') {
          <rect width="100" height="78" fill="#e3f3ff" />
          <rect y="78" width="100" height="22" fill="#a8d98f" />
          <circle cx="10" cy="80" r="2.5" fill="#e5357a" />
          <circle cx="90" cy="82" r="2.5" fill="#f2c230" />
          <path d="M0,62 H100 M0,70 H100" stroke="#c9a46b" stroke-width="1.5" />
          <path
            d="M8,58 V74 M24,58 V74 M76,58 V74 M92,58 V74"
            stroke="#c9a46b"
            stroke-width="1.5"
          />
        }
        @case ('town') {
          <rect width="100" height="78" fill="#e3f3ff" />
          <rect y="78" width="100" height="22" fill="#c8c8c8" />
          <path
            d="M2,78 V50 L14,40 L26,50 V78 Z M74,78 V46 L86,36 L98,46 V78 Z"
            fill="#e8b48a"
            stroke="#8a6d3b"
            stroke-width="1"
          />
          <rect x="10" y="60" width="7" height="8" fill="#cfeaff" />
          <rect x="82" y="56" width="7" height="8" fill="#cfeaff" />
        }
        @case ('castle') {
          <rect width="100" height="78" fill="#e3f3ff" />
          <rect y="78" width="100" height="22" fill="#a8d98f" />
          <path
            d="M60,78 V36 H64 V32 H70 V36 H76 V32 H82 V36 H88 V32 H94 V36 H98 V78 Z"
            fill="#b9b6c4"
            stroke="#6d6a75"
            stroke-width="1"
          />
          <path d="M72,78 V64 Q79,56 86,64 V78 Z" fill="#6d6a75" />
        }
        @case ('school') {
          <rect width="100" height="78" fill="#fdf6e3" />
          <rect y="78" width="100" height="22" fill="#d9b98a" />
          <rect
            x="22"
            y="14"
            width="56"
            height="26"
            fill="#2f5d46"
            stroke="#8a6d3b"
            stroke-width="1.5"
          />
          <path d="M30,22 H46 M30,28 H58" stroke="#fff" stroke-width="1" />
        }
        @case ('beach') {
          <rect width="100" height="58" fill="#dff1ff" />
          <rect y="58" width="100" height="14" fill="#7cc0ef" />
          <rect y="72" width="100" height="28" fill="#f3dfa2" />
          <circle cx="16" cy="16" r="7" fill="#ffe27a" />
        }
        @case ('boat') {
          <rect width="100" height="70" fill="#dff1ff" />
          <rect y="70" width="100" height="30" fill="#7cc0ef" />
          <path d="M4,78 H96 L88,90 H12 Z" fill="#a7743f" stroke="#6b4423" stroke-width="1" />
          <path d="M88,78 V30 L66,74 Z" fill="#fff" stroke="#8a8a8a" stroke-width="0.8" />
        }
        @default {
          <rect width="100" height="78" fill="#fff" />
          <rect y="78" width="100" height="22" fill="#ececec" />
        }
      }
      @if (night()) {
        <rect class="night" width="100" height="100" fill="#1b1f4a" opacity="0.35" />
        <circle cx="86" cy="12" r="6" fill="#fff6c9" />
      }
    </svg>
  `,
})
export class PanelBackgroundComponent {
  readonly background = input<Background>('plain');
  readonly night = input(false);
}

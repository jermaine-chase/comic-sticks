import { CharacterDesign } from '../../models/comic.models';
import { Skeleton } from './stick-figure.poses';

/**
 * The identifying features from the character sheet, drawn on a stick figure facing right. Head
 * features use coordinates around the head's centre (radius 20); body features use the skeleton.
 * Every value is drawn the same way in every panel, which is what keeps a character recognizable.
 */
export interface Shape {
  kind: 'path' | 'circle' | 'ellipse' | 'rect';
  /** Names the feature, such as "hat-top_hat", so tests and styles can find it. */
  name: string;
  d?: string;
  cx?: number;
  cy?: number;
  r?: number;
  rx?: number;
  ry?: number;
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  fill: string;
  stroke?: string;
  strokeWidth?: number;
}

/** Shapes in drawing order: behind the body, behind the head, over the face, over the body. */
export interface FeatureLayers {
  back: Shape[];
  headBack: Shape[];
  headFront: Shape[];
  front: Shape[];
}

const INK = '#1b1b1f';
const SKIN = '#fffdf6';
const HAIR = '#6b4423';
const GOLD = '#f2c230';

const path = (name: string, d: string, fill: string, stroke = INK, strokeWidth = 2): Shape => ({
  kind: 'path',
  name,
  d,
  fill,
  stroke,
  strokeWidth,
});
const circle = (
  name: string,
  cx: number,
  cy: number,
  r: number,
  fill: string,
  stroke = INK,
): Shape => ({
  kind: 'circle',
  name,
  cx,
  cy,
  r,
  fill,
  stroke,
  strokeWidth: 2,
});
const ellipse = (
  name: string,
  cx: number,
  cy: number,
  rx: number,
  ry: number,
  fill: string,
  stroke = INK,
): Shape => ({ kind: 'ellipse', name, cx, cy, rx, ry, fill, stroke, strokeWidth: 2 });
const rect = (
  name: string,
  x: number,
  y: number,
  width: number,
  height: number,
  fill: string,
): Shape => ({ kind: 'rect', name, x, y, width, height, fill, stroke: INK, strokeWidth: 2 });

const HAIR_CAP = 'M-20,-2 C-22,-20 -8,-26 2,-24 C14,-24 21,-16 20,-4 C14,-14 -10,-16 -20,-2 Z';

export function featureLayers(design: CharacterDesign | null, skeleton: Skeleton): FeatureLayers {
  const layers: FeatureLayers = { back: [], headBack: [], headFront: [], front: [] };
  if (!design) {
    return layers;
  }
  const color = design.color || INK;
  const [sx, sy] = skeleton.shoulder;
  const [hx, hy] = skeleton.hip;

  switch (design.hair) {
    case 'short':
      layers.headFront.push(path('hair-short', HAIR_CAP, HAIR));
      break;
    case 'spiky':
      layers.headFront.push(
        path(
          'hair-spiky',
          'M-19,-6 L-16,-24 L-9,-15 L-4,-30 L2,-17 L8,-29 L11,-15 L18,-22 L19,-6 C10,-15 -10,-15 -19,-6 Z',
          HAIR,
        ),
      );
      break;
    case 'long':
      layers.headBack.push(path('hair-long', 'M-21,-6 C-26,10 -24,28 -12,32 L-8,8 Z', HAIR));
      layers.headFront.push(path('hair-long-top', HAIR_CAP, HAIR));
      break;
    case 'curly':
      for (const [cx, cy] of [
        [-16, -10],
        [-11, -19],
        [-2, -23],
        [8, -21],
        [16, -13],
      ]) {
        layers.headFront.push(circle('hair-curly', cx, cy, 7, HAIR));
      }
      break;
    case 'ponytail':
      layers.headBack.push(ellipse('hair-ponytail', -25, -4, 7, 13, HAIR));
      layers.headFront.push(path('hair-ponytail-top', HAIR_CAP, HAIR));
      break;
    case 'bun':
      layers.headBack.push(circle('hair-bun', -6, -25, 8, HAIR));
      layers.headFront.push(path('hair-bun-top', HAIR_CAP, HAIR));
      break;
  }

  switch (design.ears) {
    case 'round':
      layers.headBack.push(circle('ears-round', -12, -18, 8, SKIN, color));
      layers.headBack.push(circle('ears-round', 9, -19, 8, SKIN, color));
      break;
    case 'pointy':
      layers.headBack.push(
        path('ears-pointy', 'M-17,-10 L-15,-36 L-3,-18 Z M3,-18 L14,-36 L17,-10 Z', SKIN, color),
      );
      break;
    case 'long':
      layers.headBack.push(ellipse('ears-long', -8, -36, 5, 17, SKIN, color));
      layers.headBack.push(ellipse('ears-long', 7, -36, 5, 17, SKIN, color));
      break;
    case 'floppy':
      layers.headFront.push(
        path(
          'ears-floppy',
          'M-14,-14 C-32,-12 -32,14 -22,18 C-18,8 -16,0 -12,-8 Z M12,-16 C28,-14 28,10 20,14 C16,6 14,-2 10,-10 Z',
          SKIN,
          color,
        ),
      );
      break;
  }

  switch (design.hat) {
    case 'cap':
      layers.headFront.push(path('hat-cap', 'M-20,-7 C-20,-28 20,-28 20,-7 Z', '#2a6fdb'));
      layers.headFront.push(path('hat-cap-brim', 'M8,-9 L34,-7 L32,-3 L8,-5 Z', '#1d4ea3'));
      break;
    case 'top_hat':
      layers.headFront.push(rect('hat-top_hat', -15, -54, 30, 38, '#222'));
      layers.headFront.push(rect('hat-top_hat-band', -15, -24, 30, 6, '#c8321f'));
      layers.headFront.push(rect('hat-top_hat-brim', -25, -19, 50, 5, '#222'));
      break;
    case 'bowler':
      layers.headFront.push(path('hat-bowler', 'M-17,-12 C-17,-36 17,-36 17,-12 Z', '#333'));
      layers.headFront.push(ellipse('hat-bowler-brim', 0, -12, 25, 4, '#333'));
      break;
    case 'crown':
      layers.headFront.push(
        path(
          'hat-crown',
          'M-16,-14 L-16,-36 L-8,-25 L0,-40 L8,-25 L16,-36 L16,-14 Z',
          GOLD,
          '#8a6d00',
        ),
      );
      break;
    case 'witch_hat':
      layers.headFront.push(path('hat-witch_hat', 'M-14,-16 L6,-62 L14,-16 Z', '#4b2a7b'));
      layers.headFront.push(ellipse('hat-witch_hat-brim', 0, -16, 29, 5, '#4b2a7b'));
      break;
    case 'beanie':
      layers.headFront.push(path('hat-beanie', 'M-20,-6 C-20,-32 20,-32 20,-6 Z', '#c8321f'));
      layers.headFront.push(rect('hat-beanie-band', -21, -11, 42, 7, '#8b1e12'));
      layers.headFront.push(circle('hat-beanie-pompom', 0, -30, 5, '#fff'));
      break;
    case 'straw_hat':
      layers.headFront.push(ellipse('hat-straw_hat-brim', 0, -14, 33, 6, '#e8c56a'));
      layers.headFront.push(path('hat-straw_hat', 'M-14,-14 C-14,-34 14,-34 14,-14 Z', '#e8c56a'));
      layers.headFront.push(rect('hat-straw_hat-band', -14, -19, 28, 4, '#c8321f'));
      break;
  }

  switch (design.snout) {
    case 'short':
      layers.headFront.push(ellipse('snout-short', 19, 5, 8, 6, SKIN, color));
      layers.headFront.push(circle('snout-nose', 26, 3, 2.8, INK, INK));
      break;
    case 'long':
      layers.headFront.push(ellipse('snout-long', 25, 6, 13, 6, SKIN, color));
      layers.headFront.push(circle('snout-nose', 37, 5, 3.2, INK, INK));
      break;
    case 'beak':
      layers.headFront.push(path('snout-beak', 'M16,0 L34,6 L16,11 Z', '#f2a33a'));
      break;
  }

  switch (design.tail) {
    case 'long':
      layers.back.push(
        path(
          'tail-long',
          `M${hx},${hy} C${hx - 20},${hy + 6} ${hx - 32},${hy - 24} ${hx - 42},${hy - 14}`,
          'none',
          color,
          4,
        ),
      );
      break;
    case 'bushy':
      layers.back.push(ellipse('tail-bushy', hx - 22, hy - 10, 17, 9, '#d9c3a0', color));
      break;
    case 'curly':
      layers.back.push(
        path('tail-curly', `M${hx},${hy} c-12,0 -18,-12 -11,-18 c7,-6 15,2 7,7`, 'none', color, 4),
      );
      break;
    case 'short':
      layers.back.push(circle('tail-short', hx - 15, hy - 4, 6, SKIN, color));
      break;
  }

  switch (design.accessory) {
    case 'bow':
      layers.headFront.push(
        path('accessory-bow', 'M-6,-22 L-19,-31 L-19,-14 Z M-6,-22 L7,-31 L7,-14 Z', '#e5357a'),
      );
      layers.headFront.push(circle('accessory-bow-knot', -6, -22, 3.5, '#e5357a'));
      break;
    case 'glasses':
      layers.headFront.push(circle('accessory-glasses', -6, -4, 6.5, 'none'));
      layers.headFront.push(circle('accessory-glasses', 9, -4, 6.5, 'none'));
      layers.headFront.push(
        path('accessory-glasses-bridge', 'M0.5,-4 L2.5,-4 M15.5,-4 L20,-6', 'none'),
      );
      break;
    case 'beard':
      layers.headFront.push(
        path('accessory-beard', 'M-12,12 C-10,34 14,34 16,12 C10,19 -6,19 -12,12 Z', HAIR),
      );
      break;
    case 'whiskers':
      layers.headFront.push(
        path('accessory-whiskers', 'M16,4 L34,-1 M16,7 L34,8 M16,10 L32,16', 'none', INK, 1.5),
      );
      break;
    case 'cape':
      layers.back.push(
        path(
          'accessory-cape',
          `M${sx},${sy} L${hx - 28},${hy + 44} L${hx + 2},${hy + 44} Z`,
          '#c8321f',
        ),
      );
      break;
    case 'scarf':
      layers.front.push(rect('accessory-scarf', sx - 11, sy - 4, 22, 8, '#1c8a4a'));
      layers.front.push(
        path(
          'accessory-scarf-end',
          `M${sx - 7},${sy + 3} L${sx - 13},${sy + 24} L${sx - 3},${sy + 24} Z`,
          '#1c8a4a',
        ),
      );
      break;
    case 'pocket_watch':
      layers.front.push(
        path(
          'accessory-pocket_watch-chain',
          `M${hx + 2},${hy - 16} Q${hx + 12},${hy - 14} ${hx + 12},${hy - 8}`,
          'none',
          GOLD,
          2,
        ),
      );
      layers.front.push(circle('accessory-pocket_watch', hx + 12, hy - 2, 6.5, GOLD));
      layers.front.push(
        path(
          'accessory-pocket_watch-hands',
          `M${hx + 12},${hy - 2} L${hx + 12},${hy - 6} M${hx + 12},${hy - 2} L${hx + 15},${hy - 2}`,
          'none',
          INK,
          1.2,
        ),
      );
      break;
    case 'bag':
      layers.front.push(
        path(
          'accessory-bag-strap',
          `M${sx + 4},${sy} L${hx - 14},${hy - 8}`,
          'none',
          '#5a4a2f',
          2.5,
        ),
      );
      layers.front.push(rect('accessory-bag', hx - 26, hy - 10, 16, 15, '#8a6d3b'));
      break;
  }
  return layers;
}

import { Background } from '../../models/comic.models';

/**
 * The background presets, drawn on a 100 x 100 canvas. Every preset follows the same quiet style so
 * that the figures and bubbles always stand out:
 *
 * - one clear line where the floor or ground starts (y = 76), so figures always stand on something;
 * - two or three plain landmarks that say where we are, kept to the sides and the back, away from
 *   the middle where figures stand and bubbles sit;
 * - soft colours with no outlines darker than the figures' ink.
 *
 * A shape may have a `night` colour, used after dark ("none" hides it, as for the sun). At night the
 * sky darkens, house windows light up, and outdoor scenes get a moon and a few stars.
 */
export interface BackgroundShape {
  kind: 'rect' | 'path' | 'circle' | 'ellipse';
  fill: string;
  night?: string;
  stroke?: string;
  strokeWidth?: number;
  d?: string;
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  cx?: number;
  cy?: number;
  r?: number;
  rx?: number;
  ry?: number;
}

export interface BackgroundArt {
  /** Whether the sky shows, so the moon and stars come out at night. */
  outdoor: boolean;
  shapes: BackgroundShape[];
}

export const FLOOR_Y = 76;

const SKY = '#dcefff';
const NIGHT_SKY = '#3a4677';
const GRASS = '#b9de9f';
const GRASS_EDGE = '#93c47d';
const WOOD_FLOOR = '#dcc096';
const FLOOR_EDGE = '#b39469';
const WALL = '#fbf1de';
const FRAME = '#a8875c';
const GLASS = '#d6eeff';
const GLASS_NIGHT = '#2c3766';
const LIT = '#ffe7a0';
const TRUNK = '#93694a';
const LEAF = '#79b374';
const LEAF_DARK = '#5f9a63';
const PINE = '#5c9a68';
const WATER = '#93cdef';
const STONE = '#c3c0cb';
const STONE_DARK = '#9d99a8';
const SAND = '#f3e2ab';
const CLOUD = '#ffffff';
const SUN = '#ffe489';
const BRICK = '#e2b48d';
const ROOF = '#c9705a';

type Extra = Omit<Partial<BackgroundShape>, 'kind'>;

const rect = (x: number, y: number, width: number, height: number, fill: string, extra: Extra = {}): BackgroundShape => ({
  kind: 'rect',
  x,
  y,
  width,
  height,
  fill,
  ...extra,
});
const path = (d: string, fill: string, extra: Extra = {}): BackgroundShape => ({ kind: 'path', d, fill, ...extra });
const circle = (cx: number, cy: number, r: number, fill: string, extra: Extra = {}): BackgroundShape => ({
  kind: 'circle',
  cx,
  cy,
  r,
  fill,
  ...extra,
});
const ellipse = (cx: number, cy: number, rx: number, ry: number, fill: string, extra: Extra = {}): BackgroundShape => ({
  kind: 'ellipse',
  cx,
  cy,
  rx,
  ry,
  fill,
  ...extra,
});
/** A line with no fill. */
const line = (d: string, stroke: string, strokeWidth = 1): BackgroundShape => ({
  kind: 'path',
  d,
  fill: 'none',
  stroke,
  strokeWidth,
});

// --- Building blocks ------------------------------------------------------------------------------

/** Sky above, ground below, and a clear line between them. */
const outdoors = (ground = GRASS, edge = GRASS_EDGE, sky = SKY): BackgroundShape[] => [
  rect(0, 0, 100, FLOOR_Y, sky, { night: NIGHT_SKY }),
  rect(0, FLOOR_Y, 100, 100 - FLOOR_Y, ground),
  line(`M0,${FLOOR_Y} H100`, edge, 0.8),
];

/** A wall, a floor, and a skirting board where they meet. */
const indoors = (wall = WALL, floor = WOOD_FLOOR): BackgroundShape[] => [
  rect(0, 0, 100, FLOOR_Y, wall),
  rect(0, FLOOR_Y, 100, 100 - FLOOR_Y, floor),
  rect(0, FLOOR_Y - 2, 100, 2, '#ead8b8'),
  line(`M0,${FLOOR_Y} H100`, FLOOR_EDGE, 0.8),
];

/** A window seen from inside: sky by day, dark at night. */
const windowPane = (x: number, y: number, w: number, h: number): BackgroundShape[] => [
  rect(x, y, w, h, GLASS, { night: GLASS_NIGHT, stroke: FRAME, strokeWidth: 1.2 }),
  line(`M${x + w / 2},${y} V${y + h} M${x},${y + h / 2} H${x + w}`, FRAME, 1),
];

/** A window seen from outside: lit up at night. */
const houseWindow = (x: number, y: number, w = 7, h = 7): BackgroundShape =>
  rect(x, y, w, h, GLASS, { night: LIT, stroke: FRAME, strokeWidth: 0.6 });

const cloud = (cx: number, cy: number): BackgroundShape[] => [
  ellipse(cx, cy, 7, 3, CLOUD, { night: 'none' }),
  ellipse(cx + 4, cy - 2, 4.5, 3, CLOUD, { night: 'none' }),
];

const roundTree = (x: number, base = FLOOR_Y, size = 1): BackgroundShape[] => [
  rect(x - 1.5 * size, base - 14 * size, 3 * size, 14 * size, TRUNK),
  circle(x, base - 20 * size, 9 * size, LEAF),
  circle(x - 5 * size, base - 15 * size, 5 * size, LEAF_DARK),
];

const pine = (x: number, base = FLOOR_Y, size = 1, fill = PINE): BackgroundShape[] => [
  rect(x - 1.2 * size, base - 5 * size, 2.4 * size, 5 * size, TRUNK),
  path(`M${x - 9 * size},${base - 5 * size} L${x},${base - 36 * size} L${x + 9 * size},${base - 5 * size} Z`, fill),
];

const bookcase = (x: number, w: number): BackgroundShape[] => {
  const colours = ['#c9705a', '#6d9bc9', '#e3c46a', '#7fb07a', '#a07ab8'];
  const shapes = [rect(x, 22, w, FLOOR_Y - 22, '#b4895f', { stroke: FRAME, strokeWidth: 0.8 })];
  for (const shelf of [26, 40, 54, 66]) {
    for (let i = 0; i < Math.floor((w - 2) / 2.6); i++) {
      shapes.push(rect(x + 1.2 + i * 2.6, shelf, 2, 9 - (i % 3), colours[(i + shelf) % colours.length]));
    }
    shapes.push(rect(x, shelf + 9, w, 1.2, FRAME));
  }
  return shapes;
};

// --- The presets ----------------------------------------------------------------------------------

export const BACKGROUND_ART: Record<Background, BackgroundArt> = {
  plain: { outdoor: false, shapes: [rect(0, 0, 100, FLOOR_Y, '#ffffff'), rect(0, FLOOR_Y, 100, 24, '#ececec'), line(`M0,${FLOOR_Y} H100`, '#cfcfcf', 0.8)] },

  room: {
    outdoor: false,
    shapes: [
      ...indoors(),
      ...windowPane(64, 14, 22, 20),
      rect(12, 20, 14, 11, '#f6dcc4', { stroke: FRAME, strokeWidth: 1 }),
      path('M14,29 L18,24 L21,27 L24,23 V29 Z', '#9cc79a'),
    ],
  },

  kitchen: {
    outdoor: false,
    shapes: [
      ...indoors('#fdf6e4', '#e0d2bd'),
      rect(2, 12, 26, 16, '#e7d1b0', { stroke: FRAME, strokeWidth: 0.8 }),
      line('M15,12 V28', FRAME, 0.6),
      rect(0, 52, 30, FLOOR_Y - 52, '#efe6d8', { stroke: FRAME, strokeWidth: 0.8 }),
      rect(0, 50, 31, 3, '#b98a5a'),
      ellipse(14, 47, 6, 3, '#9aa3ad'),
      ...windowPane(66, 16, 20, 18),
    ],
  },

  bedroom: {
    outdoor: false,
    shapes: [
      ...indoors('#eef0fb', WOOD_FLOOR),
      ...windowPane(12, 14, 18, 18),
      path('M10,12 H32 L30,36 H27 L28,14 H14 L15,36 H12 Z', '#e6a6b8'),
      rect(74, 50, 26, 6, '#b4895f'),
      rect(74, 56, 26, FLOOR_Y - 56, '#8fb3e0'),
      rect(80, 52, 12, 5, '#ffffff', { stroke: '#cfd6e6', strokeWidth: 0.5 }),
    ],
  },

  burrow: {
    outdoor: false,
    shapes: [
      rect(0, 0, 100, 100, '#a77b52'),
      ellipse(50, 56, 47, 40, '#ead3ad'),
      rect(0, FLOOR_Y, 100, 24, '#c99f6e'),
      line(`M0,${FLOOR_Y} H100`, '#a77b52', 0.8),
      line('M14,10 q4,6 2,12 M20,8 q-2,5 2,9 M84,12 q-3,6 0,12', '#7a5532', 1),
      circle(80, 36, 6, '#fff2c9', { night: '#ffe08a', stroke: '#8a6440', strokeWidth: 1 }),
    ],
  },

  cave: {
    outdoor: false,
    shapes: [
      rect(0, 0, 100, 100, '#6d6a75'),
      path('M4,100 V40 Q50,-4 96,40 V100 Z', '#9c99a6'),
      path('M20,18 l3,9 l3,-9 Z M62,10 l2.5,8 l2.5,-8 Z M78,22 l2,6 l2,-6 Z', '#7d7a86'),
      rect(0, FLOOR_Y, 100, 24, '#5d5a65'),
      line(`M0,${FLOOR_Y} H100`, '#4b4853', 0.8),
    ],
  },

  tunnel: {
    outdoor: false,
    shapes: [
      rect(0, 0, 100, 100, '#5a5560'),
      path('M8,100 V36 Q50,2 92,36 V100 Z', '#8d8795'),
      line('M8,48 H92 M8,60 H92 M30,36 V48 M50,22 V36 M70,36 V48 M40,48 V60 M60,48 V60', '#7a7482', 0.8),
      circle(18, 30, 3, '#ffe7a0', { night: LIT }),
      circle(82, 30, 3, '#ffe7a0', { night: LIT }),
      rect(0, FLOOR_Y, 100, 24, '#4b4752'),
      line(`M0,${FLOOR_Y} H100`, '#3e3a44', 0.8),
    ],
  },

  forest: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M0,62 Q30,52 60,60 T100,56 V76 H0 Z', '#cfe8bd'),
      ...pine(8, FLOOR_Y, 1.1),
      ...pine(22, FLOOR_Y, 0.8, '#6aa875'),
      ...pine(82, FLOOR_Y, 1.2),
      ...pine(95, FLOOR_Y, 0.85, '#6aa875'),
    ],
  },

  field: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M0,70 Q50,58 100,68 V76 H0 Z', '#c7e6ae'),
      circle(84, 16, 7, SUN, { night: 'none' }),
      ...cloud(20, 18),
      ...roundTree(70, 66, 0.5),
    ],
  },

  riverbank: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M0,60 Q40,54 100,60 V64 H0 Z', '#c7e6ae'),
      rect(0, 64, 100, 12, WATER, { night: '#4f6fa6' }),
      line('M6,68 H18 M44,71 H58 M76,67 H90', '#ffffff', 1.2),
      line('M4,76 V69 M6,76 V67 M8,76 V70 M92,76 V68 M94,76 V66 M96,76 V69', '#6f9e5c', 1),
      ...cloud(70, 16),
    ],
  },

  lake: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M0,54 Q20,40 40,52 Q60,38 80,50 Q90,44 100,48 V58 H0 Z', '#a9c4b0'),
      rect(0, 56, 100, 16, WATER, { night: '#4f6fa6' }),
      line('M10,62 H22 M50,66 H64 M78,60 H90', '#ffffff', 1.2),
      path('M0,72 Q50,68 100,72 V76 H0 Z', GRASS),
      ...cloud(24, 16),
    ],
  },

  garden: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      line('M0,62 H100 M0,70 H100', '#c9a46b', 1.5),
      line('M8,58 V76 M24,58 V76 M76,58 V76 M92,58 V76', '#c9a46b', 1.5),
      ellipse(14, 74, 9, 5, LEAF_DARK),
      circle(10, 72, 1.6, '#e5577a'),
      circle(16, 70, 1.6, '#f2c230'),
      ellipse(86, 74, 9, 5, LEAF_DARK),
      circle(84, 71, 1.6, '#e5577a'),
      circle(90, 72, 1.6, '#ffffff'),
      ...cloud(50, 14),
    ],
  },

  park: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M30,100 Q40,86 50,76 H58 Q52,88 46,100 Z', '#e8d9b5'),
      ...roundTree(86, FLOOR_Y, 1.1),
      rect(4, 64, 18, 2, '#9a6b45'),
      rect(4, 60, 18, 2, '#9a6b45'),
      line('M6,66 V76 M20,66 V76', '#5a4a3a', 1.2),
      ...cloud(36, 14),
    ],
  },

  playground: {
    outdoor: true,
    shapes: [
      ...outdoors('#e9cfa2', '#cdae7c'),
      line('M6,76 V42 M14,76 V42', '#d9534f', 1.6),
      line('M6,48 H14 M6,56 H14 M6,64 H14 M6,72 H14', '#d9534f', 1),
      path('M14,42 H18 Q30,58 32,76 H28 Q26,60 14,46 Z', '#4f86c6'),
      line('M76,76 L82,40 L88,76 M90,76 L96,40 L100,62 M82,40 H96', '#4a7a4a', 1.6),
      line('M86,40 V62 M92,40 V62', '#6b6b6b', 0.6),
      rect(84.5, 62, 9, 1.8, '#c9705a'),
      ...cloud(48, 14),
    ],
  },

  farm: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M70,76 V46 L84,34 L98,46 V76 Z', '#c9523e'),
      path('M68,47 L84,33 L100,47', 'none', { stroke: '#f4ead8', strokeWidth: 1.5 }),
      rect(79, 58, 10, 18, '#8f3a2c'),
      line('M79,58 L89,76 M89,58 L79,76', '#f4ead8', 0.8),
      line('M0,64 H30 M0,70 H30 M4,60 V76 M16,60 V76 M28,60 V76', '#c9a46b', 1.4),
      circle(16, 14, 6, SUN, { night: 'none' }),
    ],
  },

  town: {
    outdoor: true,
    shapes: [
      ...outdoors('#cfcfcf', '#aaaaaa'),
      path('M2,76 V50 L14,40 L26,50 V76 Z', BRICK, { stroke: FRAME, strokeWidth: 0.6 }),
      path('M0,51 L14,38 L28,51', 'none', { stroke: ROOF, strokeWidth: 2 }),
      houseWindow(6, 56),
      rect(16, 64, 6, 12, '#8a5a3a'),
      path('M74,76 V46 L86,36 L98,46 V76 Z', '#ecd59c', { stroke: FRAME, strokeWidth: 0.6 }),
      path('M72,47 L86,34 L100,47', 'none', { stroke: ROOF, strokeWidth: 2 }),
      houseWindow(88, 52),
      rect(77, 64, 6, 12, '#5d7fa8'),
      ...cloud(48, 14),
    ],
  },

  street: {
    outdoor: true,
    shapes: [
      ...outdoors('#d4d4d4', '#9a9a9a'),
      rect(0, 30, 26, 46, '#e8c6a6'),
      houseWindow(4, 36),
      houseWindow(15, 36),
      rect(3, 54, 20, 22, '#bfe0f2', { night: LIT, stroke: FRAME, strokeWidth: 0.6 }),
      rect(0, 50, 26, 4, '#d9534f'),
      rect(78, 26, 22, 50, '#c9d3e6'),
      houseWindow(82, 32),
      houseWindow(92, 32),
      houseWindow(82, 46),
      houseWindow(92, 46),
      line('M66,76 V40 M66,40 Q66,36 70,36', '#4a4a4a', 1.4),
      circle(71, 38, 2, '#fff4c4', { night: LIT }),
      line('M0,88 H100', '#ffffff', 0.8),
    ],
  },

  shop: {
    outdoor: false,
    shapes: [
      ...indoors('#f7efe3', '#d8cfc2'),
      rect(4, 16, 36, 48, '#d9b88c', { stroke: FRAME, strokeWidth: 0.8 }),
      line('M4,30 H40 M4,44 H40 M4,58 H40', FRAME, 1),
      rect(7, 23, 6, 7, '#e5577a'),
      rect(15, 25, 5, 5, '#4f86c6'),
      rect(23, 22, 7, 8, '#f2c230'),
      rect(8, 37, 8, 7, '#7fb07a'),
      rect(20, 38, 6, 6, '#e8862f'),
      rect(29, 36, 7, 8, '#a07ab8'),
      rect(10, 51, 9, 7, '#c9705a'),
      rect(24, 52, 8, 6, '#6d9bc9'),
      rect(70, 56, 30, FLOOR_Y - 56, '#b4895f', { stroke: FRAME, strokeWidth: 0.8 }),
      rect(72, 50, 9, 6, '#6b6b6b'),
      rect(62, 8, 30, 8, '#4f86c6'),
      line('M66,12 H88', '#ffffff', 1.4),
    ],
  },

  library: {
    outdoor: false,
    shapes: [...indoors('#f4ecdc', WOOD_FLOOR), ...bookcase(2, 26), ...bookcase(72, 26)],
  },

  school: {
    outdoor: false,
    shapes: [
      ...indoors('#fdf6e3', WOOD_FLOOR),
      rect(22, 14, 56, 26, '#2f5d46', { stroke: FRAME, strokeWidth: 1.5 }),
      line('M30,22 H46 M30,28 H58 M30,34 H40', '#ffffff', 1),
      circle(88, 14, 5, '#ffffff', { stroke: '#5a5a5a', strokeWidth: 1 }),
      line('M88,14 V11 M88,14 L90,15', '#5a5a5a', 0.8),
      ...windowPane(4, 16, 12, 22),
    ],
  },

  castle: {
    outdoor: true,
    shapes: [
      ...outdoors(),
      path('M4,76 V44 H7 V40 H11 V44 H15 V40 H19 V44 H22 V76 Z', '#cfccd8', { stroke: STONE_DARK, strokeWidth: 0.6 }),
      path('M60,76 V36 H64 V32 H70 V36 H76 V32 H82 V36 H88 V32 H94 V36 H98 V76 Z', '#b9b6c4', {
        stroke: '#6d6a75',
        strokeWidth: 1,
      }),
      path('M72,76 V64 Q79,56 86,64 V76 Z', '#6d6a75'),
      houseWindow(65, 44, 4, 6),
      houseWindow(90, 44, 4, 6),
      line('M79,32 V18', '#6d6a75', 0.8),
      path('M79,18 L88,21 L79,24 Z', '#d9534f'),
    ],
  },

  mountain: {
    outdoor: true,
    shapes: [
      ...outdoors('#cdc6b4', '#aca48f'),
      path('M-10,76 L22,30 L50,76 Z', '#a9b4c7'),
      path('M14,42 L22,30 L30,42 L26,40 L22,44 L18,40 Z', '#ffffff'),
      path('M40,76 L76,22 L112,76 Z', '#98a5ba'),
      path('M66,37 L76,22 L86,37 L81,34 L76,39 L71,34 Z', '#ffffff'),
      ...cloud(46, 14),
    ],
  },

  snow: {
    outdoor: true,
    shapes: [
      ...outdoors('#f7fbff', '#a9bdd3', '#cfe2f4'),
      ...pine(10, FLOOR_Y, 1, '#4f8a62'),
      path('M3,66 L10,52 L17,66 Z', '#ffffff'),
      ...pine(88, FLOOR_Y, 1.2, '#4f8a62'),
      path('M80,62 L88,46 L96,62 Z', '#ffffff'),
      circle(30, 20, 1, '#ffffff', { stroke: '#c9d8ea', strokeWidth: 0.3 }),
      circle(56, 12, 1, '#ffffff', { stroke: '#c9d8ea', strokeWidth: 0.3 }),
      circle(70, 30, 1, '#ffffff', { stroke: '#c9d8ea', strokeWidth: 0.3 }),
      circle(40, 40, 1, '#ffffff', { stroke: '#c9d8ea', strokeWidth: 0.3 }),
    ],
  },

  beach: {
    outdoor: true,
    shapes: [
      ...outdoors(SAND, '#dcc68a'),
      rect(0, 56, 100, 20, WATER, { night: '#4f6fa6' }),
      line('M8,62 H20 M44,66 H58 M76,60 H90', '#ffffff', 1.2),
      path('M0,76 Q25,72 50,76 T100,76', '#ffffff', { stroke: '#ffffff', strokeWidth: 0.8 }),
      circle(16, 16, 7, SUN, { night: 'none' }),
      ...cloud(70, 18),
    ],
  },

  boat: {
    outdoor: true,
    shapes: [
      rect(0, 0, 100, 70, SKY, { night: NIGHT_SKY }),
      rect(0, 70, 100, 30, WATER, { night: '#4f6fa6' }),
      line('M6,74 H16 M30,96 H44 M70,95 H84', '#ffffff', 1.2),
      path('M4,78 H96 L88,90 H12 Z', '#a7743f', { stroke: '#6b4423', strokeWidth: 1 }),
      line(`M4,${FLOOR_Y} H96`, '#6b4423', 0.8),
      path('M88,78 V30 L66,74 Z', '#ffffff', { stroke: '#8a8a8a', strokeWidth: 0.8 }),
      ...cloud(24, 18),
    ],
  },

  station: {
    outdoor: true,
    shapes: [
      ...outdoors('#cfc8bb', '#a69e90'),
      rect(0, 56, 100, 4, '#8a8a8a'),
      line('M0,62 H100 M0,66 H100', '#6b6b6b', 0.8),
      line('M4,60 V68 M14,60 V68 M24,60 V68 M34,60 V68 M44,60 V68 M54,60 V68 M64,60 V68 M74,60 V68 M84,60 V68 M94,60 V68', '#9a7b5a', 0.8),
      rect(0, 68, 100, 8, '#cfc8bb'),
      rect(0, 74, 100, 2, '#f2c230'),
      rect(70, 30, 30, 26, '#4f86c6', { stroke: '#2f5d8a', strokeWidth: 0.8 }),
      houseWindow(74, 36, 8, 8),
      houseWindow(86, 36, 8, 8),
      rect(6, 10, 26, 8, '#2f5d46'),
      line('M10,14 H28', '#ffffff', 1.2),
      line('M19,18 V30', '#5a5a5a', 1),
    ],
  },
};

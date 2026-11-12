import { Expression, Pose } from '../../models/comic.models';

type Point = readonly [x: number, y: number];

/** Joint positions on the 120 x 190 canvas, drawn facing right. Feet rest near y = 180. */
export interface Skeleton {
  head: Point;
  shoulder: Point;
  hip: Point;
  backArm: readonly [elbow: Point, hand: Point];
  frontArm: readonly [elbow: Point, hand: Point];
  backLeg: readonly [knee: Point, foot: Point];
  frontLeg: readonly [knee: Point, foot: Point];
  stool?: boolean;
  airborne?: boolean;
  openMouth?: boolean;
}

const UPRIGHT = { head: [60, 34], shoulder: [60, 64], hip: [60, 118] } as const;
const ARMS_DOWN = {
  backArm: [
    [46, 90],
    [42, 116],
  ],
  frontArm: [
    [74, 90],
    [78, 116],
  ],
} as const;
const LEGS_APART = {
  backLeg: [
    [52, 148],
    [46, 180],
  ],
  frontLeg: [
    [68, 148],
    [74, 180],
  ],
} as const;

export const SKELETONS: Record<Pose, Skeleton> = {
  standing: { ...UPRIGHT, ...ARMS_DOWN, ...LEGS_APART },
  walking: {
    head: [63, 34],
    shoulder: [61, 64],
    hip: [60, 118],
    backArm: [
      [48, 88],
      [38, 108],
    ],
    frontArm: [
      [74, 90],
      [90, 104],
    ],
    backLeg: [
      [48, 148],
      [32, 178],
    ],
    frontLeg: [
      [74, 146],
      [84, 180],
    ],
  },
  running: {
    head: [76, 38],
    shoulder: [69, 67],
    hip: [55, 116],
    backArm: [
      [46, 80],
      [34, 98],
    ],
    frontArm: [
      [88, 90],
      [104, 70],
    ],
    backLeg: [
      [36, 140],
      [10, 146],
    ],
    frontLeg: [
      [84, 134],
      [78, 176],
    ],
  },
  waving: {
    ...UPRIGHT,
    backArm: ARMS_DOWN.backArm,
    frontArm: [
      [88, 56],
      [98, 24],
    ],
    ...LEGS_APART,
  },
  shouting: {
    head: [68, 34],
    shoulder: [63, 65],
    hip: [57, 118],
    backArm: [
      [40, 80],
      [22, 94],
    ],
    frontArm: [
      [44, 88],
      [30, 108],
    ],
    backLeg: [
      [46, 148],
      [34, 180],
    ],
    frontLeg: [
      [72, 148],
      [84, 180],
    ],
    openMouth: true,
  },
  pointing: {
    ...UPRIGHT,
    backArm: ARMS_DOWN.backArm,
    frontArm: [
      [86, 68],
      [112, 62],
    ],
    ...LEGS_APART,
  },
  thinking: {
    ...UPRIGHT,
    backArm: [
      [46, 90],
      [66, 96],
    ],
    frontArm: [
      [86, 90],
      [76, 56],
    ],
    ...LEGS_APART,
  },
  sitting: {
    head: [58, 56],
    shoulder: [58, 86],
    hip: [54, 138],
    backArm: [
      [46, 112],
      [64, 130],
    ],
    frontArm: [
      [70, 112],
      [86, 130],
    ],
    backLeg: [
      [86, 142],
      [82, 180],
    ],
    frontLeg: [
      [92, 136],
      [94, 180],
    ],
    stool: true,
  },
  jumping: {
    head: [60, 22],
    shoulder: [60, 52],
    hip: [60, 104],
    backArm: [
      [38, 38],
      [26, 12],
    ],
    frontArm: [
      [82, 38],
      [94, 12],
    ],
    backLeg: [
      [42, 124],
      [30, 150],
    ],
    frontLeg: [
      [78, 124],
      [90, 150],
    ],
    airborne: true,
  },
  cheering: {
    ...UPRIGHT,
    backArm: [
      [40, 46],
      [28, 18],
    ],
    frontArm: [
      [80, 46],
      [92, 18],
    ],
    ...LEGS_APART,
  },
  fighting: {
    head: [58, 40],
    shoulder: [57, 70],
    hip: [52, 120],
    backArm: [
      [40, 88],
      [64, 82],
    ],
    frontArm: [
      [84, 70],
      [112, 64],
    ],
    backLeg: [
      [30, 150],
      [14, 180],
    ],
    frontLeg: [
      [76, 148],
      [88, 180],
    ],
  },
  falling: {
    head: [26, 118],
    shoulder: [50, 128],
    hip: [92, 150],
    backArm: [
      [40, 96],
      [22, 76],
    ],
    frontArm: [
      [70, 104],
      [74, 76],
    ],
    backLeg: [
      [104, 118],
      [116, 92],
    ],
    frontLeg: [
      [112, 156],
      [114, 180],
    ],
  },
};

/**
 * Face drawn around the head's centre at (0, 0), shifted towards the facing side. Faces are
 * deliberately exaggerated, and most carry a second cue (a tear, a sweat drop, a symbol above the
 * head), so that a reader can tell them apart at a glance.
 */
export interface Face {
  /** dot: small round eyes. wide: big eyes with pupils. happy: upward arcs. closed: gentle downward
   * arcs. half: sleepy lines. */
  eyes?: 'dot' | 'wide' | 'happy' | 'closed' | 'half';
  brows?: string;
  mouth: string;
  /** Fills the mouth with ink, for big open smiles and gasps. */
  filledMouth?: boolean;
  /** A tear below the eye. */
  tear?: boolean;
  /** A sweat drop beside the head. */
  sweat?: boolean;
  /** Pink cheeks. */
  blush?: boolean;
  /** Short strokes around the head, such as shaking or sparkle marks. */
  marks?: string;
  /** A large symbol above the head: '?', '!' or 'z'. */
  symbol?: string;
}

export const OPEN_MOUTH = 'M-6,5 Q2,1 10,5 Q12,17 2,17 Q-8,17 -6,5 Z';

/** Eyes for the shapes drawn as strokes rather than dots. */
export const EYE_PATHS = {
  happy: 'M-10,-3 Q-6,-10 -2,-3 M5,-3 Q9,-10 13,-3',
  closed: 'M-10,-5 Q-6,-1 -2,-5 M5,-5 Q9,-1 13,-5',
  half: 'M-10,-3 L-2,-3 M5,-3 L13,-3 M-10,-6 L-2,-5 M5,-5 L13,-6',
} as const;

export const TEAR_PATH = 'M-7,1 q-3,6 0,8 q3,-2 0,-8';
export const SWEAT_PATH = 'M18,-16 q-4,7 0,9 q4,-2 0,-9';

export const FACES: Record<Expression, Face> = {
  neutral: { mouth: 'M-4,9 L9,9' },
  happy: { eyes: 'happy', mouth: 'M-8,4 L12,4 Q12,17 2,17 Q-8,17 -8,4 Z', filledMouth: true },
  sad: {
    brows: 'M-11,-9 L-3,-14 M14,-9 L6,-14',
    mouth: 'M-7,15 Q2,3 11,15',
    tear: true,
  },
  angry: {
    brows: 'M-13,-15 L-1,-7 M16,-15 L4,-7',
    mouth: 'M-6,8 H10 V14 H-6 Z M-1,8 V14 M4,8 V14',
    marks: 'M-28,-20 l5,4 M-30,-12 l6,1 M30,-20 l-5,4 M32,-12 l-6,1',
  },
  surprised: {
    eyes: 'wide',
    brows: 'M-11,-15 Q-6,-19 -1,-15 M4,-15 Q9,-19 14,-15',
    mouth: 'M2,3 a5,7 0 1 0 .01,0 Z',
    filledMouth: true,
    symbol: '!',
  },
  scared: {
    eyes: 'wide',
    brows: 'M-11,-11 L-3,-15 M14,-11 L6,-15',
    mouth: 'M-7,11 q2,-4 4,0 t4,0 t4,0 t4,0',
    sweat: true,
    marks: 'M-27,-8 l-5,3 l5,3 M27,-8 l5,3 l-5,3',
  },
  confused: {
    brows: 'M-11,-10 L-3,-10 M5,-17 L13,-13',
    mouth: 'M-5,11 Q-1,7 3,11 T11,9',
    symbol: '?',
  },
  worried: {
    brows: 'M-11,-10 L-3,-14 M14,-10 L6,-14',
    mouth: 'M-5,12 q2.5,-3 5,0 t5,0',
    sweat: true,
  },
  excited: {
    eyes: 'wide',
    brows: 'M-11,-15 Q-6,-19 -1,-15 M4,-15 Q9,-19 14,-15',
    mouth: 'M-9,3 L13,3 Q13,18 2,18 Q-9,18 -9,3 Z',
    filledMouth: true,
    marks: 'M-26,-22 l-5,-5 M28,-22 l5,-5 M-30,-6 l-6,0 M32,-6 l6,0',
    symbol: '!',
  },
  proud: {
    eyes: 'closed',
    brows: 'M-11,-11 Q-6,-14 -1,-11 M4,-11 Q9,-14 14,-11',
    mouth: 'M-7,7 Q2,15 11,7',
    marks: 'M-27,-18 l-4,-4 M29,-18 l4,-4',
  },
  embarrassed: {
    brows: 'M-11,-10 L-3,-12 M14,-10 L6,-12',
    mouth: 'M-4,11 q2,-2 4,0 t4,0',
    blush: true,
    sweat: true,
  },
  determined: {
    brows: 'M-12,-12 L-1,-8 M15,-12 L4,-8',
    mouth: 'M-6,10 L11,9',
    marks: 'M-28,0 l-6,0 M30,0 l6,0',
  },
  tired: {
    eyes: 'half',
    mouth: 'M-2,10 a3,3 0 1 0 .01,0 Z',
    symbol: 'z',
  },
};

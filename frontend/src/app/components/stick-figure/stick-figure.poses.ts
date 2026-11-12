import { Expression, Pose, Stance } from '../../models/comic.models';

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
 * An animal on four legs, facing right: a level body from the hip (rear) to the shoulder (front),
 * with the head at the front. The "arms" are the front legs and the "legs" the back legs, so the
 * face, the features and the bubble tails work the same as for an upright figure. Poses that use
 * the arms (waving, pointing, cheering, fighting) lift a front paw.
 */
const QUAD = { head: [98, 100], shoulder: [80, 126], hip: [32, 128] } as const;
const QUAD_FRONT_LEGS = {
  backArm: [
    [78, 154],
    [76, 180],
  ],
  frontArm: [
    [86, 154],
    [88, 180],
  ],
} as const;
const QUAD_BACK_LEGS = {
  backLeg: [
    [30, 154],
    [28, 180],
  ],
  frontLeg: [
    [38, 154],
    [40, 180],
  ],
} as const;
const PAW_UP = {
  frontArm: [
    [96, 114],
    [110, 100],
  ],
} as const;

export const FOUR_LEGGED_SKELETONS: Record<Pose, Skeleton> = {
  standing: { ...QUAD, ...QUAD_FRONT_LEGS, ...QUAD_BACK_LEGS },
  walking: {
    ...QUAD,
    head: [100, 102],
    backArm: [
      [74, 154],
      [68, 180],
    ],
    frontArm: [
      [88, 154],
      [96, 180],
    ],
    backLeg: [
      [26, 154],
      [18, 180],
    ],
    frontLeg: [
      [40, 154],
      [46, 180],
    ],
  },
  running: {
    head: [102, 94],
    shoulder: [82, 120],
    hip: [30, 124],
    backArm: [
      [94, 140],
      [110, 148],
    ],
    frontArm: [
      [96, 146],
      [112, 160],
    ],
    backLeg: [
      [18, 140],
      [4, 152],
    ],
    frontLeg: [
      [26, 146],
      [16, 164],
    ],
    airborne: true,
  },
  waving: { ...QUAD, ...QUAD_FRONT_LEGS, ...QUAD_BACK_LEGS, ...PAW_UP },
  shouting: { ...QUAD, ...QUAD_FRONT_LEGS, ...QUAD_BACK_LEGS, head: [100, 96], openMouth: true },
  pointing: {
    ...QUAD,
    ...QUAD_FRONT_LEGS,
    ...QUAD_BACK_LEGS,
    frontArm: [
      [98, 130],
      [114, 128],
    ],
  },
  thinking: {
    ...QUAD,
    ...QUAD_FRONT_LEGS,
    ...QUAD_BACK_LEGS,
    head: [96, 102],
    frontArm: [
      [92, 130],
      [100, 120],
    ],
  },
  sitting: {
    head: [90, 96],
    shoulder: [74, 124],
    hip: [40, 162],
    backArm: [
      [72, 154],
      [72, 180],
    ],
    frontArm: [
      [80, 154],
      [82, 180],
    ],
    backLeg: [
      [56, 168],
      [66, 180],
    ],
    frontLeg: [
      [58, 172],
      [70, 180],
    ],
  },
  jumping: {
    head: [100, 72],
    shoulder: [80, 98],
    hip: [32, 104],
    backArm: [
      [86, 116],
      [96, 124],
    ],
    frontArm: [
      [92, 114],
      [104, 120],
    ],
    backLeg: [
      [22, 120],
      [30, 132],
    ],
    frontLeg: [
      [30, 122],
      [40, 134],
    ],
    airborne: true,
  },
  cheering: { ...QUAD, ...QUAD_FRONT_LEGS, ...QUAD_BACK_LEGS, ...PAW_UP, head: [100, 94] },
  fighting: {
    head: [96, 108],
    shoulder: [78, 130],
    hip: [32, 124],
    backArm: [
      [80, 156],
      [74, 180],
    ],
    frontArm: [
      [98, 118],
      [112, 110],
    ],
    backLeg: [
      [24, 152],
      [14, 180],
    ],
    frontLeg: [
      [40, 152],
      [46, 180],
    ],
  },
  falling: {
    head: [100, 164],
    shoulder: [80, 158],
    hip: [36, 150],
    backArm: [
      [76, 140],
      [72, 120],
    ],
    frontArm: [
      [86, 140],
      [90, 118],
    ],
    backLeg: [
      [26, 132],
      [30, 112],
    ],
    frontLeg: [
      [40, 132],
      [48, 114],
    ],
  },
};

/** The skeleton for a pose: an upright figure's, or a four-legged animal's. */
export function skeletonFor(pose: Pose, stance: Stance | undefined): Skeleton {
  const set = stance === 'four_legs' ? FOUR_LEGGED_SKELETONS : SKELETONS;
  return set[pose] ?? set.standing;
}

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

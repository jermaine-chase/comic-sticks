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

/** Face drawn around the head's centre at (0, 0), shifted towards the facing side. */
export interface Face {
  wideEyes?: boolean;
  brows?: string;
  mouth: string;
  filledMouth?: boolean;
  extra?: string;
}

export const OPEN_MOUTH = 'M-6,5 Q2,1 10,5 Q12,17 2,17 Q-8,17 -6,5 Z';

export const FACES: Record<Expression, Face> = {
  neutral: { mouth: 'M-4,9 L9,9' },
  happy: { mouth: 'M-7,6 Q2,17 11,6' },
  sad: {
    brows: 'M-10,-10 L-3,-12 M13,-10 L6,-12',
    mouth: 'M-5,12 Q2,5 9,12',
    extra: 'M-5,0 q-2.5,4 0,5 q2.5,-1 0,-5',
  },
  angry: { brows: 'M-11,-12 L-2,-7 M14,-12 L5,-7', mouth: 'M-5,11 Q2,6 9,11' },
  surprised: {
    wideEyes: true,
    brows: 'M-10,-13 L-3,-14 M6,-14 L13,-13',
    mouth: 'M2,4 a4,6 0 1 0 .01,0 Z',
  },
  scared: {
    wideEyes: true,
    mouth: 'M-6,10 q2,-4 4,0 t4,0 t4,0 t4,0',
    extra: 'M15,-14 q-3,5 0,6 q3,-1 0,-6',
  },
  confused: { brows: 'M-10,-10 L-3,-10 M6,-15 L13,-12', mouth: 'M-4,10 Q0,7 3,10 T10,8' },
};

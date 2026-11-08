/**
 * Mirrors the backend records in com.comicsticks.dto. The value lists match the Java enums
 * Pose, Expression, Facing, Scene and DialogueType.
 */

export const POSES = [
  'standing',
  'walking',
  'running',
  'waving',
  'shouting',
  'pointing',
  'thinking',
  'sitting',
  'jumping',
  'cheering',
  'fighting',
  'falling',
] as const;
export type Pose = (typeof POSES)[number];

export const EXPRESSIONS = [
  'neutral',
  'happy',
  'sad',
  'angry',
  'surprised',
  'scared',
  'confused',
] as const;
export type Expression = (typeof EXPRESSIONS)[number];

export type Facing = 'left' | 'right';
export type Scene = 'plain' | 'indoor' | 'outdoor' | 'night';
export type DialogueType = 'speech' | 'thought' | 'shout';

export interface ComicCharacter {
  name: string;
  pose: Pose;
  expression: Expression;
  facing: Facing;
}

export interface Dialogue {
  speaker: string;
  text: string;
  type: DialogueType;
}

export interface Panel {
  panelNumber: number;
  scene: Scene;
  caption: string;
  characters: ComicCharacter[];
  dialogue: Dialogue[];
  /** The book's own words for this panel, exactly as written. Empty when a later panel has them. */
  sourceText: string;
}

export interface ComicStory {
  title: string;
  panels: Panel[];
}

/** 1: a panel per paragraph. 2: a panel per 3 to 4 paragraphs. 3: a panel per scene. */
export type SupportLevel = 1 | 2 | 3;

export const SUPPORT_LEVELS: readonly {
  value: SupportLevel;
  label: string;
  description: string;
}[] = [
  { value: 1, label: 'Level 1', description: 'A picture for every paragraph.' },
  { value: 2, label: 'Level 2', description: 'A picture for every 3 or 4 paragraphs.' },
  { value: 3, label: 'Level 3', description: 'A picture for every scene.' },
];

/** Settings sent with each estimate and generate request. */
export interface ComicOptions {
  supportLevel: SupportLevel;
}

export const DEFAULT_OPTIONS: ComicOptions = { supportLevel: 1 };

export interface ComicEstimate {
  parts: number;
  panels: number;
  estimatedSeconds: number;
}

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
  'worried',
  'excited',
  'proud',
  'embarrassed',
  'determined',
  'tired',
] as const;
export type Expression = (typeof EXPRESSIONS)[number];

export type Facing = 'left' | 'right';
export type Scene = 'plain' | 'indoor' | 'outdoor' | 'night';

/** Speech, shout and whisper are words spoken in the text. A thought is drawn as a cloud. */
export const DIALOGUE_TYPES = ['speech', 'thought', 'shout', 'whisper'] as const;
export type DialogueType = (typeof DIALOGUE_TYPES)[number];

export interface ComicCharacter {
  name: string;
  pose: Pose;
  expression: Expression;
  facing: Facing;
  /** One plain word, such as "scared", shown as a tag near the figure. Empty when there is none. */
  feeling: string;
}

export interface Dialogue {
  speaker: string;
  text: string;
  type: DialogueType;
}

/** A "what it means" note. `phrase` appears in the panel's source text. */
export interface Note {
  phrase: string;
  meaning: string;
}

export interface Panel {
  panelNumber: number;
  scene: Scene;
  /** A scene-change box such as "Later that night...". Empty when time and place are unchanged. */
  caption: string;
  characters: ComicCharacter[];
  dialogue: Dialogue[];
  /** The book's own words for this panel, exactly as written. Empty when a later panel has them. */
  sourceText: string;
  notes: Note[];
  /** The name of a place on the sheet, whose background is drawn. Empty when none. */
  place: string;
}

export interface ComicStory {
  title: string;
  panels: Panel[];
  /** The character sheet every figure is drawn from. */
  characters: CharacterDesign[];
  places: Place[];
  /** The chapter's opening "Who's who" and closing "What happened". */
  bookends: ChapterBookends | null;
}

// --- Character sheet: matches the Java enums CharacterKind, Hat, Hair, Accessory, Ears,
// AnimalTail, Snout and Background. ---------------------------------------------------------

export const KINDS = ['person', 'animal'] as const;
export type CharacterKind = (typeof KINDS)[number];

export const HATS = [
  'none',
  'cap',
  'top_hat',
  'bowler',
  'crown',
  'witch_hat',
  'beanie',
  'straw_hat',
] as const;
export type Hat = (typeof HATS)[number];

export const HAIRS = ['none', 'short', 'spiky', 'long', 'curly', 'ponytail', 'bun'] as const;
export type Hair = (typeof HAIRS)[number];

export const ACCESSORIES = [
  'none',
  'bow',
  'glasses',
  'beard',
  'cape',
  'scarf',
  'pocket_watch',
  'bag',
  'whiskers',
] as const;
export type Accessory = (typeof ACCESSORIES)[number];

export const EARS = ['none', 'round', 'pointy', 'long', 'floppy'] as const;
export type Ears = (typeof EARS)[number];

export const TAILS = ['none', 'long', 'bushy', 'curly', 'short'] as const;
export type AnimalTail = (typeof TAILS)[number];

export const SNOUTS = ['none', 'short', 'long', 'beak'] as const;
export type Snout = (typeof SNOUTS)[number];

export const BACKGROUNDS = [
  'plain',
  'room',
  'burrow',
  'cave',
  'forest',
  'field',
  'riverbank',
  'garden',
  'town',
  'castle',
  'school',
  'beach',
  'boat',
] as const;
export type Background = (typeof BACKGROUNDS)[number];

/** A main character's fixed look. Every panel draws the character from this, by name. */
export interface CharacterDesign {
  name: string;
  /** One plain line, such as "Mole: a shy, kind animal who lives underground". */
  description: string;
  kind: CharacterKind;
  color: string;
  hat: Hat;
  hair: Hair;
  accessory: Accessory;
  ears: Ears;
  tail: AnimalTail;
  snout: Snout;
  /** Not on the reviewed sheet: a panel named them, so they got a plain default design. */
  added: boolean;
}

export interface Place {
  name: string;
  description: string;
  background: Background;
}

export interface CharacterSheet {
  characters: CharacterDesign[];
  places: Place[];
}

/** Turns an enum value such as "top_hat" into a label such as "top hat". */
export const label = (value: string) => value.replace(/_/g, ' ');

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
  /** One-word feeling tags near each figure. When off, the LLM is not asked for them. */
  feelingLabels: boolean;
  /** "What it means" notes for idioms and other tricky phrases. When off, they are not generated. */
  notes: boolean;
  /** Simple comprehension questions with picture answers at the end of each chapter. */
  questions: boolean;
}

export const DEFAULT_OPTIONS: ComicOptions = {
  supportLevel: 1,
  feelingLabels: true,
  notes: true,
  questions: false,
};

export interface ComicEstimate {
  parts: number;
  panels: number;
  estimatedSeconds: number;
}

// --- Chapter bookends ----------------------------------------------------------------------

/** What opens and closes a chapter. */
export interface ChapterBookends {
  whosWho: { characters: string[]; whereWeAre: string };
  /** 3 to 5 sentences. The last says how the main character feels at the end. */
  recap: RecapItem[];
  /** Empty when questions are off. */
  questions: Question[];
}

/** A recap sentence with its thumbnail: a panel, or a small figure of a character. */
export interface RecapItem {
  sentence: string;
  panelNumber: number | null;
  character: string | null;
  expression: Expression | null;
}

/** A question whose answers are pictures: characters or panels from the chapter. */
export interface Question {
  question: string;
  choices: AnswerChoice[];
  answerIndex: number;
}

export interface AnswerChoice {
  character: string | null;
  panelNumber: number | null;
}

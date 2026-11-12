import { Dialogue, Facing, Panel, Pose } from '../../models/comic.models';
import { SKELETONS } from '../stick-figure/stick-figure.poses';

/**
 * The fixed layout of every panel. The reader should never have to work out who is speaking:
 * figures stand in fixed slots, each bubble sits above its speaker, its tail ends at the speaker's
 * head, and a speaker turns to face the character they are talking to. The frontend enforces all
 * of this, whatever the LLM chose.
 */

/** Figure canvas size, matching the stick figure's viewBox. */
export const FIGURE_CANVAS = { width: 120, height: 190 } as const;
export const HEAD_RADIUS = 20;

export interface Point {
  x: number;
  y: number;
}

export interface Rect {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

/** Each figure's horizontal centre, as a percentage of the panel's width. */
export function slotCenter(index: number, count: number): number {
  return ((index + 0.5) / Math.max(count, 1)) * 100;
}

/**
 * Which way each figure faces. A speaker faces their listener: the nearest other speaker, or else
 * the nearest character. Someone who is not speaking faces the nearest speaker. With nobody
 * speaking, figures face the middle of the group. A figure on its own keeps its own facing.
 */
export function facings(panel: Pick<Panel, 'characters' | 'dialogue'>): Facing[] {
  const characters = panel.characters;
  if (characters.length < 2) {
    return characters.map((c) => c.facing);
  }
  const speakers = new Set(panel.dialogue.map((line) => line.speaker));
  const speakerIndexes = characters.flatMap((c, i) => (speakers.has(c.name) ? [i] : []));
  const middle = (characters.length - 1) / 2;

  return characters.map((character, i) => {
    const others = characters.map((_, j) => j).filter((j) => j !== i);
    const otherSpeakers = speakerIndexes.filter((j) => j !== i);
    let target: number;
    if (speakers.has(character.name)) {
      target = nearest(i, otherSpeakers.length ? otherSpeakers : others);
    } else if (speakerIndexes.length) {
      target = nearest(i, speakerIndexes);
    } else {
      if (i === middle) {
        return character.facing;
      }
      target = middle;
    }
    return target > i ? 'right' : 'left';
  });
}

function nearest(from: number, candidates: number[]): number {
  return candidates.reduce((best, j) => (Math.abs(j - from) < Math.abs(best - from) ? j : best));
}

/** Bubble width, as a share of the panel, by how many different characters speak in it. */
const BUBBLE_WIDTHS = [84, 84, 48, 32, 25];

/** Places a bubble over its speaker's slot, keeping it inside the panel. Values are percentages. */
export function bubbleLayout(panel: Pick<Panel, 'characters' | 'dialogue'>, line: Dialogue) {
  const count = Math.max(panel.characters.length, 1);
  const index = Math.max(
    panel.characters.findIndex((c) => c.name === line.speaker),
    0,
  );
  const distinctSpeakers = new Set(panel.dialogue.map((d) => d.speaker)).size;
  const width = BUBBLE_WIDTHS[distinctSpeakers] ?? BUBBLE_WIDTHS.at(-1)!;
  const center = slotCenter(index, count);
  const left = clamp(center - width / 2, 0, 100 - width);
  return { left, width };
}

/**
 * Whether a bubble gets its own tail. Consecutive lines from one speaker are joined instead, and
 * only the last of them points at the head, so no tail ever runs through another bubble.
 */
export function hasTail(dialogue: Dialogue[], index: number): boolean {
  const next = dialogue[index + 1];
  return !next || next.speaker !== dialogue[index].speaker;
}

/** The centre of a figure's head, in the coordinates of the rectangle the figure is drawn in. */
export function headCenter(figure: Rect, pose: Pose, facing: Facing): Point {
  const [hx, hy] = (SKELETONS[pose] ?? SKELETONS.standing).head;
  const x = facing === 'left' ? FIGURE_CANVAS.width - hx : hx;
  const scaleX = (figure.right - figure.left) / FIGURE_CANVAS.width;
  const scaleY = (figure.bottom - figure.top) / FIGURE_CANVAS.height;
  return { x: figure.left + x * scaleX, y: figure.top + hy * scaleY };
}

export interface Tail {
  /** A speech tail: an open path from the bubble's edge to just above the head. */
  path?: string;
  /** A thought trail: shrinking circles from the bubble towards the head. */
  circles?: { cx: number; cy: number; r: number }[];
}

const TAIL_HALF_WIDTH = 9;
const TAIL_GAP = 4;

/**
 * Draws the link between a bubble and its speaker's head. A speech tail starts on the bubble's
 * bottom edge, as close above the head as the bubble allows, and its tip stops just above the
 * head. A thought gets a trail of circles instead, so it can never be mistaken for speech.
 */
export function tailTo(bubble: Rect, head: Point, headRadius: number, thought: boolean): Tail {
  const baseX = clamp(
    head.x,
    bubble.left + TAIL_HALF_WIDTH + 6,
    bubble.right - TAIL_HALF_WIDTH - 6,
  );
  const baseY = bubble.bottom - 1.5;
  const tip = { x: head.x, y: head.y - headRadius - TAIL_GAP };
  if (thought) {
    const from = { x: baseX, y: bubble.bottom + 2 };
    const at = (t: number) => ({
      cx: round(from.x + (tip.x - from.x) * t),
      cy: round(from.y + (tip.y - from.y) * t),
    });
    return {
      circles: [
        { ...at(0.2), r: 7 },
        { ...at(0.55), r: 5 },
        { ...at(0.85), r: 3.5 },
      ],
    };
  }
  return {
    path:
      `M${round(baseX - TAIL_HALF_WIDTH)},${round(baseY)} ` +
      `L${round(tip.x)},${round(tip.y)} ` +
      `L${round(baseX + TAIL_HALF_WIDTH)},${round(baseY)}`,
  };
}

/** A short line joining a bubble to the next bubble from the same speaker. */
export function connector(upper: Rect, lower: Rect): string {
  const x = round((Math.max(upper.left, lower.left) + Math.min(upper.right, lower.right)) / 2);
  return `M${x},${round(upper.bottom)} L${x},${round(lower.top)}`;
}

const round = (n: number) => Math.round(n * 10) / 10;

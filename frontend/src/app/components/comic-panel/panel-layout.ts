import {
  Dialogue,
  Facing,
  HELD_THINGS,
  LARGE_THINGS,
  Panel,
  Pose,
  Thing,
} from '../../models/comic.models';
import { SKELETONS } from '../stick-figure/stick-figure.poses';
import { THING_ART } from '../things/things.drawings';

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

/** How far a widened bubble keeps from the middle of another speaker's tail, in percent. */
const TAIL_CLEARANCE = 7;

/**
 * Places the dialogue line at {@code lineIndex} over its speaker's slot, keeping it inside the
 * panel. Values are percentages. {@code minWidth} widens a bubble whose text does not fit (see
 * BUBBLE_FITS), but never across the tail of an earlier bubble from someone else: those tails run
 * down past this bubble to their speakers' heads.
 */
export function bubbleLayout(
  panel: Pick<Panel, 'characters' | 'dialogue'>,
  lineIndex: number,
  minWidth = 0,
) {
  const line = panel.dialogue[lineIndex];
  const count = Math.max(panel.characters.length, 1);
  const slotOf = (speaker: string) =>
    slotCenter(Math.max(panel.characters.findIndex((c) => c.name === speaker), 0), count);
  const distinctSpeakers = new Set(panel.dialogue.map((d) => d.speaker)).size;
  const base = BUBBLE_WIDTHS[distinctSpeakers] ?? BUBBLE_WIDTHS.at(-1)!;
  const center = slotOf(line.speaker);

  let lo = 0;
  let hi = 100;
  panel.dialogue.slice(0, lineIndex).forEach((earlier, j) => {
    if (earlier.speaker !== line.speaker && hasTail(panel.dialogue, j)) {
      const tail = slotOf(earlier.speaker);
      if (tail < center) {
        lo = Math.max(lo, tail + TAIL_CLEARANCE);
      } else if (tail > center) {
        hi = Math.min(hi, tail - TAIL_CLEARANCE);
      }
    }
  });

  const width = Math.max(base, Math.min(minWidth, hi - lo));
  const left =
    width > base
      ? clamp(center - width / 2, lo, hi - width)
      : clamp(center - width / 2, 0, 100 - width);
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

/**
 * How the bubbles are fitted into the panel, tried in order until every bubble shows all of its
 * text. Each bubble has a row of its own, so bubbles first grow wider (staying over their speaker
 * as far as they can), then the lettering shrinks, never below about 1rem. The last step lets the
 * bubble area scroll, so text is never hidden. The bubble area never moves down over the figures'
 * heads, where tails would cross the text.
 *
 * @param minWidth the narrowest a bubble may be, as a percentage of the panel's width
 * @param scale    the lettering size, as a share of the normal size
 * @param tight    whether the gaps between bubbles and the padding inside them are made smaller
 */
export interface BubbleFit {
  minWidth: number;
  scale: number;
  tight: boolean;
  scroll: boolean;
}

export const BUBBLE_FITS: readonly BubbleFit[] = [
  { minWidth: 0, scale: 1, tight: false, scroll: false },
  { minWidth: 66, scale: 1, tight: false, scroll: false },
  { minWidth: 96, scale: 1, tight: false, scroll: false },
  { minWidth: 96, scale: 1, tight: true, scroll: false },
  { minWidth: 96, scale: 0.92, tight: true, scroll: false },
  { minWidth: 96, scale: 0.85, tight: true, scroll: false },
  { minWidth: 96, scale: 0.85, tight: true, scroll: true },
];

/**
 * Whether any bubble's text is cut off: a bubble sticks out of the bubble area, or its text is
 * taller than the bubble itself. Half a pixel is allowed for rounding.
 */
export function bubblesOverflow(
  area: Rect,
  bubbles: { box: Rect; contentHeight: number; boxHeight: number }[],
): boolean {
  const slack = 0.5;
  return bubbles.some(
    ({ box, contentHeight, boxHeight }) =>
      box.top < area.top - slack ||
      box.bottom > area.bottom + slack ||
      box.left < area.left - slack ||
      box.right > area.right + slack ||
      contentHeight > boxHeight + slack,
  );
}

/** The next fit to try: one step further while text is cut off, else the same one. */
export function nextFit(step: number, overflowing: boolean): number {
  return overflowing ? Math.min(step + 1, BUBBLE_FITS.length - 1) : step;
}

/** Whether a thing can be held. Large things and animals never are. */
export function canBeHeld(thing: Thing): boolean {
  return (HELD_THINGS as readonly string[]).includes(thing);
}

/** The things a character holds: in the front hand, then the back hand. Never more than two. */
export function heldBy(panel: Pick<Panel, 'things'>, name: string): Thing[] {
  return (panel.things ?? [])
    .filter((t) => t.holder === name && canBeHeld(t.thing))
    .map((t) => t.thing)
    .slice(0, 2);
}

/**
 * A thing on the ground or in the scenery.
 *
 * @param left   its centre, as a percentage of the panel's width
 * @param width  as a percentage of the panel's width
 * @param behind large things stand behind the figures; everything else stands in front
 */
export interface ThingSpot {
  thing: Thing;
  left: number;
  width: number;
  behind: boolean;
}

/** Where loose things may stand, in the order they are tried, so the result is predictable. */
const GROUND_SPOTS = [6, 94, 50, 28, 72, 17, 83, 39, 61];

/**
 * Places the things nobody in the panel holds. Each goes in the spot that keeps it farthest from
 * the figures and from the things already placed, so nothing hides a figure. Large things go
 * first, so they get the edges, and stand behind the figures when there is no clear spot.
 */
export function groundThings(panel: Pick<Panel, 'characters' | 'things'>): ThingSpot[] {
  const names = new Set(panel.characters.map((c) => c.name));
  const isLarge = (thing: Thing) => (LARGE_THINGS as readonly string[]).includes(thing);
  const loose = (panel.things ?? [])
    .filter(
      (t) =>
        !(t.holder && names.has(t.holder) && canBeHeld(t.thing) && heldBy(panel, t.holder).includes(t.thing)),
    )
    .sort((a, b) => Number(isLarge(b.thing)) - Number(isLarge(a.thing)));
  const figures = panel.characters.map((_, i, all) => slotCenter(i, all.length));
  const placed: ThingSpot[] = [];
  for (const { thing } of loose) {
    const width = THING_ART[thing].size;
    const behind = isLarge(thing);
    let best = { left: 50, clearance: -Infinity };
    for (const spot of GROUND_SPOTS) {
      const left = clamp(spot, width / 2 + 2, 98 - width / 2);
      const others = [
        ...figures.map((f) => Math.abs(f - left) - width / 2),
        ...placed.map((p) => Math.abs(p.left - left) - (p.width + width) / 2),
      ];
      const clearance = others.length ? Math.min(...others) : Infinity;
      if (clearance > best.clearance) {
        best = { left, clearance };
      }
    }
    placed.push({ thing, left: round(best.left), width, behind });
  }
  return placed;
}

/** A short line joining a bubble to the next bubble from the same speaker. */
export function connector(upper: Rect, lower: Rect): string {
  const x = round((Math.max(upper.left, lower.left) + Math.min(upper.right, lower.right)) / 2);
  return `M${x},${round(upper.bottom)} L${x},${round(lower.top)}`;
}

const round = (n: number) => Math.round(n * 10) / 10;

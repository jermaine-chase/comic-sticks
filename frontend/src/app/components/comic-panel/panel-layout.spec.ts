import { ComicCharacter, Dialogue, PanelThing, THINGS } from '../../models/comic.models';
import { THING_ART } from '../things/things.drawings';
import {
  BUBBLE_FITS,
  bubbleLayout,
  bubblesOverflow,
  facings,
  groundThings,
  heldBy,
  hasTail,
  headCenter,
  nextFit,
  slotCenter,
  tailTo,
} from './panel-layout';

const figure = (name: string, facing: 'left' | 'right' = 'right'): ComicCharacter => ({
  name,
  pose: 'standing',
  expression: 'neutral',
  facing,
  feeling: '',
});
const says = (speaker: string, type: Dialogue['type'] = 'speech'): Dialogue => ({
  speaker,
  text: 'Hi.',
  type,
});

describe('panel layout', () => {
  describe('facings', () => {
    it('turns a speaker towards the character they talk to, whatever the LLM chose', () => {
      const characters = [figure('Mia', 'left'), figure('Leo', 'right')];

      expect(facings({ characters, dialogue: [says('Mia')] })).toEqual(['right', 'left']);
      expect(facings({ characters, dialogue: [says('Leo')] })).toEqual(['right', 'left']);
    });

    it('has a speaker face the other speaker rather than a nearer silent character', () => {
      const characters = [figure('Mia'), figure('Bob'), figure('Leo')];

      const result = facings({ characters, dialogue: [says('Mia'), says('Leo')] });

      expect(result[0]).toBe('right');
      expect(result[2]).toBe('left');
    });

    it('has silent characters face the nearest speaker', () => {
      const characters = [figure('Mia'), figure('Bob'), figure('Leo')];

      expect(facings({ characters, dialogue: [says('Mia')] })).toEqual(['right', 'left', 'left']);
    });

    it('turns a group with no speaker towards its middle', () => {
      const characters = [figure('A', 'left'), figure('B', 'left'), figure('C', 'right')];

      expect(facings({ characters, dialogue: [] })).toEqual(['right', 'left', 'left']);
    });

    it('leaves a character on their own as they are', () => {
      expect(facings({ characters: [figure('Mia', 'left')], dialogue: [says('Mia')] })).toEqual([
        'left',
      ]);
    });
  });

  it('puts figures in fixed, evenly spaced slots', () => {
    expect([0, 1, 2].map((i) => Math.round(slotCenter(i, 3)))).toEqual([17, 50, 83]);
  });

  it('places each bubble above its own speaker without crossing the other', () => {
    const panel = {
      characters: [figure('Mia'), figure('Leo')],
      dialogue: [says('Mia'), says('Leo')],
    };

    const mia = bubbleLayout(panel, 0);
    const leo = bubbleLayout(panel, 1);

    expect(mia.left + mia.width).toBeLessThanOrEqual(leo.left);
    expect(mia.left).toBeLessThan(25);
    expect(leo.left + leo.width).toBeGreaterThan(75);
  });

  it('gives a run of lines from one speaker a single tail, on the last', () => {
    const dialogue = [says('Mia'), says('Mia'), says('Leo')];

    expect(dialogue.map((_, i) => hasTail(dialogue, i))).toEqual([false, true, true]);
  });

  it('finds the head on a mirrored figure', () => {
    const box = { left: 0, top: 0, right: 120, bottom: 190 };

    expect(headCenter(box, 'running', 'right').x).toBe(76);
    expect(headCenter(box, 'running', 'left').x).toBe(44);
  });

  it('ends a speech tail just above the speaker head', () => {
    const bubble = { left: 10, top: 10, right: 210, bottom: 60 };
    const head = { x: 50, y: 150 };

    const tail = tailTo(bubble, head, 20, false);

    expect(tail.path).toBe('M41,58.5 L50,126 L59,58.5');
  });

  it('keeps the tail on the bubble when the head is beyond its edge', () => {
    const tail = tailTo(
      { left: 100, top: 0, right: 200, bottom: 50 },
      { x: 20, y: 150 },
      20,
      false,
    );

    expect(tail.path!.startsWith('M106,')).toBe(true);
    expect(tail.path).toContain('L20,126');
  });

  it('draws a thought as shrinking circles towards the head, never a pointed tail', () => {
    const tail = tailTo({ left: 0, top: 0, right: 200, bottom: 50 }, { x: 100, y: 200 }, 20, true);

    expect(tail.path).toBeUndefined();
    expect(tail.circles!.map((c) => c.r)).toEqual([7, 5, 3.5]);
    const ys = tail.circles!.map((c) => c.cy);
    expect(ys).toEqual([...ys].sort((a, b) => a - b));
    expect(ys.at(-1)!).toBeLessThan(200 - 20);
  });

  describe('things', () => {
    const panel = (things: PanelThing[], names = ['Mia', 'Leo']) => ({
      characters: names.map((n) => figure(n)),
      things,
    });

    it('puts held things in the holder’s hands, at most two, and never large things', () => {
      const p = panel([
        { thing: 'map', holder: 'Mia' },
        { thing: 'lamp', holder: 'Mia' },
        { thing: 'key', holder: 'Mia' },
        { thing: 'tree', holder: 'Mia' },
        { thing: 'dog', holder: 'Leo' },
      ]);

      expect(heldBy(p, 'Mia')).toEqual(['map', 'lamp']);
      expect(heldBy(p, 'Leo')).toEqual([]);
      expect(groundThings(p).map((s) => s.thing)).toEqual(['tree', 'key', 'dog']);
    });

    it('puts a thing on the ground when its holder is not in the panel', () => {
      const p = panel([{ thing: 'map', holder: 'Rat' }]);
      expect(groundThings(p).map((s) => s.thing)).toEqual(['map']);
    });

    it('keeps loose things clear of the figures and of each other, inside the panel', () => {
      const p = panel([
        { thing: 'dog', holder: '' },
        { thing: 'ball', holder: '' },
        { thing: 'cat', holder: '' },
      ]);
      const spots = groundThings(p);
      const figures = [25, 75];

      for (const spot of spots) {
        expect(spot.left - spot.width / 2).toBeGreaterThanOrEqual(0);
        expect(spot.left + spot.width / 2).toBeLessThanOrEqual(100);
        for (const f of figures) {
          expect(Math.abs(spot.left - f)).toBeGreaterThan(spot.width / 2);
        }
      }
      const sorted = [...spots].sort((a, b) => a.left - b.left);
      for (let i = 1; i < sorted.length; i++) {
        expect(sorted[i].left - sorted[i - 1].left).toBeGreaterThanOrEqual(
          (sorted[i].width + sorted[i - 1].width) / 2,
        );
      }
    });

    it('stands large things behind the figures and places them first', () => {
      const spots = groundThings(panel([{ thing: 'cat', holder: '' }, { thing: 'tree', holder: '' }]));

      expect(spots.map((s) => [s.thing, s.behind])).toEqual([
        ['tree', true],
        ['cat', false],
      ]);
    });

    it('places the same things in the same spots every time', () => {
      const p = panel([{ thing: 'dog', holder: '' }]);
      expect(groundThings(p)).toEqual(groundThings(p));
    });

    it('draws every thing', () => {
      for (const thing of THINGS) {
        expect(THING_ART[thing].shapes.length).toBeGreaterThan(0);
      }
    });
  });

  describe('bubble fitting', () => {
    const area = { left: 0, top: 40, right: 300, bottom: 200 };
    const bubble = (top: number, bottom: number, contentHeight = bottom - top) => ({
      box: { left: 10, top, right: 290, bottom },
      contentHeight,
      boxHeight: bottom - top,
    });

    it('sees no problem when every bubble is inside the area and shows all its text', () => {
      expect(bubblesOverflow(area, [bubble(60, 120), bubble(140, 200)])).toBe(false);
    });

    it('sees text cut off when a bubble sticks out above the area', () => {
      expect(bubblesOverflow(area, [bubble(10, 120), bubble(140, 200)])).toBe(true);
    });

    it('sees text cut off when it is taller than its own bubble', () => {
      expect(bubblesOverflow(area, [bubble(60, 120, 90)])).toBe(true);
    });

    it('widens bubbles before shrinking lettering a little, and never hides text', () => {
      const scales = BUBBLE_FITS.map((f) => f.scale);
      const widths = BUBBLE_FITS.map((f) => f.minWidth);
      expect(scales).toEqual([...scales].sort((a, b) => b - a));
      expect(widths).toEqual([...widths].sort((a, b) => a - b));
      expect(Math.min(...scales) * 1.15).toBeGreaterThanOrEqual(0.95);
      const firstSmaller = BUBBLE_FITS.findIndex((f) => f.scale < 1);
      expect(BUBBLE_FITS[firstSmaller - 1].minWidth).toBe(Math.max(...widths));
      expect(BUBBLE_FITS.at(-1)!.scroll).toBe(true);
    });

    it('widens a bubble but keeps it inside the panel', () => {
      const panel = {
        characters: [figure('Mia'), figure('Leo')],
        dialogue: [says('Leo'), says('Mia')],
      };
      expect(bubbleLayout(panel, 0)).toEqual({ left: 51, width: 48 });
      expect(bubbleLayout(panel, 0, 96)).toEqual({ left: 4, width: 96 });
    });

    it('never widens a bubble across the tail of an earlier bubble above it', () => {
      const panel = {
        characters: [figure('Leo'), figure('Mum')],
        dialogue: [says('Leo'), says('Mum')],
      };
      // Leo's tail runs down at 25% to his head, past Mum's bubble.
      const mum = bubbleLayout(panel, 1, 96);
      expect(mum.left).toBeGreaterThanOrEqual(25 + 7);
      expect(mum.width).toBeGreaterThan(48);
      expect(mum.left + mum.width).toBeLessThanOrEqual(100);
    });

    it('steps on only while text is cut off, and stops at the last step', () => {
      expect(nextFit(0, false)).toBe(0);
      expect(nextFit(0, true)).toBe(1);
      const last = BUBBLE_FITS.length - 1;
      expect(nextFit(last, true)).toBe(last);
    });
  });
});

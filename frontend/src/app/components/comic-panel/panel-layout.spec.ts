import { ComicCharacter, Dialogue } from '../../models/comic.models';
import { bubbleLayout, facings, hasTail, headCenter, slotCenter, tailTo } from './panel-layout';

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

    const mia = bubbleLayout(panel, panel.dialogue[0]);
    const leo = bubbleLayout(panel, panel.dialogue[1]);

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
});

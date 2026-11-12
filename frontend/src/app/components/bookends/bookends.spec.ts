import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CharacterDesign, Panel, Question, RecapItem } from '../../models/comic.models';
import { QuestionsComponent } from './questions.component';
import { RecapComponent } from './recap.component';
import { WhosWhoComponent } from './whos-who.component';

const MOLE: CharacterDesign = {
  name: 'Mole',
  description: 'Mole: a shy, kind animal who lives underground.',
  kind: 'animal',
  color: '#123456',
  hat: 'none',
  hair: 'none',
  accessory: 'scarf',
  ears: 'round',
  tail: 'short',
  snout: 'short',
  added: false,
};

const panel = (panelNumber: number, place = ''): Panel => ({
  panelNumber,
  scene: 'outdoor',
  caption: '',
  characters: [
    { name: 'Mole', pose: 'walking', expression: 'happy', facing: 'right', feeling: '' },
  ],
  dialogue: [{ speaker: 'Mole', text: 'Hello!', type: 'speech' }],
  sourceText: `Text of panel ${panelNumber}.`,
  notes: [],
  place,
});
const PANELS = [panel(1, 'The burrow'), panel(2), panel(3)];

async function render<T>(type: Type<T>, inputs: Record<string, unknown>) {
  const fixture = TestBed.createComponent(type);
  Object.entries(inputs).forEach(([key, value]) => fixture.componentRef.setInput(key, value));
  await fixture.whenStable();
  return { fixture, root: fixture.nativeElement as HTMLElement };
}

describe('chapter bookends', () => {
  it("who's who draws each character from the sheet with their name and description", async () => {
    const { root } = await render(WhosWhoComponent, {
      characters: ['Mole', 'Rat'],
      whereWeAre: 'We are at the burrow.',
      designs: [MOLE],
    });

    const members = root.querySelectorAll('.member');
    expect(members.length).toBe(2);
    expect(members[0].querySelector('.name')!.textContent).toBe('Mole');
    expect(members[0].querySelector('.about')!.textContent).toBe(
      'a shy, kind animal who lives underground.',
    );
    expect(members[0].querySelector('[data-feature="ears-round"]')).toBeTruthy();
    expect(members[1].querySelector('.about')).toBeNull();
    expect(root.querySelector('.where')!.textContent).toContain('We are at the burrow.');
  });

  it('the recap shows each line with its panel, and the ending with a figure', async () => {
    const recap: RecapItem[] = [
      { sentence: 'Mole left home.', panelNumber: 1, character: null, expression: null },
      {
        sentence: 'At the end, Mole feels proud.',
        panelNumber: null,
        character: 'Mole',
        expression: 'proud',
      },
    ];
    const { root } = await render(RecapComponent, {
      recap,
      panels: PANELS,
      designs: [MOLE],
      places: [{ name: 'The burrow', description: '', background: 'burrow' }],
    });

    const items = root.querySelectorAll('li');
    const thumbnail = items[0].querySelector('app-panel-thumbnail')!;
    expect(thumbnail.getAttribute('aria-label')).toBe('Picture 1: Mole');
    expect(thumbnail.querySelector('[data-background="burrow"]')).toBeTruthy();
    expect(thumbnail.querySelector('.bubble')).toBeNull();
    expect(items[1].classList).toContain('ending');
    expect(items[1].querySelector('svg')!.getAttribute('aria-label')).toBe(
      'Mole, standing, looking proud',
    );
  });

  it('questions give literal feedback and keep wrong choices marked', async () => {
    const questions: Question[] = [
      {
        question: 'Which picture shows Mole leaving home?',
        choices: [
          { character: null, panelNumber: 1 },
          { character: null, panelNumber: 3 },
        ],
        answerIndex: 0,
      },
    ];
    const { fixture, root } = await render(QuestionsComponent, { questions, panels: PANELS });
    const buttons = () => root.querySelectorAll<HTMLButtonElement>('.choice');

    expect(buttons()[1].getAttribute('aria-label')).toBe('Picture 3');
    buttons()[1].click();
    await fixture.whenStable();
    expect(root.querySelector('.feedback')!.textContent).toBe('Not this one. Try again.');

    buttons()[0].click();
    await fixture.whenStable();
    expect(root.querySelector('.feedback')!.textContent).toBe('Yes! It was picture 1.');
    expect(buttons()[0].classList).toContain('right');
    expect(buttons()[1].classList).toContain('wrong');
  });
});

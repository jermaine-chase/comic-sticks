import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Panel } from '../../models/comic.models';
import { ComicPanelComponent } from './comic-panel.component';

const PANEL: Panel = {
  panelNumber: 1,
  scene: 'outdoor',
  caption: 'Later that night...',
  characters: [
    { name: 'Mole', pose: 'standing', expression: 'scared', facing: 'left', feeling: 'scared' },
    { name: 'Rat', pose: 'pointing', expression: 'happy', facing: 'right', feeling: '' },
  ],
  dialogue: [
    { speaker: 'Rat', text: 'Over here!', type: 'shout' },
    { speaker: 'Mole', text: 'is anyone there?', type: 'whisper' },
    { speaker: 'Mole', text: 'I want to go home.', type: 'thought' },
  ],
  sourceText: '"Is anyone there?" whispered Mole. His heart sank.',
  notes: [{ phrase: 'His heart sank', meaning: 'He felt very disappointed.' }],
};

describe('ComicPanelComponent', () => {
  let fixture: ComponentFixture<ComicPanelComponent>;
  let root: HTMLElement;

  async function render(panel: Panel) {
    fixture = TestBed.createComponent(ComicPanelComponent);
    fixture.componentRef.setInput('panel', panel);
    await fixture.whenStable();
    root = fixture.nativeElement;
  }

  beforeEach(() => render(PANEL));

  it('draws each kind of bubble differently and names it for screen readers', () => {
    const bubbles = Array.from(root.querySelectorAll('.bubble'));

    expect(bubbles.map((b) => b.classList.contains('bubble-shout'))).toEqual([true, false, false]);
    expect(bubbles[1].classList).toContain('bubble-whisper');
    expect(bubbles[1].getAttribute('data-type-label')).toBe('whisper');
    expect(bubbles[2].classList).toContain('bubble-thought');
    expect(bubbles.map((b) => b.getAttribute('aria-label'))).toEqual([
      'Rat shouts',
      'Mole whispers',
      'Mole thinks',
    ]);
  });

  it('shows a feeling as a separate label, not as speech', () => {
    const feeling = root.querySelector('.feeling')!;

    expect(feeling.textContent!.trim()).toBe('scared');
    expect(feeling.closest('.bubbles')).toBeNull();
    expect(root.querySelectorAll('.feeling').length).toBe(1);
  });

  it('puts the scene change at the top and the text and notes under the panel', () => {
    expect(root.querySelector('.panel > .caption')!.textContent).toBe('Later that night...');
    expect(root.querySelector('.text-block .source-text')!.textContent).toBe(PANEL.sourceText);
    expect(root.querySelector('.notes dt')!.textContent).toContain('His heart sank');
    expect(root.querySelector('.notes dd')!.textContent).toBe('He felt very disappointed.');
  });

  it('turns the speakers to face each other', () => {
    const mirrored = Array.from(root.querySelectorAll('app-stick-figure svg > g')).map((g) =>
      (g.getAttribute('transform') ?? '').includes('scale(-1,1)'),
    );

    expect(mirrored).toEqual([false, true]);
  });

  it('leaves the text block out of a panel whose text is under a later panel', async () => {
    await render({ ...PANEL, sourceText: '', notes: [] });

    expect(root.querySelector('.text-block')).toBeNull();
  });

  it('offers no editing when read only', async () => {
    fixture.componentRef.setInput('editable', false);
    await fixture.whenStable();

    expect(root.querySelector('[contenteditable]')).toBeNull();
    expect(root.querySelector('.actor-controls')).toBeNull();
  });
});

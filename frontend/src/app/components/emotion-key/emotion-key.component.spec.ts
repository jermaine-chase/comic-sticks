import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EXPRESSIONS } from '../../models/comic.models';
import { EMOTION_CLUES, EmotionKeyComponent } from './emotion-key.component';

describe('EmotionKeyComponent', () => {
  let fixture: ComponentFixture<EmotionKeyComponent>;
  let root: HTMLElement;

  async function render(used: string[]) {
    fixture = TestBed.createComponent(EmotionKeyComponent);
    fixture.componentRef.setInput('used', used);
    await fixture.whenStable();
    root = fixture.nativeElement;
  }

  const expressionsIn = (selector: string) =>
    Array.from(root.querySelectorAll(selector)).map((li) => li.getAttribute('data-expression'));

  it('explains every face the comic can draw', () => {
    for (const expression of EXPRESSIONS) {
      expect(EMOTION_CLUES[expression].meaning).toBeTruthy();
      expect(EMOTION_CLUES[expression].clues).toBeTruthy();
    }
  });

  it('shows each face once, this comic’s first in the order they appear', async () => {
    await render(['scared', 'happy', 'scared']);

    expect(expressionsIn('li.in-comic')).toEqual(['scared', 'happy']);
    const all = expressionsIn('li');
    expect(all.length).toBe(EXPRESSIONS.length);
    expect(new Set(all).size).toBe(EXPRESSIONS.length);
    expect(root.textContent).toContain('Faces in this comic');
  });

  it('shows one plain list when the comic has no faces yet', async () => {
    await render([]);

    expect(root.querySelectorAll('h3').length).toBe(0);
    expect(expressionsIn('li').length).toBe(EXPRESSIONS.length);
  });

  it('draws each face as a close-up by the same stick figure the panels use', async () => {
    await render([]);
    const happy = root.querySelector('li[data-expression="happy"] app-stick-figure svg')!;

    expect(happy.getAttribute('viewBox')).not.toBe('0 0 120 190');
    expect(happy.getAttribute('aria-label')).toContain('happy');
    expect(root.querySelector('li[data-expression="happy"]')!.textContent).toContain('Feels good.');
  });

  it('closes from its button, from Escape and from the backdrop, but not from inside', async () => {
    await render([]);
    let closed = 0;
    fixture.componentInstance.close.subscribe(() => closed++);

    root.querySelector<HTMLButtonElement>('.close')!.click();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    root.click();
    root.querySelector<HTMLElement>('.key')!.click();

    expect(closed).toBe(3);
  });
});

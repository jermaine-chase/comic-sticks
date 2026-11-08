import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CharacterDesign, CharacterSheet } from '../../models/comic.models';
import { CharacterSheetComponent } from './character-sheet.component';

const MOLE: CharacterDesign = {
  name: 'Mole',
  description: 'Mole: a shy, kind animal who lives underground.',
  kind: 'animal',
  color: '#1b1b1f',
  hat: 'none',
  hair: 'none',
  accessory: 'scarf',
  ears: 'round',
  tail: 'short',
  snout: 'short',
  added: false,
};
const SHEET: CharacterSheet = {
  characters: [MOLE, { ...MOLE, name: 'Guard', kind: 'person', ears: 'none', added: true }],
  places: [{ name: 'The burrow', description: 'Underground.', background: 'burrow' }],
};

describe('CharacterSheetComponent', () => {
  let fixture: ComponentFixture<CharacterSheetComponent>;
  let root: HTMLElement;
  let emitted: CharacterSheet[];

  beforeEach(async () => {
    fixture = TestBed.createComponent(CharacterSheetComponent);
    fixture.componentRef.setInput('sheet', SHEET);
    emitted = [];
    fixture.componentInstance.sheetChange.subscribe((sheet) => emitted.push(sheet));
    await fixture.whenStable();
    root = fixture.nativeElement;
  });

  function change(selector: string, value: string, index = 0) {
    const field = root.querySelectorAll<HTMLInputElement | HTMLSelectElement>(selector)[index];
    field.value = value;
    field.dispatchEvent(new Event('change'));
  }

  it('previews each character exactly as the panels will draw them', () => {
    const previews = root.querySelectorAll('.preview');

    expect(previews.length).toBe(2);
    expect(previews[0].querySelector('[data-feature="ears-round"]')).toBeTruthy();
    expect(root.querySelector('[data-background="burrow"]')).toBeTruthy();
  });

  it('shows animal parts only for animals, and flags characters added while drawing', () => {
    const cards = root.querySelectorAll('.card:not(.place-card)');

    expect(cards[0].querySelector('select[name="ears"]')).toBeTruthy();
    expect(cards[1].querySelector('select[name="ears"]')).toBeNull();
    expect(cards[1].classList).toContain('card-added');
    expect(cards[1].textContent).toContain('Check this look');
  });

  it('emits the edited sheet', () => {
    change('select[name="hat"]', 'top_hat');
    change('input[name="character-name"]', 'Moley');
    change('select[name="background"]', 'cave');

    expect(emitted[0].characters[0].hat).toBe('top_hat');
    expect(emitted[1].characters[0].name).toBe('Moley');
    expect(emitted[2].places[0].background).toBe('cave');
  });

  it('removes animal parts when a character becomes a person, and confirms an added one', () => {
    change('select[name="kind"]', 'person');
    change('select[name="hair"]', 'curly', 1);

    expect(emitted[0].characters[0]).toMatchObject({ kind: 'person', ears: 'none', tail: 'none' });
    expect(emitted[1].characters[1].added).toBe(false);
  });

  it('adds and removes characters and places', () => {
    (root.querySelector('button[aria-label="Remove Mole"]') as HTMLButtonElement).click();
    Array.from(root.querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Add a place'))!
      .click();

    expect(emitted[0].characters.map((c) => c.name)).toEqual(['Guard']);
    expect(emitted[1].places.length).toBe(2);
  });
});

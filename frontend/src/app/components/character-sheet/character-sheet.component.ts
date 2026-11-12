import { Component, input, output } from '@angular/core';
import {
  ACCESSORIES,
  BACKGROUNDS,
  CharacterDesign,
  CharacterSheet,
  EARS,
  HAIRS,
  HATS,
  KINDS,
  label,
  Place,
  SNOUTS,
  SPECIES,
  SPECIES_DEFAULTS,
  STANCES,
  TAILS,
} from '../../models/comic.models';
import { PanelBackgroundComponent } from '../comic-panel/panel-background.component';
import { StickFigureComponent } from '../stick-figure/stick-figure.component';

const NEW_CHARACTER: CharacterDesign = {
  name: '',
  description: '',
  kind: 'person',
  color: '#1b1b1f',
  hat: 'none',
  hair: 'short',
  accessory: 'none',
  ears: 'none',
  tail: 'none',
  snout: 'none',
  added: false,
};

/**
 * The book's character sheet and places, shown for review before any panel is drawn. Each change
 * emits a whole new sheet, and the preview figure shows exactly how the character will look in
 * every panel.
 */
@Component({
  selector: 'app-character-sheet',
  imports: [StickFigureComponent, PanelBackgroundComponent],
  templateUrl: './character-sheet.component.html',
  styleUrl: './character-sheet.component.css',
})
export class CharacterSheetComponent {
  readonly sheet = input.required<CharacterSheet>();
  readonly sheetChange = output<CharacterSheet>();

  protected readonly label = label;
  protected readonly kinds = KINDS;
  protected readonly hats = HATS;
  protected readonly hairs = HAIRS;
  protected readonly accessories = ACCESSORIES;
  protected readonly ears = EARS;
  protected readonly tails = TAILS;
  protected readonly snouts = SNOUTS;
  protected readonly backgrounds = BACKGROUNDS;
  protected readonly species = SPECIES;
  protected readonly stances = STANCES;

  protected updateCharacter(index: number, change: Partial<CharacterDesign>): void {
    const characters = this.sheet().characters.map((c, i) => {
      if (i !== index) {
        return c;
      }
      const updated = { ...c, ...change, added: false };
      // A person has no animal parts; an animal gets some to start from.
      if (change.kind === 'person') {
        return {
          ...updated,
          ears: 'none',
          tail: 'none',
          snout: 'none',
          species: 'none',
          stance: 'upright',
        } as CharacterDesign;
      }
      // Choosing an animal gives it that animal's parts and stance, which can then be changed.
      if (change.species && change.species !== 'none') {
        return { ...updated, ...SPECIES_DEFAULTS[change.species] };
      }
      if (change.kind === 'animal' && updated.ears === 'none' && updated.snout === 'none') {
        return { ...updated, ears: 'round', snout: 'short', tail: 'short' } as CharacterDesign;
      }
      return updated;
    });
    this.sheetChange.emit({ ...this.sheet(), characters });
  }

  protected addCharacter(): void {
    const name = `Character ${this.sheet().characters.length + 1}`;
    this.sheetChange.emit({
      ...this.sheet(),
      characters: [...this.sheet().characters, { ...NEW_CHARACTER, name }],
    });
  }

  protected removeCharacter(index: number): void {
    this.sheetChange.emit({
      ...this.sheet(),
      characters: this.sheet().characters.filter((_, i) => i !== index),
    });
  }

  protected updatePlace(index: number, change: Partial<Place>): void {
    this.sheetChange.emit({
      ...this.sheet(),
      places: this.sheet().places.map((p, i) => (i === index ? { ...p, ...change } : p)),
    });
  }

  protected addPlace(): void {
    const place: Place = {
      name: `Place ${this.sheet().places.length + 1}`,
      description: '',
      background: 'plain',
    };
    this.sheetChange.emit({ ...this.sheet(), places: [...this.sheet().places, place] });
  }

  protected removePlace(index: number): void {
    this.sheetChange.emit({
      ...this.sheet(),
      places: this.sheet().places.filter((_, i) => i !== index),
    });
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLSelectElement).value;
  }
}

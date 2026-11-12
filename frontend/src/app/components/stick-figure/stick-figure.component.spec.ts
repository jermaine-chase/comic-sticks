import { TestBed } from '@angular/core/testing';
import {
  ACCESSORIES,
  CharacterDesign,
  EARS,
  EXPRESSIONS,
  HAIRS,
  HATS,
  POSES,
  SNOUTS,
  SPECIES,
  SPECIES_DEFAULTS,
  TAILS,
} from '../../models/comic.models';
import { featureLayers, SPECIES_SKIN } from './stick-figure.features';
import { StickFigureComponent } from './stick-figure.component';
import { FACES, FOUR_LEGGED_SKELETONS, skeletonFor, SKELETONS } from './stick-figure.poses';

describe('StickFigureComponent', () => {
  function render(inputs: Record<string, unknown>) {
    const fixture = TestBed.createComponent(StickFigureComponent);
    Object.entries(inputs).forEach(([key, value]) => fixture.componentRef.setInput(key, value));
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  describe('animals', () => {
    const DOG: CharacterDesign = {
      name: 'Rex',
      description: '',
      kind: 'animal',
      color: '#1b1b1f',
      hat: 'none',
      hair: 'none',
      accessory: 'none',
      ears: 'floppy',
      tail: 'short',
      snout: 'short',
      added: false,
      species: 'dog',
      stance: 'four_legs',
    };
    const head = (root: HTMLElement) => root.querySelector('svg > g > g[transform]')!.getAttribute('transform');

    it('has a four-legged drawing for every pose', () => {
      POSES.forEach((pose) => {
        const s = FOUR_LEGGED_SKELETONS[pose];
        expect(s).toBeTruthy();
        // A level body: the hip is behind the shoulder, not under it.
        expect(s.shoulder[0] - s.hip[0]).toBeGreaterThan(25);
      });
    });

    it('draws an animal on four legs only when its sheet says so', () => {
      expect(head(render({ design: DOG }))).toBe('translate(98,100)');
      expect(head(render({ design: { ...DOG, stance: 'upright' } }))).toBe('translate(60,34)');
      expect(head(render({ design: { ...DOG, kind: 'person' } }))).toBe('translate(60,34)');
      expect(skeletonFor('waving', 'four_legs').frontArm[1][1]).toBeLessThan(
        skeletonFor('standing', 'four_legs').frontArm[1][1],
      );
    });

    it('colours an animal’s head by its species and adds its telling mark', () => {
      const lion = render({ design: { ...DOG, species: 'lion' } });

      expect(lion.querySelector('circle[r="20"]')!.getAttribute('fill')).toBe(SPECIES_SKIN.lion);
      expect(lion.querySelector('[data-feature="species-mane"]')).toBeTruthy();
      expect(render({}).querySelector('circle[r="20"]')!.getAttribute('fill')).toBe('#fffdf6');
    });

    it('has a skin and default parts for every species', () => {
      SPECIES.forEach((species) => {
        expect(SPECIES_SKIN[species]).toMatch(/^#[0-9a-f]{6}$/);
        expect(SPECIES_DEFAULTS[species]).toBeTruthy();
      });
    });
  });

  it('has a drawing for every pose and expression', () => {
    POSES.forEach((pose) => expect(SKELETONS[pose]).toBeTruthy());
    EXPRESSIONS.forEach((expression) => expect(FACES[expression]).toBeTruthy());
  });

  it('describes itself to screen readers', () => {
    const svg = render({ name: 'Mia', pose: 'waving', expression: 'happy' }).querySelector('svg');

    expect(svg?.getAttribute('aria-label')).toBe('Mia, waving, looking happy');
  });

  it('draws a different body for a different pose', () => {
    const body = (pose: string) =>
      render({ pose }).querySelector('svg > g > path')?.getAttribute('d');

    expect(body('waving')).not.toBe(body('standing'));
  });

  it('mirrors the figure when facing left', () => {
    const group = render({ facing: 'left' }).querySelector('svg > g');

    expect(group?.getAttribute('transform')).toContain('scale(-1,1)');
  });

  describe('with a character-sheet design', () => {
    const person: CharacterDesign = {
      name: 'Toad',
      description: '',
      kind: 'person',
      color: '#1c8a4a',
      hat: 'top_hat',
      hair: 'none',
      accessory: 'pocket_watch',
      ears: 'none',
      tail: 'none',
      snout: 'none',
      added: false,
    };
    const features = (root: HTMLElement) =>
      Array.from(root.querySelectorAll('[data-feature]')).map((e) =>
        e.getAttribute('data-feature'),
      );

    it('draws the hat and accessory in the design colour', () => {
      const root = render({ design: person, color: '#000000' });

      expect(features(root)).toContain('hat-top_hat');
      expect(features(root)).toContain('accessory-pocket_watch');
      expect(root.querySelector('svg > g')!.getAttribute('stroke')).toBe('#1c8a4a');
    });

    it('draws an animal with its ears, tail and snout in the right layers', () => {
      const root = render({
        design: {
          ...person,
          kind: 'animal',
          hat: 'none',
          ears: 'long',
          tail: 'bushy',
          snout: 'beak',
        },
      });

      expect(root.querySelector('.features-head-back [data-feature="ears-long"]')).toBeTruthy();
      expect(root.querySelector('.features-back [data-feature="tail-bushy"]')).toBeTruthy();
      expect(root.querySelector('.features-head-front [data-feature="snout-beak"]')).toBeTruthy();
    });

    it('draws the same features in every pose', () => {
      const names = (pose: string) => features(render({ design: person, pose }));

      expect(names('running')).toEqual(names('sitting'));
    });

    it('draws no features without a design', () => {
      expect(features(render({ pose: 'waving' }))).toEqual([]);
    });

    it('has a drawing for every feature value', () => {
      const skeleton = SKELETONS.standing;
      const count = (change: Partial<CharacterDesign>) => {
        const layers = featureLayers(
          { ...person, hat: 'none', accessory: 'none', ...change },
          skeleton,
        );
        return Object.values(layers).flat().length;
      };
      HATS.filter((v) => v !== 'none').forEach((hat) => expect(count({ hat })).toBeGreaterThan(0));
      HAIRS.filter((v) => v !== 'none').forEach((hair) =>
        expect(count({ hair })).toBeGreaterThan(0),
      );
      ACCESSORIES.filter((v) => v !== 'none').forEach((accessory) =>
        expect(count({ accessory })).toBeGreaterThan(0),
      );
      EARS.filter((v) => v !== 'none').forEach((ears) =>
        expect(count({ ears })).toBeGreaterThan(0),
      );
      TAILS.filter((v) => v !== 'none').forEach((tail) =>
        expect(count({ tail })).toBeGreaterThan(0),
      );
      SNOUTS.filter((v) => v !== 'none').forEach((snout) =>
        expect(count({ snout })).toBeGreaterThan(0),
      );
    });
  });

  it('falls back to standing for an unknown pose', () => {
    const body = (pose: string) =>
      render({ pose }).querySelector('svg > g > path')?.getAttribute('d');

    expect(body('moonwalking')).toBe(body('standing'));
  });
});

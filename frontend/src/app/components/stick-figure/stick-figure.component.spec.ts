import { TestBed } from '@angular/core/testing';
import { EXPRESSIONS, POSES } from '../../models/comic.models';
import { StickFigureComponent } from './stick-figure.component';
import { FACES, SKELETONS } from './stick-figure.poses';

describe('StickFigureComponent', () => {
  function render(inputs: Record<string, unknown>) {
    const fixture = TestBed.createComponent(StickFigureComponent);
    Object.entries(inputs).forEach(([key, value]) => fixture.componentRef.setInput(key, value));
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

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

  it('falls back to standing for an unknown pose', () => {
    const body = (pose: string) =>
      render({ pose }).querySelector('svg > g > path')?.getAttribute('d');

    expect(body('moonwalking')).toBe(body('standing'));
  });
});

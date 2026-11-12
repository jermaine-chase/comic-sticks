import { TestBed } from '@angular/core/testing';
import { BACKGROUNDS, Background } from '../../models/comic.models';
import { BACKGROUND_ART, FLOOR_Y } from './backgrounds';
import { PanelBackgroundComponent } from './panel-background.component';

describe('PanelBackgroundComponent', () => {
  function render(background: Background, night = false) {
    const fixture = TestBed.createComponent(PanelBackgroundComponent);
    fixture.componentRef.setInput('background', background);
    fixture.componentRef.setInput('night', night);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has a drawing for every background', () => {
    for (const background of BACKGROUNDS) {
      expect(BACKGROUND_ART[background].shapes.length).toBeGreaterThan(2);
    }
  });

  it('marks every scene’s floor at the same height, so figures always stand on it', () => {
    for (const background of BACKGROUNDS) {
      const floorLine = BACKGROUND_ART[background].shapes.some(
        (s) => s.kind === 'path' && s.fill === 'none' && s.d?.includes(`,${FLOOR_Y} H`),
      );
      expect(floorLine, background).toBe(true);
    }
  });

  it('darkens the sky and shows the moon and stars outdoors at night', () => {
    const day = render('field');
    const night = render('field', true);

    expect(day.querySelector('.moon')).toBeNull();
    expect(night.querySelector('.moon')).toBeTruthy();
    expect(night.querySelectorAll('.star').length).toBeGreaterThan(0);
    expect(night.querySelector('rect')!.getAttribute('fill')).not.toBe(
      day.querySelector('rect')!.getAttribute('fill'),
    );
  });

  it('hides the sun at night and keeps the moon out of rooms', () => {
    const sun = (root: HTMLElement) =>
      Array.from(root.querySelectorAll('circle')).filter((c) => c.getAttribute('fill') === '#ffe489');

    expect(sun(render('field')).length).toBe(1);
    expect(sun(render('field', true)).length).toBe(0);
    expect(render('room', true).querySelector('.moon')).toBeNull();
  });

  it('lights house windows at night', () => {
    const lit = (root: HTMLElement) =>
      root.querySelectorAll('rect[fill="#ffe7a0"]').length;

    expect(lit(render('town'))).toBe(0);
    expect(lit(render('town', true))).toBeGreaterThan(0);
  });
});

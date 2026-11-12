import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  CharacterDesign,
  CharacterSheet,
  ComicEstimate,
  ComicStory,
} from '../../models/comic.models';
import { ComicContainerComponent } from './comic-container.component';

const STORY: ComicStory = {
  title: 'The Map',
  panels: [
    {
      panelNumber: 1,
      scene: 'indoor',
      caption: 'In the attic.',
      characters: [
        {
          name: 'Mia',
          pose: 'pointing',
          expression: 'surprised',
          facing: 'right',
          feeling: 'surprised',
        },
        { name: 'Leo', pose: 'standing', expression: 'neutral', facing: 'left', feeling: '' },
      ],
      dialogue: [{ speaker: 'Mia', text: '<b>Look!</b>', type: 'shout' }],
      sourceText: '',
      notes: [],
      place: '',
    },
    {
      panelNumber: 2,
      scene: 'plain',
      caption: '',
      characters: [
        {
          name: 'Leo',
          pose: 'thinking',
          expression: 'confused',
          facing: 'right',
          feeling: 'confused',
        },
      ],
      dialogue: [{ speaker: 'Leo', text: 'Hmm.', type: 'thought' }],
      sourceText: 'Mia pointed.\n"<b>Look!</b>" she shouted. Leo wondered.',
      notes: [{ phrase: 'Leo wondered', meaning: 'Leo was not sure, and thought about it.' }],
      place: 'The attic',
    },
  ],
  characters: [],
  places: [{ name: 'The attic', description: 'A dusty room.', background: 'room' }],
  bookends: {
    whosWho: { characters: ['Mia', 'Leo'], whereWeAre: 'We are in the attic.' },
    recap: [
      { sentence: 'Mia found a map.', panelNumber: 2, character: null, expression: null },
      {
        sentence: 'At the end, Mia feels surprised.',
        panelNumber: null,
        character: 'Mia',
        expression: 'surprised',
      },
    ],
    questions: [
      {
        question: 'Who shouted?',
        choices: [
          { character: 'Leo', panelNumber: null },
          { character: 'Mia', panelNumber: null },
        ],
        answerIndex: 1,
      },
    ],
  },
};

const MIA: CharacterDesign = {
  name: 'Mia',
  description: 'Mia: a girl who loves maps.',
  kind: 'person',
  color: '#c8321f',
  hat: 'cap',
  hair: 'none',
  accessory: 'glasses',
  ears: 'none',
  tail: 'none',
  snout: 'none',
  added: false,
};
const SHEET: CharacterSheet = {
  characters: [MIA, { ...MIA, name: 'Leo', color: '#1f5fbf', hat: 'none', hair: 'curly' }],
  places: STORY.places,
};
STORY.characters = SHEET.characters;

const ESTIMATE: ComicEstimate = { parts: 3, panels: 8, estimatedSeconds: 41 };

describe('ComicContainerComponent', () => {
  let fixture: ComponentFixture<ComicContainerComponent>;
  let http: HttpTestingController;
  let root: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ComicContainerComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(ComicContainerComponent);
    http = TestBed.inject(HttpTestingController);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function typeStory(text: string) {
    const textarea = root.querySelector('textarea')!;
    textarea.value = text;
    textarea.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  /** Submits the form and answers the estimate request, stopping at the confirmation. */
  async function reachConfirmation() {
    await typeStory('Mia found a map.');
    root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    http.expectOne('/api/comics/estimate').flush(ESTIMATE);
    await fixture.whenStable();
  }

  function clickButton(label: string) {
    const button = Array.from(root.querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.trim() === label,
    );
    button!.click();
  }

  /** Confirms the estimate and answers the character pass, stopping at the sheet review. */
  async function reachSheet() {
    await reachConfirmation();
    clickButton('Find the characters');
    http.expectOne('/api/comics/characters').flush(SHEET);
    await fixture.whenStable();
  }

  async function submitStory() {
    await reachSheet();
    clickButton('Start drawing');
    return http.expectOne('/api/comics/generate');
  }

  async function renderStory() {
    (await submitStory()).flush(STORY);
    await fixture.whenStable();
  }

  async function editText(element: HTMLElement, text: string) {
    element.textContent = text;
    element.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  it('posts the story and renders every panel', async () => {
    const request = await submitStory();
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      text: 'Mia found a map.',
      supportLevel: 1,
      feelingLabels: true,
      notes: true,
      questions: false,
      things: true,
      characterSheet: SHEET,
    });

    request.flush(STORY);
    await fixture.whenStable();

    expect(root.querySelector('.comic-grid')).toBeTruthy();
    expect(root.querySelectorAll('.panel').length).toBe(2);
    expect(root.querySelectorAll('.panel app-stick-figure').length).toBe(3);
    expect(root.querySelector('.bubble-shout')).toBeTruthy();
    expect(root.querySelector('.bubble-thought')).toBeTruthy();
  });

  it('opens the chapter with who is who and closes it with what happened and questions', async () => {
    await renderStory();

    const grid = root.querySelector('.comic-grid')!;
    expect(grid.firstElementChild!.tagName.toLowerCase()).toBe('app-whos-who');
    expect(grid.querySelector('app-whos-who')!.textContent).toContain('We are in the attic.');
    const recap = root.querySelector('app-recap')!;
    expect(recap.textContent).toContain('At the end, Mia feels surprised.');
    expect(recap.querySelector('app-panel-thumbnail')).toBeTruthy();
    expect(root.querySelector('app-questions')!.textContent).toContain('Who shouted?');
  });

  it('hides and shows the feeling words without drawing the comic again', async () => {
    await renderStory();
    const toggle = root.querySelector<HTMLInputElement>('input[name="show-feelings"]')!;
    expect(root.querySelectorAll('.panel .feeling').length).toBe(2);

    toggle.click();
    await fixture.whenStable();

    expect(root.querySelectorAll('.panel .feeling').length).toBe(0);
    // The faces still show the feeling.
    expect(root.querySelector('.panel app-stick-figure svg')!.getAttribute('aria-label')).toContain(
      'surprised',
    );

    toggle.click();
    await fixture.whenStable();
    expect(root.querySelectorAll('.panel .feeling').length).toBe(2);
  });

  it('opens the emotion key with this comic’s faces first, and closes it with Escape', async () => {
    await renderStory();
    clickButton('Emotion key: what each face means');
    await fixture.whenStable();

    const key = root.querySelector('app-emotion-key')!;
    expect(key.querySelector('[role="dialog"]')).toBeTruthy();
    const first = Array.from(key.querySelectorAll('li.in-comic')).map((li) =>
      li.getAttribute('data-expression'),
    );
    expect(first).toEqual(['surprised', 'neutral', 'confused']);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await fixture.whenStable();
    expect(root.querySelector('app-emotion-key')).toBeNull();
  });

  it('shows the estimate and calls nothing further until confirmed', async () => {
    await reachConfirmation();

    const confirm = root.querySelector('.confirm')!.textContent!.replace(/\s+/g, ' ');
    expect(confirm).toContain('about 40 seconds');
    expect(confirm).toContain('3 parts');
    expect(confirm).toContain('up to 8 panels');
    http.expectNone('/api/comics/generate');
  });

  it('sends the chosen support level and drops a stale estimate when it changes', async () => {
    await reachConfirmation();
    const levelTwo = root.querySelector<HTMLInputElement>(
      'input[name="support-level"][value="2"]',
    )!;

    levelTwo.click();
    await fixture.whenStable();
    expect(root.querySelector('.confirm')).toBeNull();

    root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    const request = http.expectOne('/api/comics/estimate');
    expect(request.request.body).toMatchObject({ text: 'Mia found a map.', supportLevel: 2 });
    request.flush(ESTIMATE);
  });

  it('sends the feeling and notes toggles so the server can skip that work', async () => {
    await typeStory('Mia found a map.');
    root.querySelector<HTMLInputElement>('input[name="feeling-labels"]')!.click();
    root.querySelector<HTMLInputElement>('input[name="notes"]')!.click();
    await fixture.whenStable();

    root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    const request = http.expectOne('/api/comics/estimate');
    expect(request.request.body).toMatchObject({ feelingLabels: false, notes: false });
    request.flush(ESTIMATE);
  });

  it('cancelling goes back without generating', async () => {
    await reachConfirmation();

    clickButton('Cancel');
    await fixture.whenStable();

    expect(root.querySelector('.confirm')).toBeNull();
    http.expectNone('/api/comics/generate');
  });

  it('discards the estimate when the story changes', async () => {
    await reachConfirmation();

    await typeStory('A different story.');

    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('shows the estimate while drawing', async () => {
    const request = await submitStory();
    await fixture.whenStable();

    expect(root.querySelector('.loading')?.textContent).toContain('about 40 seconds');

    request.flush(STORY);
    await fixture.whenStable();
    expect(root.querySelector('.loading')).toBeNull();
    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('shows the exact source text once, under the last panel of its part', async () => {
    await renderStory();

    const blocks = root.querySelectorAll('.panel-block');
    expect(blocks[0].querySelector('.source-text')).toBeNull();
    const source = blocks[1].querySelector<HTMLElement>('.source-text')!;
    expect(source.textContent).toBe('Mia pointed.\n"<b>Look!</b>" she shouted. Leo wondered.');
    expect(source.querySelector('b')).toBeNull();
    expect(source.hasAttribute('contenteditable')).toBe(false);
  });

  it('shows story markup as literal text', async () => {
    await renderStory();

    const bubble = root.querySelector<HTMLElement>('.bubble')!;
    expect(bubble.textContent).toBe('<b>Look!</b>');
    expect(bubble.querySelector('b')).toBeNull();
  });

  it('marks text as editable and keeps edits', async () => {
    await renderStory();
    const bubble = root.querySelector<HTMLElement>('.bubble')!;
    expect(bubble.getAttribute('contenteditable')).toBe('true');

    await editText(bubble, 'Over here!');

    expect(root.querySelector('.bubble')!.textContent).toBe('Over here!');
  });

  it('renames a character in every panel', async () => {
    await renderStory();
    const names = () =>
      Array.from(root.querySelectorAll('.actor-name')).map((name) => name.textContent);

    await editText(root.querySelectorAll<HTMLElement>('.actor-name')[1], 'Theo');

    expect(names()).toEqual(['Mia', 'Theo', 'Theo']);
  });

  it('rejects renaming a character to a name already taken', async () => {
    await renderStory();

    await editText(root.querySelectorAll<HTMLElement>('.actor-name')[1], 'Mia');

    expect(root.querySelectorAll('.actor-name')[1].textContent).toBe('Leo');
  });

  it('changes a pose from the picker', async () => {
    await renderStory();
    const select = root.querySelector<HTMLSelectElement>('.actor-controls select')!;
    expect(select.value).toBe('pointing');

    select.value = 'running';
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(root.querySelector('.panel app-stick-figure svg')!.getAttribute('aria-label')).toBe(
      'Mia, running, looking surprised',
    );
  });

  it('shows the server error detail', async () => {
    (await submitStory()).flush(
      { detail: 'No LLM API key is configured.' },
      { status: 503, statusText: 'Service Unavailable' },
    );
    await fixture.whenStable();

    expect(root.querySelector('.alert')?.textContent).toContain('No LLM API key');
  });
});

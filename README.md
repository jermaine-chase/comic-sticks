# Comic Sticks

Turns a story, typed in or uploaded as a `.txt` or `.pdf` file, into an editable stick figure comic.
An LLM parses the text into structured panels, and Angular draws them with SVG figures and
HTML/CSS speech bubbles.

| Folder      | Stack                                        | Runs on                 |
| ----------- | -------------------------------------------- | ----------------------- |
| `backend/`  | Java 21+, Spring Boot 4, RestClient, PDFBox  | http://localhost:8080   |
| `frontend/` | Angular 22 standalone components, TypeScript | http://localhost:4200   |

## Prerequisites

- JDK 21 or newer, and Maven 3.9+
- Node `^22.22.3`, `^24.15.0` or `>=26`, as Angular 22 requires. On Node 23, `ng build` and
  `ng serve` work but `ng test` does not.
- npm newer than 11.1.0, which crashes while installing these dependencies. The version in
  `frontend/package.json` under `packageManager` is known to work.

## Run it

Backend, in one terminal (PowerShell):

```powershell
cd backend
mvn spring-boot:run
```

To try the app without any LLM, use the demo profile instead. It splits the text by sentence
with simple rules, so its comics are much cruder than an LLM's:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Frontend, in a second terminal:

```powershell
cd frontend
npm install
npm start
```

Open http://localhost:4200. The dev server proxies `/api` to the backend (`proxy.conf.json`), so
CORS needs no configuration. A production deployment needs the same routing, or CORS added to the
backend.

## Choosing an LLM

The backend calls any OpenAI-compatible `/chat/completions` endpoint. By default it uses a local
Ollama server, so no environment variables are needed. Override these to use something else:

| Variable       | Default                         | Purpose                                 |
| -------------- | ------------------------------- | --------------------------------------- |
| `LLM_BASE_URL` | `http://192.168.1.239:11434/v1` | Provider endpoint, ending in `/v1`      |
| `LLM_MODEL`    | `qwen2.5-coder:7b`              | Model id                                |
| `LLM_API_KEY`  | `ollama`                        | Provider key; Ollama ignores this value |

The provider and model must support `response_format` with a strict `json_schema`.
`backend/src/main/resources/application.properties` has OpenAI and Gemini examples, along with the
timeout, upload and story length limits.

## How a story becomes a comic

1. `POST /api/comics/estimate` splits the story and predicts the run time, without calling the LLM.
   The UI shows this and waits for the user to confirm.
2. `POST /api/comics/characters` runs the character pass: the story is read in chunks of
   `comic.characters.chunk-chars`, one small call each, to find the main characters and places.
   Each character gets a fixed design (person or animal, 1 to 2 of hat, hair and accessory, and
   ears, tail and snout for animals) and each place a background preset. The UI shows this
   character sheet for review and editing.
3. `POST /api/comics/generate` sends the story to the LLM one part at a time, in order, and joins
   the panels. Every part is given the sheet, and every figure is drawn from it by name, so a
   character looks the same in every panel. Anyone a panel names who is not on the sheet gets a
   plain design marked `added`. Without a `characterSheet`, generate runs the character pass
   itself.

Each part becomes one panel, and the story's own words for that part appear, exactly as written,
under the panel (`sourceText`). The LLM never writes or rewrites that text. How much text a part
holds depends on the support level (`supportLevel`, see `StorySegmenter`):

| Level       | One panel per                                                                  |
| ----------- | ------------------------------------------------------------------------------ |
| 1 (default) | paragraph. Paragraphs over `level1-max-part-chars` are cut at sentences.       |
| 2           | 3 to 4 paragraphs (`level2-paragraphs-per-panel`).                             |
| 3           | scene, split at `***`, `* * *`, extra blank lines and chapter headings, or 6 to 10 paragraphs when there are none. |

- At level 1, only a heading, or a short line of dialogue and its short reply
  (`short-line-chars`), joins a neighbour.
- Groups never cross a scene break, and are spread evenly across each scene.
- The final part gets a second panel when there is room and it has more than one sentence. Its
  text then appears once, under the last of the two.
- Text that needs more than `comic.story.max-panels` panels is refused with a message asking for
  chapters or a shorter piece. Paragraphs are never merged to make it fit.

### What goes in a panel

Each part makes small, strict-schema LLM calls rather than one big one, which suits small local
models:

1. **Panels.** Characters with pose, face, facing and (if on) a one-word `feeling`; bubbles; and a
   `caption` only when time passes or the place changes. Bubbles of type `speech`, `shout` and
   `whisper` must be words spoken in the text: `PanelValidator` checks each one as a substring of
   the passage's quotations, ignoring curly vs. straight quotes, case and whitespace. If one fails,
   the part is retried once with the problem explained, then any bubble still failing is dropped.
   `thought` bubbles (what a character thinks, wants or plans) are drawn as clouds and not checked.
2. **Notes** (if on). "What it means" notes for idioms, metaphors, sarcasm and old words. A note is
   kept only if its `phrase` appears in the panel's `sourceText`.

A known feeling word sets the face (`FeelingExpressions`), so the label and face never disagree.
The frontend enforces the layout whatever the LLM chose: fixed figure slots, bubbles above their
speakers, tails measured to end at the speaker's head, and speakers turned to face their listener.

### Chapter bookends

Every comic opens and closes the same way (`bookends` in the response):

- **Who's who**: everyone who appears in the chapter, drawn from the sheet and named, plus one
  plain "Where we are" line. It is built in code from the panels.
- **What happened**: 2 to 4 short sentences, each tied to a panel shown as a thumbnail, from one
  small recap call. The code then adds the closing line itself ("At the end, Mole feels proud.")
  with a small figure of the main character, so the recap always ends with how they feel.
- **Questions** (`questions`, off by default): one more small call writes 2 or 3 questions and
  picks each answer from the chapter's characters or panel numbers. The code adds the wrong
  choices, so every answer is a picture the reader has seen.

`BookendsRules` checks everything the LLM suggests: panel numbers must exist, the main character
must be in the chapter, and sentences stay short. `comic.bookends.*` limits how much of the
chapter the calls see.

The estimate is `parts x (seconds-per-part + seconds-per-notes-call) + panels x seconds-per-panel`
`+ seconds-per-bookends-call (+ seconds-per-questions-call)`
(+ `chunks x seconds-per-character-chunk` when no sheet is supplied)
(all under `comic.llm`; the notes term only when notes are on, and retries are not counted). The
defaults were measured with `qwen2.5-coder:7b` on one machine with the model already loaded, so
retune them after changing model or hardware: the backend logs how long each part took.

## API

`POST /api/comics/generate` and `POST /api/comics/estimate` each accept either form:

```bash
curl -X POST localhost:8080/api/comics/generate \
  -H "Content-Type: application/json" \
  -d '{"text":"Mia found a map in the attic.","supportLevel":1}'

curl -X POST localhost:8080/api/comics/generate -F "file=@story.pdf" -F "supportLevel=2"
```

`POST /api/comics/characters` takes the same text or file and returns `{ characters, places }`.
Pass that sheet back, edited, as `characterSheet` (a JSON object, or a JSON string form field in a
multipart request) to `/estimate` and `/generate`.

`supportLevel` (default 1), `feelingLabels` and `notes` (both default `true`) and `questions`
(default `false`) are optional. When a toggle is off, that field is not requested from the LLM at
all.

An estimate response:

```json
{ "parts": 8, "panels": 12, "estimatedSeconds": 72 }
```

A generate response:

```json
{
  "title": "The Attic Map",
  "panels": [
    {
      "panelNumber": 1,
      "scene": "indoor",
      "caption": "",
      "characters": [
        { "name": "Mia", "pose": "pointing", "expression": "surprised", "facing": "right", "feeling": "surprised" }
      ],
      "dialogue": [{ "speaker": "Mia", "text": "Look at this!", "type": "shout" }],
      "sourceText": "Mia found a map in the attic. \"Look at this!\" she shouted.",
      "notes": []
    }
  ],
  "characters": [],
  "places": [],
  "bookends": {
    "whosWho": { "characters": ["Mia"], "whereWeAre": "We are in the attic, on a rainy day." },
    "recap": [
      { "sentence": "Mia found a map.", "panelNumber": 1, "character": null, "expression": null },
      { "sentence": "At the end, Mia feels excited.", "panelNumber": null, "character": "Mia", "expression": "excited" }
    ],
    "questions": []
  }
}
```

Errors are RFC 9457 problem documents, and their `detail` field is safe to show to users.

## Tests

```powershell
cd backend;  mvn test
cd frontend; npm test -- --watch=false
```

## Adding a pose or expression

Add the value in three places: the Java enum (`Pose` or `Expression`), the list in
`comic.models.ts`, and the drawing in `stick-figure.poses.ts`. The LLM schema is built from the
Java enums, so it picks up the change on its own. A new expression should also get its feeling
words in `FeelingExpressions`. Dialogue types follow the same rule (`DialogueType`,
`DIALOGUE_TYPES`, and the bubble styles in `comic-panel.component.css`).

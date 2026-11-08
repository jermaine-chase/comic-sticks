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
2. `POST /api/comics/generate` sends the story to the LLM one part at a time, in order, and joins
   the panels. Each part is told the character names found so far.

Each paragraph is a part, with these adjustments (see `StorySegmenter`):

- Paragraphs over `comic.story.max-part-chars` are cut at sentence boundaries.
- Paragraphs under `comic.story.min-part-chars`, such as chapter headings, join a neighbour.
- If there are more parts than `comic.story.max-panels`, the smallest neighbours merge.
- The final paragraph stays separate and gets two panels when one is spare, to protect the ending.

The estimate is `parts x comic.llm.seconds-per-part + panels x comic.llm.seconds-per-panel`. The
defaults were measured with `qwen2.5-coder:7b` on one machine with the model already loaded, so
retune them after changing model or hardware: the backend logs how long each part took.

## API

`POST /api/comics/generate` and `POST /api/comics/estimate` each accept either form:

```bash
curl -X POST localhost:8080/api/comics/generate \
  -H "Content-Type: application/json" \
  -d '{"text":"Mia found a map in the attic."}'

curl -X POST localhost:8080/api/comics/generate -F "file=@story.pdf"
```

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
      "caption": "A rainy Saturday.",
      "characters": [
        { "name": "Mia", "pose": "pointing", "expression": "surprised", "facing": "right" }
      ],
      "dialogue": [{ "speaker": "Mia", "text": "Look at this!", "type": "shout" }]
    }
  ]
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
Java enums, so it picks up the change on its own.

-- A generated comic, saved as the reader last edited it. The comic and the options it was drawn
-- with are stored as JSON text, so the same table works on PostgreSQL and on H2 (demo and tests).
-- Stage 6 books can add a book table later, with saved comics as its chapters.
CREATE TABLE saved_comic (
    id           UUID PRIMARY KEY,
    title        VARCHAR(300) NOT NULL,
    panel_count  INTEGER NOT NULL,
    options_json TEXT NOT NULL,
    comic_json   TEXT NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX saved_comic_updated_at ON saved_comic (updated_at);

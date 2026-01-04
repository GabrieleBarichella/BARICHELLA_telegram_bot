CREATE TABLE users (
    user_id INTEGER PRIMARY KEY NOT NULL,
    username VARCHAR(255)
);

CREATE TABLE anime (
    anime_id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    synopsis TEXT,
    episode_count INTEGER,
    average_rating REAL,
    status TEXT,
    poster_url TEXT,
    last_updated DATETIME
);

CREATE TABLE user_anime (
    user_id INTEGER NOT NULL,
    anime_id INTEGER NOT NULL,
    state TEXT CHECK(state IN ('WATCHLIST', 'WATCHING', 'COMPLETED')),
    PRIMARY KEY (user_id, anime_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (anime_id) REFERENCES anime(anime_id)
);

CREATE TABLE favorites (
    user_id INTEGER NOT NULL,
    anime_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, anime_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (anime_id) REFERENCES anime(anime_id)
);

CREATE TABLE games (
    game_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

CREATE TABLE user_games (
    user_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    score INTEGER DEFAULT 0,
    played_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id, played_at),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (game_id) REFERENCES games(game_id)
);


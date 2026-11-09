CREATE TABLE profiles (
  user_id  TEXT PRIMARY KEY,
  pseudo   TEXT UNIQUE NOT NULL,
  country  TEXT NOT NULL,
  language TEXT NOT NULL
);
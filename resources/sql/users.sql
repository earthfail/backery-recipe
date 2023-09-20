-- users definition

CREATE TABLE users (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	name TEXT(50),
	email TEXT,
	refresh_token TEXT,
	authorization_server TEXT,
	authorization_server_refresh_token TEXT,
	asrt_expire_date TEXT
, rt_expire_date TEXT, avatar_url TEXT, last_modified TEXT);

CREATE UNIQUE INDEX users_email_IDX ON users (email);

-- logs definition

CREATE TABLE logs (
	message TEXT(100),
	time_stamp TEXT
, "type" TEXT);
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
-- "users-recipes" definition

CREATE TABLE "users-recipes" (
	"user-id" INTEGER,
	"recipe-id" INTEGER,
	description TEXT(100), name TEXT(50), last_modified TEXT, finish_count INTEGER DEFAULT (0),
	CONSTRAINT users_recipes_PK PRIMARY KEY ("recipe-id"),
	CONSTRAINT users_recipes_FK FOREIGN KEY ("user-id") REFERENCES users(id)
);
-- "recipes-steps" definition

CREATE TABLE "recipes-steps" (
	"recipe-id" INTEGER,
	step INTEGER DEFAULT (1),
	description TEXT,
	media TEXT,
	"media-type" TEXT, last_modified TEXT,
	CONSTRAINT recipes_steps_FK FOREIGN KEY ("recipe-id") REFERENCES "users-recipes"("recipe-id")
);

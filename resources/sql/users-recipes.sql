-- "users-recipes" definition

CREATE TABLE "users-recipes" (
	"user-id" INTEGER,
	"recipe-id" INTEGER,
	description TEXT(100), name TEXT(50), last_modified TEXT, finish_count INTEGER DEFAULT (0),
	CONSTRAINT users_recipes_PK PRIMARY KEY ("recipe-id"),
	CONSTRAINT users_recipes_FK FOREIGN KEY ("user-id") REFERENCES users(id)
);

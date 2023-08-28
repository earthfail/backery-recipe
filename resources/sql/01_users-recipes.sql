CREATE TABLE "users-recipes" (
	"user-id" INTEGER,
	"recipe-id" INTEGER,
	description TEXT(100),
	CONSTRAINT users_recipes_PK PRIMARY KEY ("recipe-id"),
	CONSTRAINT users_recipes_FK FOREIGN KEY ("user-id") REFERENCES users(id)
);

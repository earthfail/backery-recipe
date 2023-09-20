-- "recipes-steps" definition

CREATE TABLE "recipes-steps" (
	"recipe-id" INTEGER,
	step INTEGER DEFAULT (1),
	description TEXT,
	media TEXT,
	"media-type" TEXT, last_modified TEXT,
	CONSTRAINT recipes_steps_FK FOREIGN KEY ("recipe-id") REFERENCES "users-recipes"("recipe-id")
);

-- Create foreign key recipes_steps_FK

CREATE TEMPORARY TABLE temp AS
SELECT *
FROM "recipes-steps";

DROP TABLE "recipes-steps";

CREATE TABLE "recipes-steps" (
	"recipe-id" INTEGER,
	step INTEGER DEFAULT (1),
	description TEXT,
	media TEXT,
	"media-type" TEXT,
	CONSTRAINT recipes_steps_FK FOREIGN KEY ("recipe-id") REFERENCES "users-recipes"("recipe-id")
);

INSERT INTO "recipes-steps"
SELECT *
FROM temp;

DROP TABLE temp;

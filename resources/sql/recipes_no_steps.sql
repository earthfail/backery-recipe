SELECT * from "users-recipes" ur
WHERE (ur."recipe-id") NOT IN (SELECT "recipe-id" FROM "recipes-steps" rs)  
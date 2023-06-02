* to install postgres docker image run
docker pull postgres
* to run postgres with data in dev/data/pg in project directory run
docker run --name pg-db -p 5432:5432 -v $(pwd)/dev/data/pg:/var/lib/postgresql/data -e POSTGRES_PASSWORD=postgres postgres

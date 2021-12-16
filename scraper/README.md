## Installation

Bootstrap a postgres database,
```
docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:13-alpine
```

Create the schema,
```sql
create table leaderboard (
  tstz timestamptz not null,
  "rank" jsonb not null
);

create unique index on leaderboard(tstz, ("rank"->>'name'))
```

Run `build-run-scraper.sh` to start.

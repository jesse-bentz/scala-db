# scala-db

Scala tools for interacting with PostgreSQL and MongoDB. Postgres tools are built on slick, and MongoDB on casbah.
Running tests requires setting up the databases as described below.

To compile:
```
$ sbt compile
```

Postgres Setup:
- Install Postgresql
- Then in a terminal:
```
> createdb scala-db
> psql
jesse=# create user db_test with password '';
jesse=# grant all privileges on database "scala-db" to db_test;
```

MongoDB Setup
- http://docs.mongodb.org/getting-started/shell/installation/
- Run mongod server

Run tests:
```
$ sbt test
```

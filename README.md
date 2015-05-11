# scala-db

Scala tools for interacting with PostgreSQL and MongoDB. Postgre tools are built on slick, and MongoDB on Casandra.
Running tests requires setting up the databases as described below.

Postgres Setup:
- Install Postgresql
```
> createdb jesse-scala-sample
> psql
jesse=# create user scala_sample with password '';
jesse=# grant all privileges on database "jesse-scala-sample" to scala_sample;
```

MongoDB Setup
- http://docs.mongodb.org/getting-started/shell/installation/
- Run mongod server

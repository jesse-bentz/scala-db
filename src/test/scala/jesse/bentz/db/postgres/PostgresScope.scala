package jesse.bentz.db.postgres

import org.specs2.specification.Scope

class PostgresScope extends Scope {
  PostgresSetup.setup
}

object PostgresSetup {
  lazy val setup = {
    Postgres.migrate
    Postgres.flushForTests
    Thread.sleep(1000)
  }

}


package jesse.bentz.db.mongodb

import org.specs2.specification.Scope

class MongoDBScope extends Scope {
  MongoDBSetup.setup
}

object MongoDBSetup {
  lazy val setup = {
    MongoDB.db.dropDatabase
  }
}


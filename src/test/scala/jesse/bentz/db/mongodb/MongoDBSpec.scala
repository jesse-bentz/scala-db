package jesse.bentz.db.mongodb

import org.specs2.mutable.Specification

class MongoDBSpec extends Specification {

  "MongoDBSpec" should {
    "ping" in new MongoDBScope {
      MongoDB.ping === true
    }
    "store, get" in new MongoDBScope {
      val person = Person("Jesse", "James")
      val stored = People.store(person)
      stored must beSome[Person]
      val person1 = People.get(person)
      person1 must beSome[Person]
      person1.get.firstName === person.firstName
    }
  }
}

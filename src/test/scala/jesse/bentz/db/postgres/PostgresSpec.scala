package jesse.bentz.db.postgres

import scala.util.Random
import org.specs2.mutable.Specification

class PostgresSpec extends Specification {

  def randStr = Random.alphanumeric.take(10).mkString

  "PostgresSpec" should {
    "monitor" in new PostgresScope {
      PostgresMonitor.up === true
    }
    "insert, find, update, delete" in new PostgresScope {
      val person = Person(firstName=randStr, lastName=randStr)
      val inserted = People.insert(person)
      inserted must beSome[Person]
      inserted.get.firstName === person.firstName
      inserted.get.lastName === person.lastName
      val found = People.find(person)
      found must beSome[Person]
      found.get.firstName === person.firstName
      val person1 = found.get.copy(firstName=randStr)
      People.update(person1) === true
      val found1 = People.find(person1)
      found1 must beSome[Person]
      found1.get.firstName === person1.firstName
      found1.get.pk === person1.pk
      People.delete(person1) === 1
      People.find(person1) === None
    }
  }
}

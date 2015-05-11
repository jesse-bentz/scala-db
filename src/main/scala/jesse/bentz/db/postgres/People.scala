package jesse.bentz.db.postgres

import grizzled.slf4j.Logging
import scala.slick.driver.PostgresDriver.simple._

case class Person(firstName: String, lastName: String, pk: Option[Int]=None) extends TableRow

class People(tag: Tag) extends RichTable[Person](tag, "people") {
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def * = (firstName, lastName, pk.?) <> (Person.tupled, Person.unapply)
}

object People extends TableUtils[Person, People] {
  val tableQuery = TableQuery[People]

  def __insert(entity: Person)(implicit s: Session): Person = {
    val withPk = (tableQuery returning tableQuery.map(_.pk) into ((x, pk) => x.copy(pk=Some(pk)))) += entity
    withPk
  }

  def _find(entity: Person)(implicit s: Session): Option[Person] = {
    tableQuery.filter(_.firstName === entity.firstName).filter(_.lastName === entity.lastName).firstOption
  }

}


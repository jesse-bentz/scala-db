package jesse.bentz.db.mongodb

import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logging

object People extends MongoDBCollection[Person] {

  val collectionName = "people"

  def build(obj: DBObject): Person = Person(
    id=obj._id.map(_.toString),
    firstName=obj.as[String]("firstName"),
    lastName=obj.as[String]("lastName")
  )
}

case class Person(firstName: String, lastName: String, id: Option[String]=None) extends MongoDBObj {
  val obj = MongoDBObject("firstName" -> firstName, "lastName" -> lastName)
}


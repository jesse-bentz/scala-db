package jesse.bentz.db.mongodb

import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logging

import jesse.bentz.util.ConfigurationManager

object MongoDB extends Logging {

  val config = MongoConfig(
    ConfigurationManager("mongodb.host"),
    ConfigurationManager.getInt("mongodb.port"),
    ConfigurationManager("mongodb.name"))

  lazy val db: MongoDB = {
    logger.info(s"Initializing MongoDB with config: $config")
    val client = MongoClient(config.host, config.port)
    client(config.db)
  }

  def getCollection(coll: String) = db(coll)

  def ping: Boolean = try {
    val c = getCollection("monitor")
    val test = MongoDBObject("ping" -> "pong")
    c.insert(test)
    true
  } catch { case e: Exception => logger.warn(e); false }
}

case class MongoConfig(host: String, port: Int, db: String)


trait MongoDBCollection[A <: MongoDBObj] extends Logging {

  val collectionName: String

  def build(obj: DBObject): A

  val empty = MongoDBObject()

  lazy val collection = MongoDB.getCollection(collectionName)

  def store(inst: A): Option[A] = {
    val res = try Some(collection.insert(inst.obj)) catch { case e: Exception => logger.error(e); None }
    res.map { row =>
      logger.debug(s"Stored ${inst.getClass.getSimpleName} in mongodb: $inst")
      inst
    }
  }

  def get(inst: A, ignore: List[String]=List.empty): Option[A] = get(inst.query(ignore))

  def get(q: DBObject): Option[A] = {
    logger.debug(s"Query: $q")
    collection.findOne(q).flatMap { obj =>
      logger.debug(s"Got mongodb obj: $obj")
      try Some(build(obj)) catch {
        case e: java.util.NoSuchElementException => {
          logger.error(s"Failed to inflate mongodb obj: $obj. ${e.getMessage}")
          None
        }
      }
    }
  }

  def autoIncrement(name: String): Int = {
    val newId = collection.findAndModify(
      MongoDBObject("_id" -> name), empty, empty, false, $inc("_seq" -> 1), true, true)
    newId.map(_.as[Int]("_seq")).getOrElse {
      logger.error("Failed to auto-increment: $name")
      0
    }
  }

}

trait MongoDBObj extends Logging {
  val id: Option[String]
  val obj: DBObject
  val empty = MongoDBObject()
  val ignoreForQuery: List[String] = List.empty

  def query(ignore: List[String]): MongoDBObject = {
    val _ignore = ignore ++ ignoreForQuery
    obj.filter(kv => !_ignore.contains(kv._1))
  }

}


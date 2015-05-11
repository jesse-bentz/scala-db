package jesse.bentz.db.postgres

import grizzled.slf4j.Logging
import org.joda.time.{DateTime, LocalTime}
import scala.slick.driver.PostgresDriver.simple._
import com.googlecode.flyway.core.Flyway
import scala.slick.jdbc.{StaticQuery => Q}
import org.postgresql.util.PSQLException

import jesse.bentz.util.ConfigurationManager

object Postgres extends Logging {

  // these are vars to allow reload of config at runtime
  private var config: PostgresConfig = buildConfig
  private var _db: Database = buildDB(config)
  def db: Database = _db

  def reloadConfig = {
    config = buildConfig
    _db = buildDB(config)
  }

  def buildDB(c: PostgresConfig): Database = {
    val driver = "org.postgresql.Driver"
    Database.forURL(c.url, driver=driver)
  }

  def buildConfig: PostgresConfig = {
    val c = new PostgresConfig
    logger.info("Postgres URL: " + c.baseUrl)
    c
  }

  // query wrapper to handle errors
  def safeQuery[T](f: Session => Option[T]): Either[String, Option[T]] = {
    try db.withSession { implicit s => Right(f(s)) }
    catch { case e: PSQLException => {
      logger.error(e)
      Left(e.getMessage)
    }}
  }

  def migrate {
    val url = config.baseUrl
    logger.debug("Applying migrations to " + url)
    val flyway: Flyway = new Flyway()
    flyway.setDataSource(url, config.user, config.password)
    flyway.migrate()
  }

  // is the db responding?
  def up: Boolean = {
    val up = safeQuery { implicit s => Q.queryNA[String]("show SERVER_VERSION;").firstOption }.isRight
    if (!up) logger.error("Postgres is down: " + config.baseUrl)
    up
  }

  def flushForTests {
    if (config.test) {
      // manually add tables to flush for tests here
      People.flushAndReset
    } else logger.warn("Refusing to flush non-test db: " + config.dbname)
  }

  def now = new java.sql.Timestamp(new java.util.Date().getTime)

  // helper for debugging issues with tests
  def printMsgWithTime(msg: String) {
    println(Thread.currentThread.getId + " " + LocalTime.now + " " + msg)
  }
}

case class PostgresConfig(
    dbname: String,
    host: String,
    port: String,
    user: String,
    password: String,
    test: Boolean) {

  def this() = this(
    dbname=ConfigurationManager.get("postgres.dbname"),
    host=ConfigurationManager.get("postgres.host"),
    port=ConfigurationManager.get("postgres.port"),
    user=ConfigurationManager.get("postgres.username"),
    password=ConfigurationManager.get("postgres.password"),
    test=ConfigurationManager.getBoolean("postgres.test"))

  val baseUrl = s"jdbc:postgresql://$host:$port/$dbname"
  val url = baseUrl + s"?user=$user&password=$password"
}

object PostgresMonitor {
  // lazy var
  private var _up: Option[Boolean] = None
  def up: Boolean = { if (_up.isEmpty) check; _up.getOrElse(false) }
  def check { _up = Some(Postgres.up) }
}

trait TableRow extends Logging {
  val pk: Option[Int]
}

abstract class RichTable[T <: TableRow](tag: Tag, name: String) extends Table[T](tag, name) {
 def pk = column[Int]("pk", O.PrimaryKey, O.AutoInc)
}

trait TableUtils[A <: TableRow, T <: RichTable[A]] extends Logging {

  val tableQuery: TableQuery[T]
  lazy val tableName = tableQuery.baseTableRow.tableName
  val duplicateInsertRecovery: Boolean = false

  def findOrInsert(entity: A): Option[A] = {
    Postgres.safeQuery { implicit s => _findOrInsert(entity) }.right.toOption.flatten
  }

  def _findOrInsert(entity: A)(implicit s: Session): Option[A] = {
    _find(entity) orElse (duplicateInsertRecovery match {
      case true => _insertWithDupRecovery(entity, _insert)
      case false => _insert(entity)
    })
  }

  def _find(entity: A)(implicit s: Session): Option[A]
  def find(entity: A): Option[A] = Postgres.safeQuery { implicit s => _find(entity) }.right.toOption.flatten

  def findByPk(pk: Int): Option[A] = Postgres.safeQuery { implicit s =>
    tableQuery.filter(_.pk === pk).firstOption
  }.right.toOption.flatten

  def validate(entity: A): Option[String] = None

  def update(entity: A): Boolean = Postgres.safeQuery { implicit s =>
    Some(_update(entity))
  }.right.toOption.flatten.getOrElse(false)

  def _update(entity: A)(implicit s: Session): Boolean = {
    //logger.debug(s"Attempting to update DB entity: $entity with pk: ${entity.pk}")
    val updated = for {
      valid <- if (validate(entity).isEmpty) Some(true) else None
      pk <- entity.pk
    } yield tableQuery.filter(_.pk === pk).update(entity) == 1
    updated.getOrElse(false)
  }

  def delete(entity: A): Int = Postgres.safeQuery { implicit s =>
    _delete(entity)
  }.right.toOption.flatten.getOrElse(0)

  def _delete(entity: A)(implicit s: Session): Option[Int] = {
    Some(tableQuery.filter(_.pk === entity.pk).delete)
  }

  // insert with validation
  def _insert(entity: A)(implicit s: Session): Option[A] = validate(entity) match {
    case Some(msg) => logger.warn(s"${getClass.getSimpleName} insert validation failed: $msg"); None
    case None => {
      val inserted = __insert(entity)
      //printMsgWithTime("Inserted " + inserted)
      Some(inserted)
    }
  }

  // raw insert
  def __insert(entity: A)(implicit s: Session): A

  // insert with session creation, error handling and validation
  def insert(entity: A): Option[A] = Postgres.safeQuery { implicit s => _insert(entity) }.right.toOption.flatten

  // account for parallel threads creating duplicate records
  def _insertWithDupRecovery(entity: A, insertF: A => Option[A])(implicit s: Session): Option[A] = {
    try insertF(entity) catch {
      case e: PSQLException if e.getMessage.contains("violates unique constraint") => {
        //printMsgWithTime(e.getMessage)
        logger.warn(e)
        def sleepAndFind: Option[A] = { Thread.sleep(1000); _find(entity) }
        Stream.fill(3)(sleepAndFind).find(_.isDefined).flatten
      }
      case e: Exception => logger.error(e); None
    }
  }

  // for tests
  def flushAndReset { Postgres.db.withSession { implicit s =>
    //printMsgWithTime("Flushing table: " + tableName)
    val deleted = Q.queryNA[Int](s"delete from ${tableName};").first
    //printMsgWithTime(s"Deleted $deleted rows from $tableName")
    Q.updateNA(s"alter sequence ${tableName}_pk_seq restart with 1;").execute
  }}

  def printMsgWithTime = Postgres.printMsgWithTime _
}

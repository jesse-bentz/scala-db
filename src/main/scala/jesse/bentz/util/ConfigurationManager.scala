package jesse.bentz.util

import com.typesafe.config.{ConfigFactory, ConfigException, ConfigRenderOptions, ConfigObject}

object ConfigurationManager {
  val config = ConfigFactory.load
  def apply(key: String): String = get(key)
  def get(key: String): String = config.getString(key)
  def getOpt(key: String): Option[String] = try Some(get(key)) catch { case e: ConfigException.Missing => None }
  def getBoolean(key: String): Boolean = try config.getBoolean(key) catch { case e: ConfigException.Missing => false }
  def getInt(key: String): Int = config.getInt(key)
}

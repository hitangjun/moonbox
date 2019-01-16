package moonbox.grid.deploy

import com.typesafe.config.ConfigFactory
import moonbox.common.util.Utils
import moonbox.grid.deploy.worker.LaunchUtils
import org.apache.spark.launcher.SparkLauncher

trait DriverDescription {
	def master: String
	def deployMode: Option[String]
	def mainClass: String
	def appResource: String
	def toAppArgs: Seq[String]
	def toConf: Map[String, String]
}

case class LocalDriverDescription(
	driverId: String,
	masters: Array[String],
	config: Map[String, String]) extends DriverDescription {

	override def master = {
		val cores = Runtime.getRuntime.availableProcessors()
		s"local[${cores * 50}]"
	}
	override def deployMode = None
	override def mainClass = "moonbox.application.interactive.Main"

	override def toString: String = {
		s"DriverDescription ($master)"
	}

	override def toAppArgs: Seq[String] = {
		(config.filterKeys(key => !(key.startsWith("spark.hadoop") || key.startsWith("spark.yarn"))) ++ Map(
			"driverId" -> driverId,
			"masters" -> masters.mkString(";"),
			"applicationType" -> "CENTRALIZED"
		)).toSeq.flatMap { case (k, v) => Seq(k, v)}
	}

	override def toConf: Map[String, String] = {
		config.filterKeys(_.startsWith("spark.")) ++ Map(
			SparkLauncher.DRIVER_EXTRA_CLASSPATH -> LaunchUtils.getDriverClasspath()
		)
	}

	override def appResource: String = {
		LaunchUtils.getAppResourceJar("interactive").getOrElse(
			throw new Exception("Interactive app jar does not found in env.")
		)
	}
}

case class ClusterDriverDescription(
	driverId: String,
	masters: Array[String],
	config: Map[String, String]) extends DriverDescription {

	override def master = "yarn"
	override def deployMode = Some("client")
	override def mainClass = "moonbox.application.interactive.Main"

	override def toString: String = {
		s"DriverDescription ($master ${deployMode.get})"
	}

	override def toAppArgs: Seq[String] = {
		(config.filterKeys(key => !(key.startsWith("spark.hadoop") || key.startsWith("spark.yarn"))) ++ Map(
			"driverId" -> driverId,
			"masters" -> masters.mkString(";"),
			"applicationType" -> "DISTRIBUTED"
		)).toSeq.flatMap { case (k, v) => Seq(k, v)}
	}

	override def toConf: Map[String, String] = {
		config.filterKeys(_.startsWith("spark.")) ++ Map(
			SparkLauncher.DRIVER_EXTRA_CLASSPATH -> LaunchUtils.getDriverClasspath()
		)
	}

	override def appResource: String = {
		LaunchUtils.getAppResourceJar("interactive").getOrElse(
			throw new Exception("Interactive app jar does not found in env.")
		)
	}
}

case class BatchDriverDescription(
	username: String,
	sqls: Seq[String],
	config: Map[String, String]
) extends DriverDescription {

	override def master = "yarn"
	override def deployMode = Some("cluster")
	override def mainClass = "moonbox.application.batch.Main"

	override def toString: String = {
		s"DriverDescription ($master ${deployMode.get} $username ${sqls.mkString(";")})"
	}

	override def toAppArgs: Seq[String] = {
		(config.filterKeys(key => !(key.startsWith("spark.hadoop") || key.startsWith("spark.yarn"))) ++ Map(
			"username" -> username,
			"sqls" -> sqls.mkString(";")
		)).toSeq.flatMap { case (k, v) => Seq(k, v) }
	}

	override def toConf: Map[String, String] = {
		config.filterKeys(_.startsWith("spark."))
	}

	override def appResource: String = {
		LaunchUtils.getAppResourceJar("batch").getOrElse(
			throw new Exception("batch app jar does not found in env.")
		)
	}
}


/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package config

import java.io.File
import java.io.{BufferedReader, InputStream, InputStreamReader}
import org.apache.commons.logging.{Log, LogFactory}
import scala.collection.Iterator
import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.{Elem, Node}

class ComponentLaunchSpecification {
  // In the superconfig file DSL
  var component = ""
  var host = "127.0.0.1"
  var registry = host
  var wantsInput = false
  // var port = 1099  // TODO: add port support to Ant, then to config files
  // var name = ""  // TODO: add name support to Ant, and then to config files
  var args = ""
  var libPath = ""
  var pause = 0

  // NOT in the superconfig file DSL
  var p: Option[Process] = None
  var printlnLog: Log = null
}

trait ConfigFile {
  var specs: Seq[ComponentLaunchSpecification] = Nil
  var log = LogFactory.getLog("ConfigFile")
  val debugLog = System.getProperty("config.debug")
  val debugMode = debugLog.nonEmpty

  def getComponentSpecs: java.util.List[ComponentLaunchSpecification] = specs

  def COMPONENT[T](clazz: Class[T]): Unit = {
    specs = new ComponentLaunchSpecification +: specs
    specs.head.component = clazz.getName
    specs.head.printlnLog = LogFactory.getLog(clazz.getName)
  }

  def host(h: String) = specs.head.host = h

  def registry(h: String) = specs.head.registry = h

  def wantsInput = specs.head.wantsInput = true

  // def port(p: Int) = specs.head.port = p

  // def name(n: String) = specs.head.name = n

  def args(a: String) = specs.head.args = a

  def libPath(p: String) = specs.head.libPath = p

  def pause(s: Int) = specs.head.pause = s

  def main(args: Array[String]): Unit = {
    val buildFile = scala.xml.XML.loadFile(System.getProperty("buildFile"))
    val ClassSplitter = """^(\w+\.\w+).+?(\w+)$""".r

    // TODO: add support for stdin

    // this looks a bit crazy, but it's really just the standard Fork-and-Join concurrency pattern
    System.exit(specs.reverse.map { spec =>
      Thread.sleep(spec.pause)

      val ClassSplitter(pkg, className) = spec.component
      val folder = pkg.replaceAll("\\.", "/")
      val btNode = (buildFile \\ "target").find(_.descendant.exists(n => (n \ "@package.dir").toString == folder))
      val buildTarget = btNode.map(n => (n \ "@name").toString.orElse("main")).getOrElse("main")
      val classpathref = btNode.map(n => (n \ "build-java" \ "@classpath").toString.orElse("base.classpath")).getOrElse("base.classpath")

      val pb = new ProcessBuilder("./ant", buildTarget, "run-config-target"
                                  , "-Dconfig.target.component="+ spec.component
                                  , "-Dconfig.target.args="+ spec.args
                                  , "-Dconfig.target.classpath="+ classpathref
                                  , "-Dconfig.target.local.ip="+ spec.host
                                  , "-Dconfig.target.registry.ip="+ spec.registry
                                  , "-Dconfig.target.libPath="+ spec.libPath
                                  , if (debugMode) "-debug" else "")
      
      if (debugMode) {
        pb.redirectErrorStream(true)
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(debugLog)))
      }
      startLoggingThreads(pb.start, spec)
    }.map(_.waitFor).sum)
  }

  def startLoggingThreads(p: Process, spec: ComponentLaunchSpecification): Process = {
    if (!debugMode) {
      startLoggingThread(p.getInputStream, spec, false)
      startLoggingThread(p.getErrorStream, spec, true)
    }
    spec.p = Some(p)
    p
  }

  val AntMessage = """^[^\]]+\]\s(.*)""".r
  val LogMessage = """^[^\]]+\]\s(\w+)\s*([\w.]+)\s*\-\s*(.*)""".r

  def startLoggingThread(is: InputStream, spec: ComponentLaunchSpecification, isError: Boolean) {
    new Thread(new Runnable() {
      override def run {
        val in = new BufferedReader(new InputStreamReader(is))
        Iterator.continually{in.readLine}.takeWhile(_ != null).foreach { _ match {
            case AntMessage(message) => message match {
              case LogMessage(lvl, cls, msg) => logLoggingMessage(spec, lvl, cls, msg)
              case _ => logPrintlnMessage(spec, message, isError)
            }
            case _ => Unit
          }
        }
        log.debug("Closing output stream...")
      }
    }, spec.component.split('.').lastOption.getOrElse("Unknown")).start()
  }

  def logLoggingMessage(spec: ComponentLaunchSpecification, level: String, clazz: String, message: String) {
    val clsLog = LogFactory.getLog(clazz)
    level match {
      case "FATAL" => clsLog.fatal(message)
      case "ERROR" => clsLog.error(message)
      case "WARN"  => clsLog.warn(message)
      case "INFO"  => clsLog.info(message)
      case "DEBUG" => clsLog.debug(message)
      case "TRACE" => clsLog.trace(message)
    }
  }

  def logPrintlnMessage(spec: ComponentLaunchSpecification, message: String, isError: Boolean) = isError match {
    case true => spec.printlnLog.error(message)
    case false => spec.printlnLog.info(message)
  }

  implicit class SuperString(s: String) {
    def orElse(t: String) = if (s.isEmpty) t else s
  }

}
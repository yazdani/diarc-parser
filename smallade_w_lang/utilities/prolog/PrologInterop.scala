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
package utilities.prolog

import alice.tuprolog.{Number, Prolog, Struct, Term, Var}
import scala.collection.JavaConversions._

/**
 * NOTE: if you find yourself getting alice.tuprolog.InvalidTheoryException: The argument '''' is not followed by either a ',' or ')'
 * it probably means you're calling atomate and using the resulting string to construct a new Struct.
 * For some reason that partiular constructor does the escaping for you, although Val does not, nor do Struct literals in larger prolog strings.
 */
object PrologInterop {
  // TODO: check if this regex needs to have anything escaped... assuming no for the time being
  def atomate(s: String): String = """^([a-z][\w_]*|[#$&*+-./:<=>?@^~]+|'.+')$""".r.findFirstIn(s) match {
    case Some(_) => s
    case None => "'%s'".format(s)
  }

  def deatomate(s: String): String = s.stripPrefix("'").stripSuffix("'")

  // TODO: consider some lossless encoding instead of just hammering the string into the appropriate shape
  def variablate(s: String): String = """^[A-Z_][\w_]*$""".r.findFirstIn(s) match {
    case Some(_) => s
    case None => """^[\w_]+""".r.findFirstIn("""\s""".r.replaceAllIn(s, "_")) match {
      case Some(ss) => ss.capitalize
      case None => "_"
    }
  }

  def termify(sym: com.Symbol, engine: Prolog): Term = sym match {
    case constant: com.Constant => new Struct(constant.getName)
    case variable: com.Variable => {
      val v = new Var(variablate(variable.getName))
      if (variable.getValue != null) {
        v.unify(engine, termify(variable.getValue, engine))
      }
      v
    }
    case term: com.Term => new Struct(term.getName, term.getArgs.map(termify(_, engine)).toArray)
    case _ => new Struct(sym.getName)
  }

  def cleanStructName(name: String): Option[String] = name match {
    case "" => None
    case "." => None
    case "[]" => None
    case _ => Some(deatomate(name))
  }

  object AtomE {
    def unapply(t: Term): Option[String] = t match {
      case s: Struct if s.getArity == 0 => Some(s.getName)
      case v: Var    if v.isBound       => unapply(v.getTerm)
      case _ => None
    }
  }

  object StructE {
    def unapply(t: Term): Option[(Option[String], Seq[Term])] = t match {
      case s: Struct => Some((cleanStructName(s.getName), (0 until s.getArity).map(s.getArg(_)).toSeq ))
      case v: Var if v.isBound => unapply(v.getTerm)
      case _ => None
    }
  }

  object VarE {
    def unapply(t: Term): Option[(String, Option[Term])] = t match {
      case v: Var if v.isBound => Some((v.getName, Some(v.getTerm)))
      case v: Var => Some((v.getName, None))
      case _ => None
    }
  }

  object NumberE {
    def unapply(t: Term): Option[Double] = t match {
      case n: Number => Some(n.doubleValue)
      case v: Var if v.isBound => unapply(v.getTerm)
      case _ => None
    }
  }
}

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
package utilities.scala

import scala.annotation.tailrec
import scala.concurrent.{Await, blocking, CanAwait, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object JavaInterop {
  import scala.language.implicitConversions

  // Renaming to beautify the Java API
  def JavaFuture[T](sf: scala.concurrent.Future[T]) = sf2jf(sf)

  class ScalaFutureWrapper[T](val sf: scala.concurrent.Future[T]) extends java.util.concurrent.Future[T] {
    def cancel(miir: Boolean) = false
    val isCancelled = false
    def isDone = sf.isCompleted
    def get: T = Await.result(sf, Duration.Inf)
    def get(timeout: Long, units: java.util.concurrent.TimeUnit): T = Await.result(sf, Duration(timeout, units))
  }

  class JavaFutureWrapper[T](val jf: java.util.concurrent.Future[T]) extends scala.concurrent.Future[T] {
    def isCompleted = jf.isDone

    def onComplete[U](func: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
      executor.execute(new Runnable {
        def run = func(Try( blocking { jf.get } ))
      })
    }

    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = atMost match {
      case Duration(timeout, units) => {
        jf.get(timeout, units)
        this
      }
    }

    def result(atMost: Duration)(implicit permit: CanAwait): T =
      atMost match { case Duration(timeout, units) => jf.get(timeout, units) }

    def value: Option[Try[T]] = (jf.isCancelled, jf.isDone) match {
      case (true, _) => Some(Failure(new Exception("Execution was cancelled!")))
      case (_, true) => Some(Success(jf.get))
      case _ => None
    }
  }

  implicit def sf2jf[T](sf: scala.concurrent.Future[T]): java.util.concurrent.Future[T] = sf match {
    case wrapper: JavaFutureWrapper[T] => wrapper.jf
    case _ => new ScalaFutureWrapper(sf)
  }

  implicit def jf2sf[T](jf: java.util.concurrent.Future[T]): scala.concurrent.Future[T] = jf match {
    case wrapper: ScalaFutureWrapper[T] => wrapper.sf
    case _ => new JavaFutureWrapper(jf)
  }
}

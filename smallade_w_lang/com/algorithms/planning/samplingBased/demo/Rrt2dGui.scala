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
package com.algorithms.planning.samplingBased.demo

import ade.gui.{ADEGuiCallHelper, SuperADEGuiPanel}
import com.algorithms.planning.{Path, StateContainer, StateSpace}
import com.algorithms.planning.samplingBased.BallTree.BallForest
import java.awt.{Canvas, Color}
import javax.swing.GroupLayout


class Rrt2dGui[T](helper: ADEGuiCallHelper, stateSpace: StateSpace[T], refreshMillis: Int)
    extends SuperADEGuiPanel(helper, refreshMillis) {
  def this(helper: ADEGuiCallHelper, stateSpace: StateSpace[T]) = this(helper, stateSpace, 1000)

  val spec = call("getVisualizationParams", classOf[RrtVisualizationParams])

  val canvas = new Canvas
  // here be netbeans gui designer magick
  val gl = new GroupLayout(this);
  this.setLayout(gl);
  gl.setHorizontalGroup(
    gl.createParallelGroup(GroupLayout.Alignment.LEADING)
    .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 400, Short.MaxValue)
  );
  gl.setVerticalGroup(
    gl.createParallelGroup(GroupLayout.Alignment.LEADING)
    .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 300, Short.MaxValue)
  );

  def drawPoint(state: T) {
    val (x, y) = stateSpace.projectToUnit2D(state)
    canvas.getGraphics.drawRect((x * canvas.getWidth).toInt
                                , (y * canvas.getHeight).toInt
                                , spec.pointSize
                                , spec.pointSize)    
  }

  def drawEdge(a: T, b: T) {
    val (ax, ay) = stateSpace.projectToUnit2D(a)
    val (bx, by) = stateSpace.projectToUnit2D(b)
    val width = canvas.getWidth
    val height = canvas.getHeight
    canvas.getGraphics.drawLine((ax * width).toInt
                                , (ay * height).toInt
                                , (bx *  width).toInt
                                , (by * height).toInt)
  }

  def drawBall(ball: (T, Double)) {
    val (pt, rad) = ball
    val (x, y) = stateSpace.projectToUnit2D(pt)
    if (spec.pointSize > 0) {
      canvas.getGraphics.drawRect((x * canvas.getWidth).toInt
                                  , (y * canvas.getHeight).toInt
                                  , spec.pointSize
                                  , spec.pointSize)    
    }
    val r = stateSpace.projectToUnitDistance(rad)
    val circleWidth = r * canvas.getWidth * 2
    val circleHeight = r * canvas.getHeight * 2
    canvas.getGraphics.drawOval((x * canvas.getWidth - circleWidth/2).toInt
                                , (y * canvas.getHeight - circleHeight/2).toInt
                                , circleWidth.toInt
                                , circleHeight.toInt)
  }

  def drawPath(path: Path[T], color: Color = Color.red) {
    val prev = canvas.getForeground
    canvas.setForeground(color)
    val width = canvas.getWidth
    val height = canvas.getHeight

    val divs = 100
    (0 to divs).map(d => path.stateAt(d.toDouble/divs)).sliding(2).foreach { pair =>
      val (ax, ay) = stateSpace.projectToUnit2D(pair(0))
      val (bx, by) = stateSpace.projectToUnit2D(pair(1))
      canvas.getGraphics.drawLine((ax * width).toInt
                                  , (ay * height).toInt
                                  , (bx * width).toInt
                                  , (by * height).toInt)
    }
    canvas.setForeground(prev)
  }

  override def refreshGui() {
    canvas.getGraphics.clearRect(0, 0, canvas.getWidth, canvas.getHeight)
    val data = call("getData", classOf[StateContainer[T]])
    data match {
      case null => Unit
      case forest: BallForest[T] => forest.balls.foreach(drawBall(_))
      case _ => data.states.foreach { state => if (spec.pointSize > 0) drawPoint(state) }
    }
    if (spec.drawLines && data != null) {
      data.states.foreach { state => data.getConnections(state).foreach {drawEdge(state, _)} }
    }

    call("getSolution", classOf[Option[Path[T]]]) match {
      case Some(sln) => drawPath(sln)
      case _ => Unit
    }

  }
}

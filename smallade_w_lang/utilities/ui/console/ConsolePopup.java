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
package utilities.ui.console;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This is a simple popup console to bypass a technical limitation of using forking
 * a new JVM to run servers.
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class ConsolePopup extends ADEGuiPanel {
  ConsoleInputStream in = new ConsoleInputStream();
  OutputStream out = null;

  /** Creates new form ConsolePopup */
  public ConsolePopup(ADEGuiCallHelper helper) {
    super(helper, 100);
    initComponents();

    out = new OutputStream() {
      @Override
      public void write(final int b) throws IOException {
        updateTextPane(String.valueOf((char) b));
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        updateTextPane(new String(b, off, len));
      }

      @Override
      public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
      }
    };

    System.setIn(in);
    System.setOut(new PrintStream(out));
  }

  private void updateTextPane(String text) {
    if (outputBox != null) {
      outputBox.append(text);
    }
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane1 = new javax.swing.JScrollPane();
    outputBox = new javax.swing.JTextArea();
    inputBox = new javax.swing.JTextField();

    addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        formKeyPressed(evt);
      }
    });

    outputBox.setColumns(20);
    outputBox.setEditable(false);
    outputBox.setRows(5);
    outputBox.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        outputBoxKeyPressed(evt);
      }
    });
    jScrollPane1.setViewportView(outputBox);

    inputBox.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        handleKeyPress(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(inputBox, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
      .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(inputBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );
  }// </editor-fold>//GEN-END:initComponents

  private void handleKeyPress(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_handleKeyPress
    // if the user has pressed enter
    if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
      in.addString(inputBox.getText());
      updateTextPane("> " + inputBox.getText());
      inputBox.setText("");
    }
  }//GEN-LAST:event_handleKeyPress

  private void outputBoxKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_outputBoxKeyPressed
    handleKeyPress(evt);
  }//GEN-LAST:event_outputBoxKeyPressed

  private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
    handleKeyPress(evt);
  }//GEN-LAST:event_formKeyPressed

  @Override
  public void refreshGui() {
    if (jScrollPane1 != null) {
      jScrollPane1.repaint();
      outputBox.repaint();
      inputBox.repaint();
    }
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField inputBox;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTextArea outputBox;
  // End of variables declaration//GEN-END:variables
}

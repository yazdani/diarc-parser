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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a handy stdin/stdout wrapper GUI.
 *
 * The idea is that if you're launching multiple components in the same terminal
 * there's no good way to separate stdio, so instead tell each component to use
 * this as one of its GUIs, and then you'll get a window for each.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class ConsoleInputStream extends InputStream {
  ConcurrentLinkedQueue<String> clq = new ConcurrentLinkedQueue<String>();
  int byteCount = 0;
  byte[] currentString = null;
  int currentOffset = 0;
  Log log = LogFactory.getLog(ConsoleInputStream.class);

  @Override
  public int available() {
    return byteCount;
  }

  @Override
  public int read() throws IOException {
    do {
      // blocking here is part of the spec: http://docs.oracle.com/javase/1.4.2/docs/api/java/io/InputStream.html#read()
      try {
        if (currentString == null) {
          Thread.sleep(100);
        }
      } catch (InterruptedException ex) {
        log.warn("Hrm?  Wha?  Oh, you woke me up while I was sleeping!", ex);
      }
      updateCurrentString();
    } while (currentString == null);

    byteCount--;
    return currentString[currentOffset++];
  }

  void updateCurrentString() {
    if (currentString == null || currentOffset >= currentString.length) {
      if (clq.isEmpty()) {
        currentString = null;
        byteCount = 0;
      } else {
        currentString = clq.remove().getBytes();
        currentOffset = 0;
      }
    }
  }

  void addString(String s) {
    clq.offer(s);
    byteCount += s.getBytes().length;
  }
}

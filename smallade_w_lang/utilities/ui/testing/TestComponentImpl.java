/*
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Evan Krause
 *
 * Copyright 1997-2013 Evan Krausep and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Evan Krause at <evan.krause@tufts.edu>
 */

package utilities.ui.testing;

import ade.ADEComponent;
import ade.ADEComponentImpl;
import ade.ADEException;
import ade.ADEGlobals;
import com.Predicate;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.vecmath.Point3d;

/**
 * Generic Test GUI for any ADE component or interface. This uses Java
 * reflection to expose all of the methods made available in the component's
 * interface. Note that all parameter arguments require instantiating the
 * parameter's Object type from a String and the only way to do this is
 * to hand-write each needed type. Add any needed type to the growing if-else
 * statement in MethodCallHelper.callMethod() below.
 * 
 * @author Evan Krause
 */
public class TestComponentImpl extends ADEComponentImpl implements TestComponent {

  private Object testComponentRef;
  private static Class testComponentClass;// = com.interfaces.VisionComponent.class;
  private final JFrame testFrame;
  private final JPanel testPanel;
  private final JPanel methodsPanel;
  private final JPanel resultPanel;
  private final JTextArea resultTxtArea;
  //panel params
  private int nextGridY = 0;
  private int maxGridWidth = 0;
  private final HashMap<JButton, MethodCallHelper> classMethods = new HashMap<JButton, MethodCallHelper>();

  public TestComponentImpl() throws RemoteException {
    super();

    if (testComponentClass == null) {
      System.err.println("[TestComponent] ERROR component not set. Aborting!");
      System.exit(0);
    }

    //set up JFrame and Panels
    testFrame = new JFrame("Test Component Panel: " + testComponentClass.getName());
    methodsPanel = new JPanel(new GridBagLayout());
    resultPanel = new JPanel(new BorderLayout());
    
    testPanel = new JPanel(new BorderLayout());
    JScrollPane testPanelScroller = new JScrollPane(testPanel);
    testPanelScroller.setWheelScrollingEnabled(true);
    testPanelScroller.getVerticalScrollBar().setUnitIncrement(16);
    testPanel.setLayout(new BorderLayout());
    testPanel.add(methodsPanel, BorderLayout.NORTH);
    testPanel.add(resultPanel, BorderLayout.CENTER);
    
    testFrame.setResizable(true);
    testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    testFrame.setBackground(null);
    testFrame.add(testPanelScroller);
    testFrame.setVisible(true);
    
    //add menu bar with save/load options
    JMenuItem save_menuItm = new JMenuItem("Save Entries");
    JMenuItem load_menuItm = new JMenuItem("Load Entries");
    save_menuItm.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        save_menuItmActionPerformed(evt);
      }
    });
    load_menuItm.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        load_menuItmActionPerformed(evt);
      }
    });
    
    JMenu file_menu = new JMenu();
    file_menu.add(save_menuItm);
    file_menu.add(load_menuItm);
    file_menu.setText("File");
    
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(file_menu);

    testFrame.setJMenuBar(menuBar);

    //init panel with methods and argument fields
    for (Method method : testComponentClass.getMethods()) {
      //only add methods that aren't part of ADEComponent interface
      boolean isADEmethod = false;
      for (Method m : ADEComponent.class.getMethods()) {
        if (m.equals(method)) {
          isADEmethod = true;
          break;
        }
      }

      if (!isADEmethod) {
        addMethod(method);
      }
    }

    //add results text area
    resultTxtArea = new JTextArea();
    resultTxtArea.setEnabled(true);
    resultTxtArea.setText("Results");
    resultPanel.add(resultTxtArea, BorderLayout.CENTER);

    //connect to ade component
    registerNewComponentNotification(new String[][]{{"type", testComponentClass.getName()}}, true);
    testComponentRef = getClient(testComponentClass.getName());
  }
  
  private void save_menuItmActionPerformed(java.awt.event.ActionEvent evt) {
    List<String> inputs = new ArrayList<String>();
    for (Component component : methodsPanel.getComponents()) {
      if (component instanceof JTextField) {
        JTextField textField = (JTextField) component;
        inputs.add(textField.getText());
      }
    }

    try {
      //use buffering
      OutputStream file = new FileOutputStream("tmp/" + testComponentClass.getName());
      OutputStream buffer = new BufferedOutputStream(file);
      ObjectOutput output = new ObjectOutputStream(buffer);
      try {
        output.writeObject(inputs);
      } finally {
        output.close();
      }
    } catch (IOException ex) {
      System.err.println("[TestComponent::save] " + ex);
    }
  }
  
  private void load_menuItmActionPerformed(java.awt.event.ActionEvent evt) {
    try {
      //use buffering
      InputStream file = new FileInputStream("tmp/" + testComponentClass.getName());
      InputStream buffer = new BufferedInputStream(file);
      ObjectInput input = new ObjectInputStream(buffer);
      try {
        List<String> inputs = (List<String>) input.readObject();
        
        //fill text fields
        JTextField textField;
        int txtFieldCount = 0;
        for (Component component : methodsPanel.getComponents()) {
          if (component instanceof JTextField) {
            textField = (JTextField) component;
            textField.setText(inputs.get(txtFieldCount++));
          }
        }
      } catch (ClassNotFoundException ex) {
        System.err.println("[TestComponent::load] " + ex);
      } finally {
        input.close();
      }
    } catch (IOException ex) {
      System.err.println("[TestComponent::load] " + ex);
    }
  }

  private void addMethod(final Method method) {

    try {
      EventQueue.invokeAndWait(new Runnable() {
        @Override
        public void run() {

          //add submitt button
          JButton submitBtn = new JButton("submit");
          submitBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              submitBtnActionPerformed(evt);
            }
          });
          GridBagConstraints c = new GridBagConstraints();
          c.gridx = 0;
          c.gridy = nextGridY++;
          methodsPanel.add(submitBtn, c);

          //MethodCallHelper paired with submit button -- filled in later in this method
          MethodCallHelper methodCallHelper = new MethodCallHelper(method);
          classMethods.put(submitBtn, methodCallHelper);

          //set methodLabel name / location
          JLabel methodLabel = new JLabel();
          //methodLabel.setFont(new Font("Dialog", Font.BOLD, 14));
          methodLabel.setText(method.getName());
          c.gridx += 1;
          methodsPanel.add(methodLabel, c);

          //add method params info / location
          JTextField methodParamTxtField;
          for (Type methodParamType : method.getGenericParameterTypes()) {
            c.gridx += 1;
            JLabel methodParamLabel = new JLabel(methodParamToString(methodParamType));
            methodsPanel.add(methodParamLabel, c);

            methodParamTxtField = new JTextField(10);
            methodLabel.setSize(100, methodLabel.getSize().height);
            //methodLabel.setFont(new Font("Dialog", Font.BOLD, 14));
            c.gridx += 1;
            methodsPanel.add(methodParamTxtField, c);
            methodCallHelper.addParamArg(methodParamTxtField);
          }

          //keep track of grid width
          if (c.gridx > maxGridWidth) {
            maxGridWidth = c.gridx;
          }

          //resize frame
          testFrame.setSize(methodsPanel.getSize().width + 40, methodsPanel.getSize().height + 200);
        }
      });
    } catch (Exception e) {
      System.err.println("[TestComponent::addMethod] " + e.toString());
    }
  }

  private void submitBtnActionPerformed(java.awt.event.ActionEvent evt) {
    JButton source = (JButton) evt.getSource();
    classMethods.get(source).callMethod();
  }

  private String methodParamToString(Type methodParamType) {
    String result;
    if (methodParamType instanceof ParameterizedType) {
      //if is a parameterized type, get generic class info too
      ParameterizedType pType = (ParameterizedType) methodParamType;
      Class<?> genericClass = (Class<?>) pType.getActualTypeArguments()[0];
      Class methodParamClass = (Class) pType.getRawType();
      result = methodParamClass.getSimpleName() + "<" + genericClass.getSimpleName() + ">";
    } else {
      //not parameterized type, just get class info
      Class methodParamClass = (Class) methodParamType;
      result = methodParamClass.getSimpleName();
    }
    return result;
  }

  /**
   * Helper class that keeps track of necessary info to make an ADE call.
   */
  class MethodCallHelper {

    private Method method;
    List<JTextField> arguments = new ArrayList<JTextField>();

    MethodCallHelper(Method m) {
      method = m;
    }

    /**
     * Adds a text field to pull from when this <call> is called. These must be
     * added in the same order as the actual method arguments appear.
     *
     * @param txtField
     */
    public void addParamArg(JTextField argument) {
      arguments.add(argument);
    }

    /**
     * Parse all text fields, convert them to their corresponding java types and
     * call the ADE component.
     *
     * @return Object returned by ADE call.
     */
    public Object callMethod() {
      Object[] args = new Object[arguments.size()];

      try {

        //convert all method parameters from string to java type
        for (int i = 0; i < method.getParameterTypes().length; ++i) {
          Type paramType = method.getGenericParameterTypes()[i];
          String paramStr = methodParamToString(paramType);
          String argStr = arguments.get(i).getText();
          if (paramStr.equalsIgnoreCase("predicate")) {
            args[i] = utilities.Util.createPredicate(argStr);
          } else if (paramStr.equalsIgnoreCase("float")) {
            args[i] = Float.parseFloat(argStr);
          } else if (paramStr.equalsIgnoreCase("double")) {
            args[i] = Double.parseDouble(argStr);
          } else if (paramStr.equalsIgnoreCase("int")) {
            args[i] = Integer.parseInt(argStr);
          } else if (paramStr.equalsIgnoreCase("long")) {
            args[i] = Long.parseLong(argStr);
          } else if (paramStr.equalsIgnoreCase("boolean")) {
            args[i] = Boolean.parseBoolean(argStr);
          } else if (paramStr.equalsIgnoreCase("string")) {
            args[i] = argStr;
          } else if (paramStr.equalsIgnoreCase("List<Predicate>")) {
            List<Predicate> predicates = new ArrayList<Predicate>();
            String[] splitArgs = argStr.split(";");
            for (String predicateStr : splitArgs) {
              predicates.add(utilities.Util.createPredicate(predicateStr));
            }
            args[i] = predicates;
          } else if (paramStr.equalsIgnoreCase("Point3d")) {
		String [] point = argStr.replace("(","").replace(")","").split(",");
		args[i] = new Point3d(Double.parseDouble(point[0]),
                                      Double.parseDouble(point[1]),
		                      Double.parseDouble(point[2]));
	  } else {
            String error = "[TestComponent::callMethod] conversion to " + paramStr + " not supported yet";
            //System.err.println(error);
            resultTxtArea.setText(error);
            return null;
          }
        }
        try {
          Object result = call(0, testComponentRef, method.getName(), args);
          if (result != null) {
            resultTxtArea.setText(method.getName() + ":\n" + result.toString());
          } else {
            resultTxtArea.setText(method.getName() + ":\n" + "null");
          }

        } catch (ADEException e) {
          resultTxtArea.setText(e.toString());
        }
      } catch (Exception e) {
        resultTxtArea.setText(e.toString());
      }
      return null;
    }
  }

  // ********************************************************************
  // *** abstract methods in ADEComponentImpl that need to be implemented
  // ********************************************************************
  /**
   * This method will be activated whenever a client calls the getClient method.
   * Any connection-specific initialization should be included here, either
   * general or user-specific.
   *
   * @param user the ID of the user/client that gained a connection
   */
  @Override
  protected void clientConnectReact(String user) {
  }

  /**
   * This method will be activated whenever a client that has called the
   * getClient method fails to update (meaning that the heartbeat signal has not
   * been received by the reaper), allowing both general and user specific
   * reactions to lost connections. If it returns true, the client's connection
   * is removed.
   *
   * @param user the ID of the user/client that lost a connection
   */
  @Override
  protected boolean clientDownReact(String user) {
    return false;
  }

  /**
   * Do not call. Public so that the registry can get at it.
   *
   * @param newserverkey
   */
  @Override
  public void notifyComponentJoined(final String newserverkey) {

    System.out.println("[TestComponent::notifyComponentJoined] " + newserverkey);
    new Thread() {
      @Override
      public void run() {
        if (testComponentRef == null) {
          testComponentRef = getClient(testComponentClass.getName());
        }
      }
    }.start();
  }

  /**
   * This method will be activated whenever the pseudo-reference returns a
   * remote exception (e.g., the server this is sending a heartbeat to has
   * failed).
   *
   * @param s the type of {@link ade.ADEComponent ADEComponent} that failed
   */
  @Override
  protected void componentDownReact(String serverkey, String[][] constraints) {
    System.out.println("[TestComponent::componentDownReact] " + serverkey);

      testComponentRef = null;
  }

  /**
   * This method will be activated whenever the heartbeat reconnects to a client
   * (e.g., the server this is sending a heartbeat to has failed and then
   * recovered). <b>NOTE:</b> the pseudo-reference will not be set until
   * <b>after</b> this method is executed. To perform operations on the newly
   * (re)acquired reference, you must use the <tt>ref</tt> parameter object.
   *
   * @param s the ID of the {@link ade.ADEComponent ADEComponent} that connected
   * @param ref the pseudo-reference for the requested server
   */
  @Override
  protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
    System.out.println("[TestComponent::componentConnectReact] " + serverkey);

    if (testComponentRef == null) {
      testComponentRef = ref;
    }
  }

  /**
   * Adds additional local checks for credentials before allowing a shutdown.
   *
   * @param credentials the Object presented for verification
   * @return must return "false" if shutdown is denied, true if permitted
   */
  @Override
  protected boolean localrequestShutdown(Object credentials) {
    return true;
  }

  /**
   * Adds additional local checks for credentials before allowing an unclean
   * shutdown.
   *
   * @param credentials the Object presented for verification
   * @return <tt>false</tt> if shutdown is denied, <tt>true</tt> if permitted
   */
  @Override
  protected boolean localrequestKill(Object credentials) {
    return true;
  }

  /**
   * Implements the local shutdown mechanism that derived classes need to
   * implement to cleanly shutdown. Note that the process initiated by a call to
   * <tt>stopMyself</tt> will eventually call this method as a result of the
   * {@link ade.ADEComponentImpl#requestShutdown requestShutdown} method.
   */
  @Override
  protected void localshutdown() {
  }

  @Override
  protected void componentNotify(Object ref, ADEGlobals.RecoveryState recState) {
  }

  /**
   * Parses command line arguments specific to this ADEComponent.
   *
   * @param args the custom command line arguments
   * @return <tt>true</tt> if all <tt>args</tt> are recognized, <tt>false</tt>
   * otherwise
   */
  @Override
  protected boolean parseadditionalargs(String[] args) {

    boolean found = false;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-component") || args[i].equalsIgnoreCase("-comp")) {
        try {
          testComponentClass = Class.forName(args[++i]);
        } catch (ClassNotFoundException e) {
          System.err.println("[TestComponent] ERROR class not found for: " + args[i]);
        }
        found = true;
      }
    }
    return found;
  }

  @Override
  protected String additionalUsageInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("Component-specific options:\n\n");
    sb.append("-comp(onent) [ADEComponent]   <ADE Component to parse and make calls to\n>");
    return sb.toString();
  }

  /**
   * The server is always ready to provide its service after it has come up
   */
  @Override
  protected boolean localServicesReady() {
    return true;
  }

  @Override
  protected void updateComponent() {
  }

  @Override
  protected void updateFromLog(String logEntry) {
  }
}

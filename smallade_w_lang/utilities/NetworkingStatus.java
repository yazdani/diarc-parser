package utilities;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkingStatus {

  public static String ETHERNET_DEVICE = "eth0";

  public static boolean isEthernetAvailable() {
    try {
      NetworkInterface ni = NetworkInterface.getByName(ETHERNET_DEVICE);
      if(ni == null) return false;
      else {  
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        return addresses.hasMoreElements();
      }
    } catch (SocketException se) {
      se.printStackTrace();
    }
    return false; 
  }

  // main method for testing purposes
  public static void main(String[] args) {
    System.out.println(NetworkingStatus.isEthernetAvailable());
  }

}

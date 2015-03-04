import java.io.File;
public class JHome {
    public static void main(String[] args) {
	//System.out.println(System.getProperty("java.home"));
	System.out.println(new File(System.getProperty("java.home")).getParent());
    }
}

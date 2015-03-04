package utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.adesim.util.SimUtil;

public class ColorConverter {

	public static final ColorTable colorTable = new ColorTable();
	//       note that color names are all in lower case for consistency's sake


	public static class ColorTable extends LinkedHashMap<String, Color> {
		private static final long serialVersionUID = 1L;

		// linked map simply so that the order of colors is the same one as when they
		//    are added.  useful for debugging and graphic display (see "main" method)

		public ColorTable() {
			// using 48 colors obtained by an extensive color survey:  http://blog.xkcd.com/2010/05/03/color-survey-results/
			//    also adding white, as for some reason it's missing from the list.
			addColor("purple", "7e1e9c");
			addColor("green", "15b01a");
			addColor("blue", "0343df");
			addColor("pink", "ff81c0");
			addColor("brown", "653700");
			addColor("red", "e50000");
			addColor("light blue", "95d0fc");
			addColor("teal", "029386");
			addColor("orange", "f97306");
			addColor("light green", "96f97b");
			addColor("magenta", "c20078");
			addColor("yellow", "ffff14");
			addColor("sky blue", "75bbfd");
			addColor("grey", "929591");
			addColor("light grey", "d3d3d3");
			addColor("lime green", "89fe05");
			addColor("light purple", "bf77f6");
			addColor("violet", "9a0eea");
			addColor("dark green", "033500");
			addColor("turquoise", "06c2ac");
			addColor("lavender", "c79fef");
			addColor("dark blue", "00035b");
			addColor("tan", "dab26f");
			addColor("cyan", "00ffff");
			addColor("aqua", "13eac9");
			addColor("forest green", "06470c");
			addColor("mauve", "ae7181");
			addColor("dark purple", "35063e");
			addColor("bright green", "01ff07");
			addColor("maroon", "650021");
			addColor("olive", "6e750e");
			addColor("salmon", "ff796c");
			addColor("beige", "e6daa6");
			addColor("royal blue", "0504aa");
			addColor("navy blue", "001146");
			addColor("lilac", "cea2fd");
			addColor("black", "000000");
			addColor("hot pink", "ff028d");
			addColor("light brown", "ad8150");
			addColor("pale green", "c7fdb5");
			addColor("peach", "ffb07c");
			addColor("olive green", "677a04");
			addColor("dark pink", "cb416b");
			addColor("periwinkle", "8e82fe");
			addColor("sea green", "53fca1");
			addColor("lime", "aaff32");
			addColor("indigo", "380282");
			addColor("mustard", "ceb301");
			addColor("light pink", "ffd1df");
			addColor("white", "ffffff");



//			alternatively, could have used the 17 standardly defined CSS colors:  the 16 HTML ones + orange.
//			     however, probably better to use "empirically-obtained" colors such as above, rather than
//			     color specs developed by HTML programmers!
//			addColor("White", "FFFFFF");
//			addColor("Silver","C0C0C0");
//			addColor("Gray", "808080");
//			addColor("Black", "000000");
//			addColor("Red", "FF0000");
//			addColor("Maroon", "800000");
//			addColor("Yellow", "FFFF00");
//			addColor("Olive", "808000");
//			addColor("Lime", "00FF00");
//			addColor("Green", "008000");
//			addColor("Aqua", "00FFFF");
//			addColor("Teal", "008080");
//			addColor("Blue", "0000FF");
//			addColor("Navy", "000080");
//			addColor("Fuchsia", "FF00FF");
//			addColor("Purple", "800080");
//			addColor("Orange", "FFA500");

		}

		private void addColor(String name, String hex) {
			// color names are all in lower case for consistency's sake
			this.put(name.toLowerCase(), new Color(Integer.parseInt(hex, 16)));
		}
	}


	public static Color getColorByName(String name, boolean throwExceptionIfNotFound) {
		Color color = colorTable.get(name.toLowerCase());
		if (  (color == null) && throwExceptionIfNotFound  ) {
			throw new RuntimeException("Could not find color name \"" + name +
					"\" in " + ColorConverter.class.getCanonicalName());
		} else {
			return color;
		}
	}

	/** returns closest matching color name based on the passed-in color.
	 * 	Note that color names are all in lower case for consistency's sake */
	public static String getColorName(Color color) {
		//System.out.println("Current color = " + color.getRed() + " " + color.getGreen() + " " + color.getBlue());
		double bestMatchDistance = 255*255*255;
		String bestMatchColorName = null;
		for (Entry<String, Color> eachColorPair : colorTable.entrySet()) {
			double colorDistance = getColorDistance(eachColorPair.getValue(), color);
			//System.out.println("Examining color " + eachColorPair.getValue().getRed() + " " + eachColorPair.getValue().getGreen() + " " + eachColorPair.getValue().getBlue());
			//System.out.println("Distance for color " + eachColorPair.getKey() + " == " + colorDistance);
			if (colorDistance < bestMatchDistance) {
				bestMatchColorName = eachColorPair.getKey();
				bestMatchDistance = colorDistance;
			}
		}

		return bestMatchColorName;
	}

	/** returns color name ONLY if it's a perfect match to one of the colors in the colorTable.
	 * Otherwise, return null; */
	public static String getExactMatchColorNameIfAny(Color color) {
		for (Entry<String, Color> eachColorPair : colorTable.entrySet()) {
			if (eachColorPair.getValue().equals(color)) {
				return eachColorPair.getKey();
			}
		}
		// if haven't quit yet:
		return null;
	}

	private static double getColorDistance(Color color1, Color color2) {
		int dRed = color1.getRed() - color2.getRed();
		int dGreen = color1.getGreen() - color2.getGreen();
		int dBlue = color1.getBlue() - color2.getBlue();
		return Math.sqrt( dRed*dRed + dGreen*dGreen + dBlue*dBlue );
	}








	/** main method for debugging purposes, just to display a grid to ensure that colors look right*/
	public static void main(String [] args) {
		createDisplayGrid();

		createTesterFrame();
	}

	private static void createTesterFrame() {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(600, 600);

		JPanel contentPane = new JPanel(new BorderLayout());
		final JColorChooser colorChooserPanel = new JColorChooser();
		contentPane.add(colorChooserPanel, BorderLayout.CENTER);
		final JLabel resultLabel = new JLabel("Resulting color is:  ");
		resultLabel.setFont(new Font("Arial", Font.BOLD, 36));
		contentPane.add(resultLabel, BorderLayout.SOUTH);

		colorChooserPanel.getSelectionModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateResultLable(resultLabel, colorChooserPanel);
			}
		});

		updateResultLable(resultLabel, colorChooserPanel);
		frame.setContentPane(contentPane);
		frame.setVisible(true);
	}

	private static void updateResultLable(JLabel resultLabel,
			JColorChooser colorChooserPanel) {
		resultLabel.setText("Resulting color is:  " + getColorName(colorChooserPanel.getColor()));
	}



	private static void createDisplayGrid() {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(600, 600);

		JPanel contentPane = new JPanel();
		int rows = (int) Math.sqrt(colorTable.size());
		int cols = (((colorTable.size() - 1) / rows) + 1);
		contentPane.setLayout(new GridLayout(rows, cols, 10, 10));
		for (final Entry<String, Color> each : colorTable.entrySet()) {
			JLabel label = new JLabel() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					SimUtil.drawStringIntoRectangle(each.getKey(), new Rectangle(0, 0, this.getWidth(), this.getHeight()/2), (Graphics2D) g);
					g.setColor(each.getValue());
					g.fillRect(0, this.getHeight()/2, this.getWidth(), this.getHeight()/2);
				}
			};
			label.setToolTipText(each.getKey());
			contentPane.add(label);
		}
		frame.setContentPane(contentPane);

		frame.setVisible(true);
	}


}

/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.gui;

import boofcv.gui.dialogs.OpenImageSetDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class BoofSwingUtil {
	public static final String KEY_RECENT_FILES = "RecentFiles";
	public static final String KEY_PREVIOUS_SELECTION = "PreviouslySelected";

	public static final double MIN_ZOOM = 0.01;
	public static final double MAX_ZOOM = 50;

	public static File saveFileChooser(Component parent, FileTypes ...filters) {
		return fileChooser(parent,false,new File(".").getPath(),filters);
	}

	public static String[] openImageSetChooser(Window parent , OpenImageSetDialog.Mode mode , int numberOfImages) {
		Preferences prefs;
		if( parent == null ) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		}
		File defaultPath = BoofSwingUtil.directoryUserHome();
		String previousPath=prefs.get(KEY_PREVIOUS_SELECTION, defaultPath.getPath());

		String[] response = OpenImageSetDialog.showDialog(new File(previousPath),mode,numberOfImages,parent);

		if( response != null ) {
			prefs.put(KEY_PREVIOUS_SELECTION, new File(response[0]).getParent());
		}
		return response;
	}

	public static File openFileChooser(Component parent, FileTypes ...filters) {
		return openFileChooser(parent,new File(".").getPath(),filters);
	}

	public static File openFileChooser(Component parent, String defaultPath , FileTypes ...filters) {
		return fileChooser(parent,true,defaultPath,filters);
	}

	public static File fileChooser(Component parent, boolean openFile, String defaultPath , FileTypes ...filters) {

		Preferences prefs;
		if( parent == null ) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		}
		String previousPath=prefs.get(KEY_PREVIOUS_SELECTION, defaultPath);
		JFileChooser chooser = new JFileChooser(previousPath);
		chooser.setSelectedFile(new File(previousPath));

		boolean selectDirectories = false;
		escape:for( FileTypes t : filters ) {
			FileNameExtensionFilter ff;
			switch( t ) {
				case IMAGES:
					ff = new FileNameExtensionFilter("Images", ImageIO.getReaderFileSuffixes());
					break;
				case VIDEOS:
					ff = new FileNameExtensionFilter("Videos","mpg","mp4","mov","avi","wmv");
					break;

				case DIRECTORIES:
					selectDirectories = true;
					break escape;
				default:
					throw new RuntimeException("Unknown file type");
			}
			chooser.addChoosableFileFilter(ff);
		}

		if( selectDirectories && filters.length > 1 ) {
			throw new IllegalArgumentException("specified directories and other filters. Can only just do directories");
		}
		if( selectDirectories ) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		} else if( filters.length > 0 ) {
			chooser.setFileFilter(chooser.getChoosableFileFilters()[1]);
		}

		File selected = null;
		int returnVal = openFile ? chooser.showOpenDialog(parent) : chooser.showSaveDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selected = chooser.getSelectedFile();
			prefs.put(KEY_PREVIOUS_SELECTION, selected.getPath());
		}
		return selected;
	}

	public static java.util.List<String> getListOfRecentFiles(Component parent) {

		Preferences prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		String encodedString =prefs.get(KEY_RECENT_FILES, "");

		String []fileNames = encodedString.split("\n");

		java.util.List<String> output = new ArrayList<>();
		for( String f : fileNames ) {
			output.add(f);
		}
		return output;
	}

	public static void addToRecentFiles( Component parent , String filePath ) {
		java.util.List<String> files = getListOfRecentFiles(parent);

		files.remove(filePath);

		if( files.size() >= 10 ) {
			files.remove(9);
		}
		files.add(0,filePath);

		String encoded = "";
		for (int i = 0; i < files.size(); i++) {
			encoded += files.get(i);
			if( i < files.size()-1 ) {
				encoded += "\n";
			}
		}
		Preferences prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		prefs.put(KEY_RECENT_FILES,encoded);
	}

	public static void invokeNowOrLater(Runnable r ) {
		if(SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	public static void checkGuiThread() {
		if( !SwingUtilities.isEventDispatchThread() )
			throw new RuntimeException("Must be run in UI thread");
	}

	/**
	 * Select a zoom which will allow the entire image to be shown in the panel
	 */
	public static double selectZoomToShowAll(JComponent panel , int width , int height ) {
		int w = panel.getWidth();
		int h = panel.getHeight();
		if( w == 0 ) {
			w = panel.getPreferredSize().width;
			h = panel.getPreferredSize().height;
		}

		double scale = Math.max(width/(double)w,height/(double)h);
		if( scale > 1.0 ) {
			return 1.0/scale;
		} else {
			return 1.0;
		}
	}

	/**
	 * Figures out what the scale should be to fit the window inside the default display
	 */
	public static double selectZoomToFitInDisplay( int width , int height ) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double w = screenSize.getWidth();
		double h = screenSize.getHeight();

		double scale = Math.max(width/w,height/h);
		if( scale > 1.0 ) {
			return 1.0/scale;
		} else {
			return 1.0;
		}
	}

	public static JFormattedTextField createTextField( int current , int min , int max ) {
		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(min);
		formatter.setMaximum(max);
		formatter.setAllowsInvalid(true);
//		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField field = new JFormattedTextField(formatter);
		field.setHorizontalAlignment(JTextField.RIGHT);
		field.setValue(current);
		return field;
	}

	public static JFormattedTextField createTextField( double current , double min , double max ) {
		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Double.class);
		if( !Double.isNaN(min) )
			formatter.setMinimum(min);
		if( !Double.isNaN(max) )
			formatter.setMaximum(max);
		formatter.setAllowsInvalid(true);
//		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField field = new JFormattedTextField(formatter);
		field.setHorizontalAlignment(JTextField.RIGHT);
		field.setValue(current);
		return field;
	}

	public static void setMenuItemKeys( JMenuItem menu , int mnmonic , int accelerator ) {
		menu.setMnemonic(mnmonic);
		menu.setAccelerator(KeyStroke.getKeyStroke(accelerator, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
	}

	public static void warningDialog(Component component, Exception e) {
		JOptionPane.showMessageDialog(component, e.getMessage());
	}

	/**
	 * Sets rendering hints that will enable antialiasing and make sub pixel rendering look good
	 * @param g2
	 */
	public static void antialiasing( Graphics2D g2 ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public static JButton createButtonIconGUI(String path, int width, int height) {
		return createButtonIcon("boofcv/gui/"+path,width,height,true);
	}

	public static JButton createButtonIcon(String path, int width, int height, boolean opaque) {
		try {

			BufferedImage b;
//			if( path.endsWith("svg")) {
//				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
//				SVGUniverse svg = new SVGUniverse();
//				URI uri = svg.loadSVG(classloader.getResource( path));
//				SVGDiagram diagram = svg.getDiagram(uri);
//				diagram.setIgnoringClipHeuristic(true);
//
//				double scale = computeButtonScale(width,height,diagram.getWidth(),diagram.getHeight());
//				width = (int) (diagram.getWidth() * scale);
//				height = (int) (diagram.getHeight() * scale);
//
//				b = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
//				Graphics2D g2 = b.createGraphics();
//				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
//				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//				g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//
//				// scale and center
//				double offX = (width-diagram.getWidth()*scale)/2;
//				double offY = (height-diagram.getHeight()*scale)/2;
//
//				AffineTransform transform = new AffineTransform(scale,0,0,scale,offX,offY);
//				g2.setTransform(transform);
//				diagram.render(g2);
//			} else {
				URL url = ClassLoader.getSystemResource( path );
				if( url != null ) {
					b = ImageIO.read(url);
					double scale = computeButtonScale(width, height, b.getWidth(), b.getHeight());
					if (scale != 1.0) {
						width = (int) (b.getWidth() * scale);
						height = (int) (b.getHeight() * scale);

						BufferedImage a = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2 = a.createGraphics();
						g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						g2.drawImage(b, 0, 0,width,height,null);
						b = a;
					}
				} else {
					JButton button = new JButton(path);
					int bWidth = button.getPreferredSize().width;
					int bHeight = button.getPreferredSize().height;

					double scale = computeButtonScale(width, height, bWidth, bHeight);

					button.setPreferredSize(new Dimension((int)(scale*bWidth),(int)(scale*bHeight)));
					return button;
				}
//			}
			JButton button = new JButton(new ImageIcon (b));

			if( !opaque ) {
				button.setOpaque(false);
				button.setBackground(new Color(0,0,0,0));
				button.setBorder(new EmptyBorder(0,0,0,0));
			}

			button.setPreferredSize(new Dimension(width,height));
			return button;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Uses mime type to determine if it's an image or not
	 */
	public static boolean isImage( File file ){
		try {
			String mimeType = Files.probeContentType(file.toPath());
			if( mimeType == null )
				return false;
			return mimeType.startsWith("image");
		} catch (IOException e) {
			return false;
		}
	}

	private static double computeButtonScale( int width , int height,
											  double imageWidth , double imageHeight ) {
		double scale = 1;
		if( width > 0 || height > 0 ) {
			if( width <= 0 ) {
				scale = height/ imageHeight;
			} else if( height <= 0 ) {
				scale = width/ imageWidth;
			} else {
				scale = Math.min(width/ imageWidth,height/ imageHeight);
			}
		}
		return scale;
	}

	public static JButton button( String name , ActionListener action ) {
		JButton b = new JButton(name);
		b.addActionListener(action);
		return b;
	}

	public static JCheckBox checkbox( String name , boolean checked , ActionListener action ) {
		JCheckBox b = new JCheckBox(name);
		if( action != null )
			b.addActionListener(action);
		b.setSelected(checked);
		return b;
	}

	public static File directoryUserHome() {
		String home = System.getProperty("user.home");
		if( home == null )
			home = "";
		return new File(home);
	}

	public enum FileTypes
	{
		IMAGES,VIDEOS,DIRECTORIES
	}
}

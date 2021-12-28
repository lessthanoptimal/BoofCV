/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofVerbose;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.cloud.PointCloudReader;
import boofcv.core.image.ConvertImage;
import boofcv.gui.dialogs.FilePreviewChooser;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.dialogs.OpenStereoSequencesChooser;
import boofcv.gui.settings.GlobalDemoSettings;
import boofcv.io.image.ConvertImageMisc;
import boofcv.io.image.UtilImageIO;
import boofcv.io.points.PointCloudIO;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.visualize.PointCloudViewer;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;

public class BoofSwingUtil {
	public static final String KEY_RECENT_FILES = "RecentFiles";
	public static final String KEY_PREVIOUS_SELECTION = "PreviouslySelected";
	public static final String KEY_PREVIOUS_DIRECTORY = "PreviousDirectory";

	public static final double MIN_ZOOM = 0.01;
	public static final double MAX_ZOOM = 50;

	/**
	 * Standard initialization of Swing for applications
	 */
	public static void initializeSwing() {
		try {
			// In Mac OS X Display the menubar in the correct location
			System.setProperty("apple.laf.useScreenMenuBar", "true");

			// smoother font
			System.setProperty("apple.awt.textantialiasing", "true");
		} catch (Exception ignore) {
		}

		// If the default layout manager tabbed panes will get smaller and smaller since it has a border
		Insets insets = UIManager.getInsets("TabbedPane.contentBorderInsets");
		insets.bottom = 0; // because the sides are now mangled it looks better without the bottom border too
		insets.left = 0;
		insets.right = 0;
		UIManager.put("TabbedPane.contentBorderInsets", insets);

		GlobalDemoSettings.SETTINGS.changeTheme();
	}

	public static void setVerboseWithDemoSettings( VerbosePrint v ) {
		if (GlobalDemoSettings.SETTINGS.verboseRecursive)
			v.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		else
			v.setVerbose(null, null);
	}

	public static boolean isRightClick( MouseEvent e ) {
		return (e.getButton() == MouseEvent.BUTTON3 ||
				(System.getProperty("os.name").contains("Mac OS X") &&
						(e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
						(e.getModifiers() & InputEvent.CTRL_MASK) != 0));
	}

	public static boolean isMiddleMouseButton( MouseEvent e ) {
		boolean clicked = SwingUtilities.isMiddleMouseButton(e);

		// This is for Mac OS X. Checks to see if control-command are held down with the mouse press
		clicked |= e.isControlDown() && ((e.getModifiersEx() & 256) != 0);

		return clicked;
	}

	public static double mouseWheelImageZoom( double scale, MouseWheelEvent e ) {
		if (e.getWheelRotation() > 0) {
			scale *= e.getWheelRotation()*1.1;
		} else if (e.getWheelRotation() < 0) {
			scale /= -e.getWheelRotation()*1.1;
		}
		return scale;
	}

	public static void recursiveEnable( JComponent panel, Boolean isEnabled ) {
		panel.setEnabled(isEnabled);

		Component[] components = panel.getComponents();

		for (Component component : components) {
			if (component instanceof JComponent) {
				recursiveEnable((JComponent)component, isEnabled);
			}
			component.setEnabled(isEnabled);
		}
	}

	public static File ensureSuffix( File f, String suffix ) {
		String name = f.getName();
		if (!name.toLowerCase().endsWith(suffix)) {
			name = FilenameUtils.getBaseName(name) + suffix;
			f = new File(f.getParent(), name);
		}
		return f;
	}

	public static @Nullable File saveFileChooser( Component parent, FileTypes... filters ) {
		return fileChooser(null, parent, false, new File(".").getPath(), null, filters);
	}

	public static @Nullable String[] openImageSetChooser( @Nullable Window parent,
														  OpenImageSetDialog.Mode mode, int numberOfImages ) {
		Preferences prefs;
		if (parent == null) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		}
		File defaultPath = BoofSwingUtil.directoryUserHome();
		String previousPath = prefs.get(KEY_PREVIOUS_SELECTION, defaultPath.getPath());

		String[] response = OpenImageSetDialog.showDialog(new File(previousPath), mode, numberOfImages, parent);

		if (response != null) {
			prefs.put(KEY_PREVIOUS_SELECTION, new File(response[0]).getParent());
		}
		return response;
	}

	public static @Nullable OpenStereoSequencesChooser.Selected openStereoChooser(
			Window parent, @Nullable Class<?> owner, boolean isSequence, boolean justImages ) {
		Preferences prefs;
		if (owner == null) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(owner.getSimpleName());
		}
		File defaultPath = BoofSwingUtil.directoryUserHome();
		String previousPath = prefs.get(KEY_PREVIOUS_SELECTION, defaultPath.getPath());

		OpenStereoSequencesChooser.Selected response =
				OpenStereoSequencesChooser.showDialog(parent, isSequence, justImages, new File(previousPath));

		if (response != null) {
			prefs.put(KEY_PREVIOUS_SELECTION, response.left.getParent());
		}
		return response;
	}

	public static String getDefaultPath( @Nullable Object parent, @Nullable String key ) {
		File defaultPath = BoofSwingUtil.directoryUserHome();
		if (key == null)
			return defaultPath.getPath();

		Preferences prefs;
		if (parent == null) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		}
		return prefs.get(key, defaultPath.getPath());
	}

	public static void saveDefaultPath( @Nullable Object parent, @Nullable String key, File file ) {
		if (key == null)
			return;
		Preferences prefs;
		if (parent == null) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		}
		if (!file.isDirectory())
			file = file.getParentFile();
		prefs.put(key, file.getAbsolutePath());
	}

	/**
	 * Opens a file choose when there is no parent component. Instead a string can be passed in so that the
	 * preference is specific to the application still
	 */
	public static @Nullable File openFileChooser( @Nullable String preferenceName, FileTypes... filters ) {
		return fileChooser(preferenceName, null, true, new File(".").getPath(), null, filters);
	}

	public static @Nullable File openFilePreview( @Nullable String preferenceName, FileTypes... filters ) {
		return fileChooserPreview(preferenceName, null, true, new File(".").getPath(), filters);
	}

	public static @Nullable File openFileChooser( @Nullable Component parent, FileTypes... filters ) {
		return openFileChooser(parent, new File(".").getPath(), filters);
	}

	public static @Nullable File openFilePreview( @Nullable Component parent, FileTypes... filters ) {
		return fileChooserPreview(null, parent, true, new File(".").getPath(), filters);
	}

	public static @Nullable File openFileChooser( @Nullable Component parent, String defaultPath, FileTypes... filters ) {

		// For now don't use the preview chooser if there are directories
		boolean directories = false;
		// Make sure it will try to load images
		boolean images = false;
		for (var filter : filters) {
			switch (filter) {
				case DIRECTORIES -> directories = true;
				case IMAGES, VIDEOS -> images = true;
				default -> {
				}
			}
		}
		if (!directories && images) {
			// Default to the new preview if they only want to see images
			return fileChooserPreview(null, parent, true, defaultPath, filters);
		} else {
			return fileChooser(null, parent, true, defaultPath, null, filters);
		}
	}

	/**
	 * Opens a file chooser for the specified file types. Previous path is loaded using preferences.
	 *
	 * @param massageName A lambda that lets you change the name of the previous path. Useful when a file type is selected.
	 */
	public static @Nullable File fileChooser( @Nullable String preferenceName,
											  @Nullable Component parent,
											  boolean openFile,
											  String defaultPath,
											  @Nullable BoofLambdas.MassageString massageName,
											  FileTypes... filters ) {

		if (preferenceName == null && parent != null) {
			preferenceName = parent.getClass().getSimpleName();
		}

		Preferences prefs;
		if (preferenceName == null) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(preferenceName);
		}
		File previousPath = new File(prefs.get(KEY_PREVIOUS_SELECTION, defaultPath));
		if (massageName != null) {
			previousPath = new File(massageName.process(previousPath.getPath()));
		}
		var chooser = new JFileChooser();

		boolean selectDirectories = false;
		for (FileTypes t : filters) {
			javax.swing.filechooser.FileFilter ff;
			ff = switch (t) {
				case FILES -> new javax.swing.filechooser.FileFilter() {
					@Override public boolean accept( File pathname ) {return true;}

					@Override public String getDescription() {return "All";}
				};
				case YAML -> new FileNameExtensionFilter("yaml", "yaml", "yml");
				case XML -> new FileNameExtensionFilter("xml", "xml");
				case IMAGES -> new FileNameExtensionFilter("Images", ImageIO.getReaderFileSuffixes());
				case VIDEOS -> new FileNameExtensionFilter("Videos", "mpg", "mp4", "mov", "avi", "wmv");
				case DIRECTORIES -> {
					selectDirectories = true;
					yield null;
				}
				default -> throw new RuntimeException("Unknown file type");
			};
			if (ff == null)
				break;
			chooser.addChoosableFileFilter(ff);
		}

		if (selectDirectories) {
			if (filters.length == 1)
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			else
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		}

		if (chooser.getChoosableFileFilters().length > 1) {
			chooser.setFileFilter(chooser.getChoosableFileFilters()[1]);
		}

		// Point it at the directory containing the previous selection
		File currentDirectory = previousPath.getParentFile();
		if (currentDirectory != null && currentDirectory.exists()) {
			chooser.setCurrentDirectory(currentDirectory);
			chooser.setSelectedFile(previousPath);
		} else {
			// otherwise, set the name to the previous file/directory name
			chooser.setSelectedFile(new File(previousPath.getName()));
		}

		File selected = null;
		int returnVal = openFile ? chooser.showOpenDialog(parent) : chooser.showSaveDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selected = chooser.getSelectedFile();
			prefs.put(KEY_PREVIOUS_SELECTION, selected.getPath());
		}
		return selected;
	}

	/**
	 * File chooser with a preview. Work in progress for replacing the old file chooser
	 */
	public static @Nullable File fileChooserPreview( @Nullable String preferenceName,
													 @Nullable Component parent,
													 boolean openFile,
													 String defaultPath, FileTypes... filters ) {

		if (preferenceName == null && parent != null) {
			preferenceName = parent.getClass().getSimpleName();
		}

		Preferences prefs;
		if (preferenceName == null) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(preferenceName);
		}
		File previousPath = new File(prefs.get(KEY_PREVIOUS_SELECTION, defaultPath)).getAbsoluteFile();
		FilePreviewChooser chooser = new FilePreviewChooser(openFile);

		escape:
		for (FileTypes t : filters) {
			javax.swing.filechooser.FileFilter ff;
			switch (t) {
				case FILES:
					ff = new javax.swing.filechooser.FileFilter() {
						@Override
						public boolean accept( File pathname ) {
							return true;
						}

						@Override
						public String getDescription() {
							return "All";
						}
					};
					break;

				case YAML:
					ff = new FileNameExtensionFilter("yaml", "yaml", "yml");
					break;

				case XML:
					ff = new FileNameExtensionFilter("xml", "xml");
					break;
				case IMAGES:
					ff = new FileNameExtensionFilter("Images", UtilImageIO.IMAGE_SUFFIXES);
					break;
				case VIDEOS:
					ff = new FileNameExtensionFilter("Videos", "mpg", "mp4", "mov", "avi", "wmv");
					break;

				case DIRECTORIES:
					break escape;
				default:
					throw new RuntimeException("Unknown file type");
			}
			chooser.getBrowser().addFileFilter(ff);
		}
		chooser.getBrowser().setSelectedFile(previousPath);

//		if( selectDirectories ) {
//			if( filters.length == 1 )
//				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//			else
//				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
//		}
//		if( chooser.getChoosableFileFilters().length > 1 ) {
//			chooser.setFileFilter(chooser.getChoosableFileFilters()[1]);
//		}

		File selected = chooser.showDialog(parent);
		System.out.println("Chooser file " + selected);
		if (selected != null) {
			prefs.put(KEY_PREVIOUS_SELECTION, selected.getPath());
		}
		return selected;
	}

	public static java.util.List<RecentFiles> getListOfRecentFiles( Component parent ) {
		return getListOfRecentFiles(parent.getClass().getSimpleName());
	}

	public static java.util.List<RecentFiles> getListOfRecentFiles( String preferenceName ) {

		Preferences prefs = Preferences.userRoot().node(preferenceName);
		String encodedString = prefs.get(KEY_RECENT_FILES, "");
		// See if recent file list exists, if not just return an empty list
		if (encodedString.length() == 0) {
			return new java.util.ArrayList<>();
		}
		try {
			java.util.List<RecentFiles> results = new ArrayList<>();
			java.util.List<Map<String, Object>> decoded = new Yaml().load(encodedString);
			for (var d : decoded) {
				RecentFiles r = new RecentFiles();
				r.name = (String)d.getOrDefault("name", "DefaultName");
				r.files = (java.util.List<String>)Objects.requireNonNull(d.get("files"));
				results.add(r);
			}
			return results;
		} catch (RuntimeException e) {
			e.printStackTrace();
			return new java.util.ArrayList<>();
		}
	}

	public static void addToRecentFiles( Component parent, String name, List<String> filePaths ) {
		addToRecentFiles(parent.getClass().getSimpleName(), name, filePaths);
	}

	public static void addToRecentFiles( String preferenceName, String name, List<String> filePaths ) {
		java.util.List<RecentFiles> files = getListOfRecentFiles(preferenceName);

		// Remove it from the list if it's already there
		for (int i = 0; i < files.size(); i++) {
			boolean matched = true;
			RecentFiles list = files.get(i);
			if (list.files.size() != filePaths.size())
				matched = false;
			for (int j = 0; j < list.files.size() && matched; j++) {
				if (!list.files.get(j).equals(filePaths.get(j)))
					matched = false;
			}
			if (matched) {
				files.remove(i);
				break;
			}
		}

		if (files.size() >= 10) {
			files.remove(9);
		}
		RecentFiles r = new RecentFiles();
		r.name = name;
		r.files = filePaths;
		files.add(0, r);

		saveRecentFiles(preferenceName, files);
	}

	public static void saveRecentFiles( String preferenceName, List<RecentFiles> files ) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		String encoded = yaml.dump(encodeForYaml(files));
		Preferences prefs = Preferences.userRoot().node(preferenceName);
		prefs.put(KEY_RECENT_FILES, encoded);
	}

	public static void updateRecentItems( JComponent owner, @Nullable JMenu menuRecent,
										  BoofLambdas.ProcessObject<BoofSwingUtil.RecentFiles> function ) {
		if (menuRecent == null)
			return;
		menuRecent.removeAll();
		List<BoofSwingUtil.RecentFiles> recentFiles = BoofSwingUtil.getListOfRecentFiles(owner);

		for (BoofSwingUtil.RecentFiles info : recentFiles) {
			JMenuItem recentItem = new JMenuItem(info.name);
			recentItem.addActionListener(e -> function.process(info));
			menuRecent.add(recentItem);
		}

		// don't add clear option if there is nothing to clear
		if (recentFiles.size() == 0)
			return;

		// Add the option to clear the list of recent files
		JMenuItem clearItem = new JMenuItem("Clear Recent");
		clearItem.addActionListener(e -> {
			menuRecent.removeAll();
			BoofSwingUtil.saveRecentFiles(owner.getClass().getSimpleName(), new ArrayList<>());
		});
		menuRecent.addSeparator();
		menuRecent.add(clearItem);
	}

	private static java.util.List<Map<String, Object>> encodeForYaml( java.util.List<RecentFiles> list ) {
		java.util.List<Map<String, Object>> output = new ArrayList<>();

		for (RecentFiles r : list) {
			Map<String, Object> m = new HashMap<>();
			m.put("name", r.name);
			m.put("files", r.files);
			output.add(m);
		}

		return output;
	}

	public static void invokeNowOrLater( Runnable r ) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	public static void checkGuiThread() {
		if (!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("Must be run in UI thread");
	}

	public static void checkNotGuiThread() {
		if (SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("Must NOT be run in UI thread");
	}

	/**
	 * Select a zoom which will allow the entire image to be shown in the panel
	 */
	public static double selectZoomToShowAll( JComponent panel, int width, int height ) {
		int w = panel.getWidth();
		int h = panel.getHeight();
		if (w == 0) {
			w = panel.getPreferredSize().width;
			h = panel.getPreferredSize().height;
		}

		double scale = Math.max(width/(double)w, height/(double)h);
		if (scale > 1.0) {
			return 1.0/scale;
		} else {
			return 1.0;
		}
	}

	/**
	 * Figures out what the scale should be to fit the window inside the default display
	 */
	public static double selectZoomToFitInDisplay( int width, int height ) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double w = screenSize.getWidth();
		double h = screenSize.getHeight();

		double scale = Math.max(width/w, height/h);
		if (scale > 1.0) {
			return 1.0/scale;
		} else {
			return 1.0;
		}
	}

	public static JPanel gridPanel( int cols, Component... children ) {
		return gridPanel(0, 2, 0, 0, children);
	}

	public static JPanel gridPanel( int rows, int cols, int hgap, int vgap, Component... children ) {
		JPanel panel = new JPanel(new GridLayout(rows, cols, hgap, vgap));
		for (int i = 0; i < children.length; i++) {
			panel.add(children[i]);
		}
		panel.setMaximumSize(panel.getPreferredSize());
		return panel;
	}

	public static JFormattedTextField createTextField( int current, int min, int max ) {
		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Integer.class);
		if (Integer.MIN_VALUE != min)
			formatter.setMinimum(min);
		if (Integer.MAX_VALUE != max)
			formatter.setMaximum(max);
		formatter.setAllowsInvalid(true);
//		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField field = new JFormattedTextField(formatter);
		field.setHorizontalAlignment(JTextField.RIGHT);
		field.setValue(current);
		return field;
	}

	public static JFormattedTextField createTextField( double current, double min, double max ) {
		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Double.class);
		if (!Double.isNaN(min))
			formatter.setMinimum(min);
		if (!Double.isNaN(max))
			formatter.setMaximum(max);
		formatter.setAllowsInvalid(true);
//		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField field = new JFormattedTextField(formatter);
		field.setHorizontalAlignment(JTextField.RIGHT);
		field.setValue(current);
		return field;
	}

	public static JFormattedTextField createHexTextField( long current ) {
		HexFormatter formatter = new HexFormatter();
		formatter.setValueClass(Long.class);
		formatter.setAllowsInvalid(true);
//		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField field = new JFormattedTextField(formatter);
		field.setHorizontalAlignment(JTextField.RIGHT);
		field.setValue(current);
		return field;
	}

	/**
	 * Wraps the component in the center of a BorderLayout
	 */
	public static JComponent wrapBorder( JComponent comp ) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(comp, BorderLayout.CENTER);
		return panel;
	}

	private static class HexFormatter extends DefaultFormatter {
		@Override
		public Object stringToValue( String text ) throws ParseException {
			if (text == null)
				return 0L;
			try {
				if (text.startsWith("0x")) {
					text = text.substring(2);
				}
				return Long.valueOf(text, 16);
			} catch (NumberFormatException nfe) {
				throw new ParseException(text, 0);
			}
		}

		@Override
		public String valueToString( Object value ) {
			if (value == null)
				return "";
			return "0x" + Long.toHexString((Long)value).toUpperCase();
		}
	}

	public static JMenuItem createMenuItem( String name, int mnmonic, int accelerator, BoofLambdas.ProcessCall action ) {
		JMenuItem item = new JMenuItem(name);
		BoofSwingUtil.setMenuItemKeys(item, mnmonic, accelerator);
		item.addActionListener(e -> action.process());
		return item;
	}

	public static JMenuItem createMenuItem( String name, BoofLambdas.ProcessCall action ) {
		JMenuItem item = new JMenuItem(name);
		item.addActionListener(e -> action.process());
		return item;
	}

	public static void setMenuItemKeys( JMenuItem menu, int mnemonic, int accelerator ) {
		menu.setMnemonic(mnemonic);
		menu.setAccelerator(KeyStroke.getKeyStroke(accelerator, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public static void warningDialog( Component component, Exception e ) {
		JOptionPane.showMessageDialog(component, e.getMessage());
	}

	/**
	 * Convenience function which sets rendering hints that the improve rendering quality of 2D graphics and will
	 * return the type casted Graphics2D. Potentially converting 3 lines of code into 1 line of code.
	 */
	public static Graphics2D antialiasing( Graphics g ) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return g2;
	}

	public static JButton createButtonIconGUI( String path, int width, int height ) {
		return createButtonIcon("boofcv/gui/" + path, width, height, true);
	}

	public static JButton createButtonIcon( String path, int width, int height, boolean opaque ) {
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
			URL url = ClassLoader.getSystemResource(path);
			if (url != null) {
				b = ImageIO.read(url);
				double scale = computeButtonScale(width, height, b.getWidth(), b.getHeight());
				if (scale != 1.0) {
					width = (int)(b.getWidth()*scale);
					height = (int)(b.getHeight()*scale);

					BufferedImage a = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = a.createGraphics();
					g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					g2.drawImage(b, 0, 0, width, height, null);
					b = a;
				}
			} else {
				JButton button = new JButton(path);
				int bWidth = button.getPreferredSize().width;
				int bHeight = button.getPreferredSize().height;

				double scale = computeButtonScale(width, height, bWidth, bHeight);

				button.setPreferredSize(new Dimension((int)(scale*bWidth), (int)(scale*bHeight)));
				return button;
			}
//			}
			JButton button = new JButton(new ImageIcon(b));

			if (!opaque) {
				button.setOpaque(false);
				button.setBackground(new Color(0, 0, 0, 0));
				button.setBorder(new EmptyBorder(0, 0, 0, 0));
			}

			button.setPreferredSize(new Dimension(width, height));
			return button;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static double computeButtonScale( int width, int height,
											  double imageWidth, double imageHeight ) {
		double scale = 1;
		if (width > 0 || height > 0) {
			if (width <= 0) {
				scale = height/imageHeight;
			} else if (height <= 0) {
				scale = width/imageWidth;
			} else {
				scale = Math.min(width/imageWidth, height/imageHeight);
			}
		}
		return scale;
	}

	public static JButton button( String name, @Nullable ActionListener action ) {
		var b = new JButton(name);
		b.addActionListener(action);
		return b;
	}

	public static JCheckBox checkbox( String name, boolean checked, @Nullable ActionListener action ) {
		var b = new JCheckBox(name);
		if (action != null)
			b.addActionListener(action);
		b.setSelected(checked);
		return b;
	}

	public static File directoryUserHome() {
		String home = System.getProperty("user.home");
		if (home == null)
			home = "";
		return new File(home);
	}

	/**
	 * Opens a dialog, asks the user where to save the point cloud, then saves the point cloud
	 */
	public static void savePointCloudDialog( @Nullable Component owner, @Nullable String key, PointCloudViewer pcv ) {
		String path = getDefaultPath(owner, key);

		var fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(path, "pointcloud.ply"));

		if (fileChooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			saveDefaultPath(owner, key, file);

			DogArray<Point3dRgbI_F64> cloud = pcv.copyCloud(null);
			String n = FilenameUtils.getBaseName(file.getName()) + ".ply";
			try {
				var f = new File(file.getParent(), n);
				var w = new FileOutputStream(f);
				PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64RGB(cloud.toList()), true, w);
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Opens a dialog, asks the user where to save the sequence of Se3
	 */
	public static void saveListSe3Dialog( @Nullable Component owner, @Nullable String key, PointCloudViewer pcv ) {
		String path = getDefaultPath(owner, key);

		var fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(path, "pointcloud.ply"));

		if (fileChooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			saveDefaultPath(owner, key, file);

			DogArray<Point3dRgbI_F64> cloud = pcv.copyCloud(null);
			String n = FilenameUtils.getBaseName(file.getName()) + ".ply";
			try {
				var f = new File(file.getParent(), n);
				var w = new FileOutputStream(f);
				PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64RGB(cloud.toList()), true, w);
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Opens a dialog, asks the user where to save the disparity image, converts the image into a U16 format,
	 * saves it as a PNG
	 */
	public static void saveDisparityDialog( @Nullable JComponent owner, @Nullable String key, ImageGray d ) {

		String path = getDefaultPath(owner, key);
		var fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(path, "disparity.png"));
		fileChooser.setDialogTitle("Save Disparity Image");
		if (fileChooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			saveDefaultPath(owner, key, file);

			// Convert disparity into a U16 image format
			GrayF32 disparity;
			if (d instanceof GrayF32) {
				disparity = (GrayF32)d;
			} else {
				disparity = new GrayF32(d.width, d.height);
				ConvertImage.convert((GrayU8)d, disparity);
			}
			var output = new GrayU16(disparity.width, disparity.height);
			ConvertImageMisc.convert_F32_U16(disparity, 8, output);

			// save as 16-bit png

			file = ensureSuffix(file, ".png");
			UtilImageIO.saveImage(output, file.getAbsolutePath());
		}
	}

	/**
	 * Renders camera views as squares from a {@link SceneStructureMetric}
	 */
	public static void visualizeCameras( SceneStructureMetric structure, PointCloudViewer viewer ) {
		var vertexes = new DogArray<>(Point3D_F64::new);
		var world_to_view = new Se3_F64();
		var view_to_world = new Se3_F64();
		var tmpSE3 = new Se3_F64();
		double r = 0.1;
		structure.views.forEach(v -> {
			structure.getWorldToView(v, world_to_view, tmpSE3).invert(view_to_world);

			// Represent the camera with a box
			vertexes.reset();
			vertexes.grow().setTo(-r, -r, 0);
			vertexes.grow().setTo(r, -r, 0);
			vertexes.grow().setTo(r, r, 0);
			vertexes.grow().setTo(-r, r, 0);

			for (int j = 0; j < vertexes.size; j++) {
				var p = vertexes.get(j);
				SePointOps_F64.transform(view_to_world, p, p);
			}
			viewer.addWireFrame(vertexes.toList(), true, 0xFF0000, 1);

			// Line indicating pointing direction
			vertexes.reset();
			vertexes.grow().setTo(0, 0, 0);
			vertexes.grow().setTo(0, 0, r);
			for (int j = 0; j < vertexes.size; j++) {
				var p = vertexes.get(j);
				SePointOps_F64.transform(view_to_world, p, p);
			}
			viewer.addWireFrame(vertexes.toList(), false, 0x0000FF, 1);
		});
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class RecentFiles {
		public String name;
		public java.util.List<String> files;
	}

	public enum FileTypes {FILES, YAML, XML, IMAGES, VIDEOS, DIRECTORIES}
}

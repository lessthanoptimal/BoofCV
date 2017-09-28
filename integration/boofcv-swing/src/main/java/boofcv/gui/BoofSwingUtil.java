/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class BoofSwingUtil {
	private static final String KEY_RECENT_FILES = "RecentFiles";

	public static final double MIN_ZOOM = 0.01;
	public static final double MAX_ZOOM = 50;

	public static File openFileChooseDialog(Component parent) {
		return openFileChooseDialog(parent,new File(".").getPath());
	}

	public static File openFileChooseDialog(Component parent, String defaultPath ) {
		String key = "PreviouslySelected";

		Preferences prefs;
		if( parent == null ) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(parent.getClass().getSimpleName());
		}
		String previousPath=prefs.get(key, defaultPath);
		JFileChooser chooser = new JFileChooser(previousPath);

		File selected = null;
		int returnVal = chooser.showOpenDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selected = chooser.getSelectedFile();
			prefs.put(key, selected.getParent());
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
}

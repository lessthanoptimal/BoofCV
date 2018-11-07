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

package boofcv.gui.image;

import boofcv.alg.misc.GImageStatistics;
import boofcv.gui.BoofSwingUtil;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays images in a new window.
 *
 * @author Peter Abeles
 */
public class ShowImages {

	/**
	 * Creates a dialog window showing the specified image.  The function will not
	 * exit until the user clicks ok
	 */
	public static void showDialog(BufferedImage img) {
		ImageIcon icon = new ImageIcon();
		icon.setImage(img);
		JOptionPane.showMessageDialog(null, icon);
	}

	/**
	 * Shows a set of images in a grid pattern.
	 *
	 * @param numColumns How many columns are in the grid
	 * @param title Number of the window
	 * @param images List of images to show
	 * @return Display panel
	 */
	public static ImageGridPanel showGrid( int numColumns , String title , BufferedImage ...images ) {
		JFrame frame = new JFrame(title);

		int numRows = images.length/numColumns + images.length%numColumns;
		ImageGridPanel panel = new ImageGridPanel(numRows,numColumns,images);

		frame.add(panel, BorderLayout.CENTER);

		frame.pack();
		frame.setVisible(true);

		return panel;
	}

	/**
	 * Creates a window showing the specified image.
	 */
	public static ImagePanel showWindow(BufferedImage img, String title) {
		return showWindow(img,title,false);
	}

	/**
	 * Creates a window showing the specified image.
	 */
	public static ImagePanel showWindow(BufferedImage img, String title, boolean closeOnExit ) {
		JFrame frame = new JFrame(title);

		ImagePanel panel = new ImagePanel(img);
		panel.setScaling(ScaleOptions.DOWN);

		// If the window will be too large to be displayed on the monitor set the bounds to something that can be
		// shown. The default behavior will just change one axis leaving it to have an awkward appearance
		Rectangle monitorBounds = frame.getGraphicsConfiguration().getBounds();
		if( monitorBounds.width < img.getWidth() || monitorBounds.height < img.getHeight()) {
			double scale = Math.min(monitorBounds.width/(double)img.getWidth(),monitorBounds.height/(double)img.getHeight());
			int width = (int)(scale*img.getWidth());
			int height = (int)(scale*img.getHeight());
			panel.setPreferredSize(new Dimension(width,height));
		}

		frame.add(panel, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		if( closeOnExit )
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		return panel;
	}

	public static ImagePanel showWindow( ImageBase img, String title ) {
		BufferedImage buff = ConvertBufferedImage.convertTo(img,null,true);
		return showWindow(buff,title);
	}

	public static JFrame showWindow( final JComponent component , String title ) {
		return showWindow(component,title,false);
	}

	public static JFrame showWindow( final JComponent component , String title, final boolean closeOnExit ) {
		final JFrame frame = new JFrame(title);
		frame.add(component, BorderLayout.CENTER);

		SwingUtilities.invokeLater(() -> {
			frame.pack();
			frame.setLocationRelativeTo(null); // centers window in the monitor
			frame.setVisible(true);
			if( closeOnExit )
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		});

		return frame;
	}

	/**
	 * Sets up the window but doesn't show it. Must be called in a GUI thread
	 */
	public static JFrame setupWindow( final JComponent component , String title, final boolean closeOnExit ) {
		BoofSwingUtil.checkGuiThread();

		final JFrame frame = new JFrame(title);
		frame.add(component, BorderLayout.CENTER);

		frame.pack();
		frame.setLocationRelativeTo(null); // centers window in the monitor
		if( closeOnExit )
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		return frame;
	}

	public static ImagePanel showWindow(ImageGray img , String title , boolean showMagnitude) {
		double max = GImageStatistics.maxAbs(img);
		BufferedImage buff;
		if( showMagnitude )
			buff = VisualizeImageData.grayMagnitude(img,null,max);
		else
			buff = VisualizeImageData.colorizeSign(img,null,max);

		return showWindow(buff,title);
	}
}

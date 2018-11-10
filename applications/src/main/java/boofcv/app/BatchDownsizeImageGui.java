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

package boofcv.app;

import boofcv.app.batch.BatchControlPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Peter Abeles
 */
public class BatchDownsizeImageGui extends JPanel implements BatchDownsizeImage.Listener {
	public static final String KEY_WIDTH = "width";
	public static final String KEY_HEIGHT = "height";

	Preferences prefs = Preferences.userRoot().node(BatchDownsizeImageGui.class.getSimpleName());

	BatchDownsizeImage downsizer = new BatchDownsizeImage();
	boolean processing = false;

	ControlPanel control = new ControlPanel();
	ImagePanel imagePanel = new ImagePanel();

	public BatchDownsizeImageGui() {
		super(new BorderLayout());

		downsizer.listener = this;

		imagePanel.setPreferredSize(new Dimension(400,400));
		imagePanel.setScaling(ScaleOptions.DOWN);

		add(control,BorderLayout.WEST);
		add(imagePanel,BorderLayout.CENTER);

		ShowImages.showWindow(this,"Batch Downsize Images", true);
	}

	@Override
	public void loadedImage(BufferedImage image, String name) {
		imagePanel.setImageRepaint(image);
	}

	@Override
	public void finishedConverting() {
		SwingUtilities.invokeLater(()->{
			control.bAction.setText("Start");
			processing = false;
		});
	}

	private class ControlPanel extends BatchControlPanel implements ChangeListener {
		JSpinner spinnerWidth;
		JSpinner spinnerHeight;

		public ControlPanel() {
			int width = Integer.parseInt(prefs.get(KEY_WIDTH,"640"));
			int height = Integer.parseInt(prefs.get(KEY_HEIGHT,"480"));

			if( width < 0 ) width = 640;
			if( height < 0 ) height = 480;

			spinnerWidth = spinner(width,0,10000,20);
			spinnerHeight = spinner(height,0,10000,20);

			addLabeled(spinnerWidth,"Width");
			addLabeled(spinnerHeight,"Height");

			addStandardControls(prefs);
		}

		@Override
		protected void handleStart() {
			if( processing ) {
				downsizer.cancel = true;
			} else {
				downsizer.width = ((Number)spinnerWidth.getValue()).intValue();
				downsizer.height = ((Number)spinnerHeight.getValue()).intValue();
				downsizer.pathInput = textInputDirectory.getText();
				downsizer.pathOutput = textOutputDirectory.getText();

				downsizer.regex = textRegex.getText();
				downsizer.rename = checkRename.isSelected();
				downsizer.recursive = checkRecursive.isSelected();

				if( downsizer.width == 0 && downsizer.height == 0 ) {
					JOptionPane.showMessageDialog(this, "Width and Height can't be zero");
					return;
				}

				try {
					Pattern.compile(downsizer.regex);
				} catch (PatternSyntaxException exception) {
					JOptionPane.showMessageDialog(this, "Invalid Regex");
					return;
				}

				prefs.put(KEY_INPUT,downsizer.pathInput);
				prefs.put(KEY_OUTPUT,downsizer.pathOutput);
				prefs.put(KEY_WIDTH,downsizer.width+"");
				prefs.put(KEY_HEIGHT,downsizer.height+"");

				bAction.setText("Cancel");
				processing = true;
				new Thread(()->downsizer.process()).start();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {

		}
	}

	public static void main(String[] args) {
		new BatchDownsizeImageGui();
	}
}

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

package boofcv.app;

import boofcv.alg.distort.AdjustmentType;
import boofcv.app.batch.BatchConvertControlPanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;

import static boofcv.app.batch.BatchConvertControlPanel.KEY_INPUT;
import static boofcv.app.batch.BatchConvertControlPanel.KEY_OUTPUT;

/**
 * GUI For batch removal of lens distortion
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BatchRemoveLensDistortionGui extends JPanel implements BatchRemoveLensDistortion.Listener {

	public static final String KEY_INTRINSIC = "intrinsic";

	ControlPanel control = new ControlPanel();
	ImagePanel imagePanel = new ImagePanel();

	boolean processing = false;
	BatchRemoveLensDistortion undistorter;

	public BatchRemoveLensDistortionGui() {
		super(new BorderLayout());

		imagePanel.setPreferredSize(new Dimension(400, 400));
		imagePanel.setScaling(ScaleOptions.DOWN);

		add(control, BorderLayout.WEST);
		add(imagePanel, BorderLayout.CENTER);

		ShowImages.showWindow(this, "Batch Remove Lens Distortion", true);
	}

	public boolean spawn( String pathIntrinsic, String pathInput, String pathOutput,
						  boolean rename, AdjustmentType adjustment ) {
		if (processing)
			return false;

		System.out.println("point A");

		BoofSwingUtil.checkGuiThread();

		if (!new File(pathIntrinsic).exists()) {
			JOptionPane.showMessageDialog(this, "Intrinsic path does not exist");
			return false;
		}

		try {
			CalibrationIO.load(pathIntrinsic);
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this, e);
			return false;
		}

		if (!new File(pathInput).exists()) {
			JOptionPane.showMessageDialog(this, "Input does not exist");
			return false;
		}
		if (pathOutput.length() == 0) {
			pathOutput = pathInput;
		} else if (!new File(pathOutput).exists()) {
			JOptionPane.showMessageDialog(this, "Output does not exist");
			return false;
		}


		Preferences prefs = Preferences.userRoot().node(getClass().getSimpleName());
		prefs.put(KEY_INTRINSIC, pathIntrinsic);
		prefs.put(KEY_INPUT, pathInput);
		prefs.put(KEY_OUTPUT, pathOutput);

		control.bAction.setText("Cancel");
		processing = true;
		System.out.println("point B");
		String _pathOutput = pathOutput;
		undistorter = new BatchRemoveLensDistortion(pathIntrinsic, pathInput, _pathOutput,
				rename, adjustment, this);
		new Thread(() -> {undistorter.process();}).start();

		return true;
	}

	@Override
	public void loadedImage( BufferedImage image, String name ) {
		imagePanel.setImageRepaint(image);
	}

	@Override
	public void finishedConverting() {
		SwingUtilities.invokeLater(() -> {
			control.bAction.setText("Start");
			processing = false;
		});
	}

	private class ControlPanel extends BatchConvertControlPanel {
		JTextField textIntrinsic = new JTextField();
		JComboBox<String> comboResize = new JComboBox<>(new String[]{"None", "Expand", "Full View"});

		public ControlPanel() {
			int textWidth = 200;
			int textHeight = 30;

			Preferences prefs = Preferences.userRoot().node(BatchRemoveLensDistortionGui.class.getSimpleName());

			textIntrinsic.setPreferredSize(new Dimension(textWidth, textHeight));
			textIntrinsic.setMaximumSize(textIntrinsic.getPreferredSize());
			comboResize.setMaximumSize(comboResize.getPreferredSize());

			textIntrinsic.setText(prefs.get(KEY_INTRINSIC, ""));

			addLabeled(createTextSelect(textIntrinsic, "Intrinsic", false), "Intrinsic");
			addLabeled(comboResize, "Adjust");

			addStandardControls(prefs);
		}

		@Override
		protected void handleStart() {
			if (processing) {
				undistorter.cancel = true;
			} else {
				System.out.println("Handle Start");
				String pathIntrinsic = textIntrinsic.getText();
				String pathInput = textInputSource.getText();
				String pathOutput = textOutputDirectory.getText();

				boolean rename = checkRename.isSelected();

				AdjustmentType adjustment = switch (comboResize.getSelectedIndex()) {
					case 0 -> AdjustmentType.NONE;
					case 1 -> AdjustmentType.FULL_VIEW;
					case 2 -> AdjustmentType.EXPAND;
					default -> throw new RuntimeException("Unknown");
				};

				System.out.println("before spawn");
				spawn(pathIntrinsic, pathInput, pathOutput, rename, adjustment);
			}
		}
	}

	public static void main( String[] args ) {
		new BatchRemoveLensDistortionGui();
	}
}

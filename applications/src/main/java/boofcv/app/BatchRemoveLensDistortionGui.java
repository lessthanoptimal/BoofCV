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

import boofcv.alg.distort.AdjustmentType;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.struct.calib.CameraModel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * GUI For batch removal of lens distortion
 *
 * @author Peter Abeles
 */
public class BatchRemoveLensDistortionGui extends JPanel implements BatchRemoveLensDistortion.Listener {

	public static final String KEY_INTRINSIC = "intrinsic";
	public static final String KEY_INPUT = "input";
	public static final String KEY_OUTPUT = "output";
	public static final String KEY_REGEX = "regex";


	ControlPanel control = new ControlPanel();
	ImagePanel imagePanel = new ImagePanel();

	boolean processing = false;
	BatchRemoveLensDistortion undistorter;

	public BatchRemoveLensDistortionGui() {
		super(new BorderLayout());

		imagePanel.setPreferredSize(new Dimension(400,400));
		imagePanel.setScaling(ScaleOptions.DOWN);

		add(control,BorderLayout.WEST);
		add(imagePanel,BorderLayout.CENTER);
	}

	public boolean spawn( String pathIntrinsic , String pathInput , String pathOutput ,
						  String regex , boolean rename , boolean recursive , AdjustmentType adjustment )
	{
		if( processing )
			return false;

		System.out.println("point A");

		BoofSwingUtil.checkGuiThread();

		if( !new File(pathIntrinsic).exists() ) {
			JOptionPane.showMessageDialog(this, "Intrinsic path does not exist");
			return false;
		}

		CameraModel model;
		try {
			model = CalibrationIO.load(pathIntrinsic);
		} catch( RuntimeException e ) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this,e);
			return false;
		}

		if( !new File(pathInput).exists() ) {
			JOptionPane.showMessageDialog(this, "Input does not exist");
			return false;
		}
		if( pathOutput.length() == 0 ) {
			pathOutput = pathInput;
		} else if( !new File(pathOutput).exists() ){
			JOptionPane.showMessageDialog(this, "Output does not exist");
			return false;
		}

		try {
			Pattern.compile(regex);
		} catch (PatternSyntaxException exception) {
			JOptionPane.showMessageDialog(this, "Invalid Regex");
			return false;
		}

		Preferences prefs = Preferences.userRoot().node(getClass().getSimpleName());
		prefs.put(KEY_INTRINSIC,pathIntrinsic);
		prefs.put(KEY_INPUT,pathInput);
		prefs.put(KEY_OUTPUT,pathOutput);
		prefs.put(KEY_REGEX,regex);

		control.bAction.setText("Cancel");
		processing = true;
		System.out.println("point B");
		String _pathOutput = pathOutput;
		undistorter = new BatchRemoveLensDistortion(pathIntrinsic,pathInput,_pathOutput,
				regex,rename,recursive,adjustment,this);
		new Thread(()->{undistorter.process();}).start();

		return true;
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

	private class ControlPanel extends StandardAlgConfigPanel {
		JTextField textIntrinsic = new JTextField();
		JTextField textInputDirectory = new JTextField();
		JTextField textOutputDirectory = new JTextField();
		JTextField textRegex = new JTextField();
		JCheckBox checkRecursive = new JCheckBox("Recursive");
		JCheckBox checkRename = new JCheckBox("Rename");
		JComboBox<String> comboResize = new JComboBox<>(new String[]{"None","Expand","Full View"});
		JButton bAction = new JButton("Start");

		public ControlPanel() {
			int textWidth = 200;
			int textHeight = 30;

			Preferences prefs = Preferences.userRoot().node(BatchRemoveLensDistortionGui.class.getSimpleName());

			textIntrinsic.setPreferredSize(new Dimension(textWidth,textHeight));
			textIntrinsic.setMaximumSize(textIntrinsic.getPreferredSize());
			textInputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
			textInputDirectory.setMaximumSize(textInputDirectory.getPreferredSize());
			textOutputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
			textOutputDirectory.setMaximumSize(textOutputDirectory.getPreferredSize());
			textRegex.setPreferredSize(new Dimension(textWidth+40,textHeight));
			textRegex.setMaximumSize(textRegex.getPreferredSize());
			textRegex.setText("([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)");
			checkRecursive.setSelected(false);
			comboResize.setMaximumSize(comboResize.getPreferredSize());

			textIntrinsic.setText(prefs.get(KEY_INTRINSIC,""));
			textInputDirectory.setText(prefs.get(KEY_INPUT,""));
			textOutputDirectory.setText(prefs.get(KEY_OUTPUT,""));

			bAction.addActionListener(a-> handleStart());

			addLabeled(createTextSelect(textIntrinsic,"Intrinsic",false),"Intrinsic");
			addLabeled(createTextSelect(textInputDirectory,"Input Directory",true),"Input");
			addLabeled(createTextSelect(textOutputDirectory,"Output Directory",true),"Output");
			addAlignLeft(checkRecursive);
			addAlignLeft(checkRename);
			addLabeled(textRegex,"Regex");
			addLabeled(comboResize,"Adjust");
			addVerticalGlue();
			addAlignCenter(bAction);
		}

		private JPanel createTextSelect( final JTextField field , final String message , boolean directory ) {
			JButton bOpen = new JButton("S");
			bOpen.addActionListener(a->{
				selectPath(field,message,directory);
			});

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
			panel.add(field);
			panel.add(bOpen);
			return panel;
		}

		private void selectPath(JTextField field , String message , boolean directory) {
			File current = new File(field.getText());
			if( !current.exists() )
				current = new File(".");

			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(current);
			chooser.setDialogTitle(message);
			if( directory )
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			else
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				field.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		}

		private void handleStart() {
			if( processing ) {
				undistorter.cancel = true;
			} else {
				System.out.println("Handle Start");
				String pathIntrinsic = textIntrinsic.getText();
				String pathInput = textInputDirectory.getText();
				String pathOutput = textOutputDirectory.getText();

				String regex = textRegex.getText();
				boolean rename = checkRename.isSelected();
				boolean recursive = checkRecursive.isSelected();

				AdjustmentType adjustment;
				switch (comboResize.getSelectedIndex()) {
					case 0:
						adjustment = AdjustmentType.NONE;
						break;
					case 1:
						adjustment = AdjustmentType.FULL_VIEW;
						break;
					case 2:
						adjustment = AdjustmentType.EXPAND;
						break;
					default:
						throw new RuntimeException("Unknown");
				}

				System.out.println("before spawn");
				spawn(pathIntrinsic, pathInput, pathOutput, regex, rename, recursive, adjustment);
			}
		}
	}

	public static void main(String[] args) {
		BatchRemoveLensDistortionGui app = new BatchRemoveLensDistortionGui();
		ShowImages.showWindow(app,"Batch Remove Lens Distortion", true);
	}
}

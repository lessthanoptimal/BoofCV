/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.dialogs;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofLambdas;
import boofcv.struct.calib.StereoParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;

import static boofcv.gui.BoofSwingUtil.KEY_PREVIOUS_SELECTION;

/**
 * Presents a file choose that lets the user select two sequences for left and right stereo camera as well as
 * the stereo calibration file.
 *
 * @author Peter Abeles
 */
public class OpenStereoSequencesChooser extends JSpringPanel {

	// TODO open Videos
	// TODO let the user specify sequences with regrex file names
	// TODO see if the two sequences have the same number of images
	// TODO truncate image size for memory

	private static final int PREVIEW_PIXELS = 300;

	protected Listener listener;

	// Path to left and right input
	JTextField textLeftPath= createTextWidget();
	JTextField textRightPath= createTextWidget();
	JTextField textCalibrationPath= createTextWidget();

	ImagePanel previewLeft = new ImagePanel();
	ImagePanel previewRight = new ImagePanel();

	JButton bCancel = new JButton("Cancel");
	JButton bOK = new JButton("OK");

	// the directory it's i
	File directory;

	JDialog dialog;
	StereoParameters stereoParameters;

	public OpenStereoSequencesChooser(JDialog dialog, Listener listener,File directory) {
		this.listener = listener;
		this.directory = directory;

		JPanel previewPanel = new JPanel();
		previewPanel.setLayout(new BoxLayout(previewPanel,BoxLayout.X_AXIS));
		previewPanel.add(previewLeft);
		previewPanel.add(previewRight);

		int ip = PREVIEW_PIXELS+40;
		previewLeft.setPreferredSize(new Dimension(ip,ip));
		previewRight.setPreferredSize(new Dimension(ip,ip));

		configureButtons(dialog, listener);

		JPanel buttonPanel = StandardAlgConfigPanel.createHorizontalPanel(bCancel,Box.createHorizontalGlue(),bOK);

		JPanel leftPanel = createPathPanel("Left",textLeftPath,this::handleLeftPath);
		JPanel rightPanel = createPathPanel("Right",textRightPath,this::handleRightPath);
		JPanel calibPanel = createPathPanel("Calibration",textCalibrationPath,this::handleCalibrationPath);

		constrainWestNorthEast(leftPanel,null,6,6);
		constrainWestNorthEast(rightPanel,leftPanel,6,6);
		constrainWestNorthEast(calibPanel,rightPanel,6,6);
		constrainWestNorthEast(previewPanel,calibPanel,6,6);
		constrainWestSouthEast(buttonPanel,null,10,10);

		layout.putConstraint(SpringLayout.SOUTH, previewPanel, -5, SpringLayout.NORTH, buttonPanel);

		setPreferredSize(new Dimension(500,400));
	}

	private void configureButtons(JDialog dialog, Listener listener) {
		// Fix the size
		bCancel.setMaximumSize(bCancel.getPreferredSize());
		bOK.setMaximumSize(bOK.getPreferredSize());
		bCancel.setMinimumSize(bCancel.getPreferredSize());
		bOK.setMinimumSize(bOK.getPreferredSize());
		// disable initially since the user hasn't selected the inputs
		bOK.setEnabled(false);
		// Make OK the default button
		bOK.setDefaultCapable(true);
		SwingUtilities.getRootPane(dialog).setDefaultButton(bOK);

		// Specify how it should respond to the user selecting these buttons
		bCancel.addActionListener((e)->listener.userCanceled());
		bOK.addActionListener((e)->handleOK());
	}

	/**
	 * User hsa clicked OK and wishes to load this sequence
	 */
	private void handleOK() {
		File left = new File(textLeftPath.getText());
		File right = new File(textRightPath.getText());
		File calibration = new File(textCalibrationPath.getText());
		listener.selectedInputs(left,right,calibration);
	}

	/**
	 * User wishes to select a path for left sequence
	 */
	private void handleLeftPath() {
		handleSelectSequence(textLeftPath,previewLeft);
	}

	/**
	 * User wishes to select a path for right sequence
	 */
	private void handleRightPath() {
		handleSelectSequence(textRightPath,previewRight);
	}

	/**
	 * User wishes to select a path for calibration
	 */
	private void handleCalibrationPath() {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setCurrentDirectory(directory);
		int returnVal = fc.showOpenDialog(this);
		if( returnVal != JFileChooser.APPROVE_OPTION )
			return;
		File selected = fc.getSelectedFile();
		try {
			stereoParameters = CalibrationIO.load(selected);
			textCalibrationPath.setText(selected.getPath());
			directory = selected.getParentFile();
			checkEverythingSet();
		} catch( RuntimeException e ){
			BoofSwingUtil.warningDialog(this,e);
		}
	}

	/**
	 * Checks to see if the user has selected all three inputs
	 */
	private void checkEverythingSet() {
		boolean allGood = !textLeftPath.getText().isEmpty() &&
				!textRightPath.getText().isEmpty() && !textCalibrationPath.getText().isEmpty();
		bOK.setEnabled(allGood);
	}

	private void handleSelectSequence( JTextField textArea , ImagePanel previewPanel ) {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setCurrentDirectory(directory);
		int returnVal = fc.showOpenDialog(this);
		if( returnVal != JFileChooser.APPROVE_OPTION )
			return;

		File selected = fc.getSelectedFile();
		textArea.setText(selected.getPath());
		setPreview(selected,previewPanel);
		checkEverythingSet();
		// If it's a file you want the directory it's in.
		// If it's a directory containing images you want to open the parent directory
		directory = selected.getParentFile();
	}

	/**
	 * Sets a preview image for selected path. If directory it uses the first image.
	 */
	private void setPreview( File path , ImagePanel previewPanel ) {
		BufferedImage preview=null;
		if( path.isDirectory() ) {
			File f = findFirstImageInDirectory(path);
			if( f != null )
				preview = UtilImageIO.loadImage(f.getAbsolutePath());
		} else {
			preview = UtilImageIO.loadImage(path.getAbsolutePath());
		}
		previewPanel.setImageRepaint(preview);
	}

	/**
	 * Finds the first image in the sequence in the directory
	 */
	private File findFirstImageInDirectory( File directory ) {
		String first = null;
		File[] files = directory.listFiles();
		if( files == null )
			return null;
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if( f.isDirectory() )
				continue;
			if( !UtilImageIO.isImage(f))
				continue;
			if( first == null || first.compareTo(f.getName()) < 0 ) {
				first = f.getName();
			}
		}
		if( first != null )
			return new File(directory,first);
		else
			return null;
	}

	/**
	 * User has given up and wishes to do nothing
	 */
	void handleCancel() {
		listener.userCanceled();
	}

	private JPanel createPathPanel(String name, JTextField text , BoofLambdas.Process callback ) {
		JLabel label = new JLabel(name);
		label.setPreferredSize(new Dimension(100,30));
		JButton bOpen = new JButton();
		bOpen.setIcon(UIManager.getIcon("FileView.directoryIcon"));
		bOpen.setMaximumSize(bOpen.getPreferredSize());
		bOpen.addActionListener(e->callback.process());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.add(label);
		panel.add(text);
		panel.add(Box.createRigidArea(new Dimension(10,5)));
		panel.add(bOpen);

		return panel;
	}

	private static JTextField createTextWidget() {
		JTextField textArea = new JTextField();
		textArea.setEditable(false);
		return textArea;
	}

	/**
	 * Lets the listener know what the user has chosen to do.
	 */
	public interface Listener {
		void selectedInputs(File left, File right, File calibration);

		void userCanceled();
	}

	/**
	 * Output object. Contains everything the user has selected
	 */
	public static class Selected {
		public File left;
		public File right;
		public File calibration;
	}

	public static class DefaultListener implements Listener {
		JDialog dialog;
		public boolean canceled = false;
		public final Selected selected = new Selected();

		public DefaultListener(JDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public void selectedInputs(File left, File right, File calibration) {
			this.selected.left = left;
			this.selected.right = right;
			this.selected.calibration = calibration;
			this.dialog.setVisible(false);
		}

		@Override
		public void userCanceled() {
			canceled = true;
			this.dialog.setVisible(false);
		}
	}

	public static Selected showDialog(Window owner )
	{
		Preferences prefs;
		if( owner == null ) {
			prefs = Preferences.userRoot();
		} else {
			prefs = Preferences.userRoot().node(owner.getClass().getSimpleName());
		}
		File defaultPath = BoofSwingUtil.directoryUserHome();
		String previousPath=prefs.get(KEY_PREVIOUS_SELECTION, defaultPath.getPath());

		JDialog dialog = new JDialog(owner,"Open Stereo Sequence", Dialog.ModalityType.APPLICATION_MODAL);
		DefaultListener listener = new DefaultListener(dialog);
		OpenStereoSequencesChooser panel = new OpenStereoSequencesChooser(dialog,listener,new File(previousPath));

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				panel.handleCancel();
			}
		});
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		// should block at this point
		dialog.dispose();

		if( listener.canceled )
			return null;

		// save the path
		prefs.put(KEY_PREVIOUS_SELECTION, listener.selected.calibration.getParent());

		return listener.selected;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(()->{
			showDialog(null);
		});
	}
}

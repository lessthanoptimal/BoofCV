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

package boofcv.io.webcamcapture;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Opens a dialog and lets the user configure the camera and select which one
 *
 * @author Peter Abeles
 */
public class OpenWebcamDialog extends StandardAlgConfigPanel {

	JDialog dialog;

	JComboBox comboCameras;
	Webcam selectedCamera;

	JFormattedTextField fieldWidth,fieldHeight;
	int width,height;

	DefaultComboBoxModel modelSizes = new DefaultComboBoxModel();
	JComboBox comboSizes = new JComboBox(modelSizes);

	JCheckBox cSave = new JCheckBox("Save");

	JButton bCancel = new JButton("Cancel");
	JButton bOK = new JButton("OK");

	volatile boolean concluded = false;

	ActionListener cameraListener;
	ActionListener sizeListener;

	public OpenWebcamDialog( JDialog dialog ) {
		this.dialog = dialog;
		setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

		final List<Webcam> cameras = Webcam.getWebcams();
		Vector<String> names = new Vector<>();

		for( Webcam w : cameras ) {
			names.add( w.getName() );
		}

		bOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionOK();
			}
		});
		bCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionCancel();
			}
		});

		cSave.setSelected(true);

		fieldWidth = BoofSwingUtil.createTextField(0,0,20000);
		fieldHeight = BoofSwingUtil.createTextField(0,0,20000);

		fieldWidth.setPreferredSize(new Dimension(150,30));
		fieldHeight.setPreferredSize(new Dimension(150,30));
		fieldWidth.setMaximumSize(fieldWidth.getPreferredSize());
		fieldHeight.setMaximumSize(fieldHeight.getPreferredSize());

		fieldWidth.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				width = (Integer)fieldWidth.getValue();
			}
		});
		fieldHeight.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				height = (Integer)fieldHeight.getValue();
			}
		});

		comboSizes.setPreferredSize(new Dimension(200,30));
		comboSizes.setMaximumSize(comboSizes.getPreferredSize());

		comboCameras = new JComboBox(names);
		comboCameras.setPreferredSize(new Dimension(200,30));
		comboCameras.setMaximumSize(comboCameras.getPreferredSize());
		cameraListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Webcam w = cameras.get(comboCameras.getSelectedIndex());
				selectedCamera = w;
				Dimension s = w.getViewSize();
				setCameraSize(s.width,s.height);

				comboSizes.removeActionListener(sizeListener);
				modelSizes.removeAllElements();
				for( Dimension d : w.getViewSizes() ) {
					modelSizes.addElement(d.width+" x "+d.height);
				}
				comboSizes.addActionListener(sizeListener);
			}
		};
		comboCameras.addActionListener(cameraListener);

		sizeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Webcam w = cameras.get(comboCameras.getSelectedIndex());
				selectedCamera = w;
				String text = (String)comboSizes.getSelectedItem();
				if( text == null )
					return;
				String words[] = text.split(" x ");
				int width = Integer.parseInt(words[0]);
				int height = Integer.parseInt(words[1]);
				setCameraSize(width,height);
			}
		};
		comboSizes.addActionListener(sizeListener);

		final JPanel sizePanel = new JPanel();
		sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.X_AXIS));
		sizePanel.add( fieldWidth );
		sizePanel.add( Box.createHorizontalStrut(10));
		sizePanel.add( fieldHeight );
		sizePanel.setMaximumSize(sizePanel.getPreferredSize());

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add( bCancel );
		buttonPanel.add( Box.createHorizontalGlue());
		buttonPanel.add( bOK );
//		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		dialog.getRootPane().setDefaultButton(bOK);

		final JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.X_AXIS));
		checkPanel.add( cSave );
		checkPanel.add( Box.createHorizontalGlue());
//		checkPanel.setMaximumSize(checkPanel.getPreferredSize());

		addLabeled(comboCameras,"Webcam",this);
		addLabeled(sizePanel,"Size",this);
		addAlignRight(comboSizes,this);
		add(checkPanel);
		add(buttonPanel);

		comboCameras.setSelectedIndex(0);
		loadPreferences();
	}

	public boolean loadPreferences() {
		Preferences prefs = Preferences.userRoot().node(getClass().getSimpleName());

		String cameraName = prefs.get("camera","");
		final int width = prefs.getInt("width",-1);
		final int height = prefs.getInt("height",-1);

		if( cameraName.length() <= 0 )
			return false;

		List<Webcam> cameras = Webcam.getWebcams();
		int match = -1;
		for( int i = 0; i < cameras.size(); i++ ) {
			Webcam w = cameras.get(i);
			if( w.getName().equals(cameraName)) {
				match = i;
				break;
			}
		}

		if( match == -1 )
			return false;

		comboCameras.removeActionListener(cameraListener);
		comboCameras.setSelectedIndex(match);
		if( width > 0 && height > 0 ) {
			setCameraSize(width,height);
		}
		comboCameras.addActionListener(cameraListener);
		return true;
	}

	public void savePreferences() {
		if( selectedCamera == null )
			return;
		Preferences prefs = Preferences.userRoot().node(getClass().getSimpleName());

		prefs.put("camera",selectedCamera.getName());
		prefs.putInt("width",width);
		prefs.putInt("height",height);

		try {
			prefs.flush();
		} catch (BackingStoreException ignore) {}
	}

	private void setCameraSize( final int width , final int height ) {
//		BoofSwingUtil.checkGuiThread();
		this.width = width;
		this.height = height;
		fieldWidth.setValue(width);
		fieldHeight.setValue(height);

//		BoofSwingUtil.invokeNowOrLater(new Runnable() {
////		SwingUtilities.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				fieldWidth.setValue(width);
//				fieldHeight.setValue(height);
//			}
//		});
	}

	public static Selection showDialog( Window owner )
	{
		JDialog dialog = new JDialog(owner,"Select Webcam",Dialog.ModalityType.APPLICATION_MODAL);
		final OpenWebcamDialog panel = new OpenWebcamDialog(dialog);

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				panel.actionCancel();
			}
		});
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(panel,BorderLayout.CENTER);
		dialog.setSize(new Dimension(400,200));
//		dialog.pack();
		if( owner != null )
			dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		// should block at this point
		dialog.dispose();

		if( panel.selectedCamera != null ) {
			Selection s = new Selection();
			s.camera = panel.selectedCamera;
			s.width = panel.getSelectedWidth();
			s.height = panel.getSelectedHeight();
			return s;
		} else {
			return null;
		}
	}

	public void actionOK() {
		if( cSave.isSelected()) {
			savePreferences();
		}
		dialog.setVisible(false);
		concluded = true;
	}

	public void actionCancel() {
		selectedCamera = null;
		dialog.setVisible(false);
		concluded = true;
	}

	public Webcam getSelectedCamera() {
		return selectedCamera;
	}

	public int getSelectedWidth() {
		return width;
	}

	public int getSelectedHeight() {
		return height;
	}

	public static class Selection {
		public Webcam camera;
		public int width,height;
	}

	public static void main(String[] args) {
		Selection s = OpenWebcamDialog.showDialog(null);
		if( s != null )
			System.out.println("Selected camera. "+s.width+" "+s.height);
		else
			System.out.println("Didn't select camera");
		System.exit(0);
	}
}

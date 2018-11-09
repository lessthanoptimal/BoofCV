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

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.square.FiducialSquareGenerator;
import boofcv.app.markers.CreateSquareMarkerControlPanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import org.ddogleg.struct.GrowQueue_I64;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * GUI for printing square binary fiducials
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareBinaryGui extends JPanel implements CreateSquareMarkerControlPanel.Listener {

	ControlPanel controls = new ControlPanel(this);
	ImagePanel imagePanel = new ImagePanel();
	JFrame frame;

	FiducialImageEngine render = new FiducialImageEngine();
	FiducialSquareGenerator generator = new FiducialSquareGenerator(render);
	BufferedImage buffered;

	public CreateFiducialSquareBinaryGui() {
		super(new BorderLayout());

		render.configure(20,300);
		generator.setMarkerWidth(300);
		buffered = new BufferedImage(render.getGray().width,render.getGray().height,BufferedImage.TYPE_INT_RGB);

		imagePanel.setPreferredSize(new Dimension(400,400));
		imagePanel.setScaling(ScaleOptions.DOWN);
		imagePanel.setCentering(true);

		add(controls,BorderLayout.WEST);
		add(imagePanel,BorderLayout.CENTER);

		setPreferredSize(new Dimension(700,500));
		frame = ShowImages.setupWindow(this,"Fiducial Square Binary",true);
		createMenuBar();

		renderPreview();
		frame.setVisible(true);
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		JMenuItem menuSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuSave,KeyEvent.VK_S,KeyEvent.VK_S);
		menuSave.addActionListener(e -> saveFile(false));

		JMenuItem menuPrint = new JMenuItem("Print...");
		BoofSwingUtil.setMenuItemKeys(menuPrint,KeyEvent.VK_P,KeyEvent.VK_P);
		menuPrint.addActionListener(e -> saveFile(true));

		JMenuItem menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit,KeyEvent.VK_Q,KeyEvent.VK_Q);
		menuQuit.addActionListener(e -> System.exit(0));

		JMenuItem menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.addActionListener(e -> showHelp());

		menuFile.addSeparator();
		menuFile.add(menuSave);
		menuFile.add(menuPrint);
		menuFile.add(menuHelp);
		menuFile.add(menuQuit);
		menuBar.add(menuFile);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		JMenuItem menuCut = new JMenuItem(new DefaultEditorKit.CutAction());
		menuCut.setText("Cut");BoofSwingUtil.setMenuItemKeys(menuCut,KeyEvent.VK_T,KeyEvent.VK_X);
		JMenuItem menuCopy = new JMenuItem(new DefaultEditorKit.CopyAction());
		menuCopy.setText("Copy");BoofSwingUtil.setMenuItemKeys(menuCopy,KeyEvent.VK_C,KeyEvent.VK_C);
		JMenuItem menuPaste = new JMenuItem(new DefaultEditorKit.PasteAction());
		menuPaste.setText("Paste");BoofSwingUtil.setMenuItemKeys(menuPaste,KeyEvent.VK_P,KeyEvent.VK_V);

		editMenu.add(menuCut);
		editMenu.add(menuCopy);
		editMenu.add(menuPaste);
		menuBar.add(editMenu);

		frame.setJMenuBar(menuBar);
	}

	private void saveFile( boolean sendToPrinter ) {
		if( controls.patterns.size == 0 )
			return;
		CreateFiducialSquareBinary c = new CreateFiducialSquareBinary();
		c.sendToPrinter = sendToPrinter;
		c.unit = controls.documentUnits;
		c.paperSize = controls.paperSize;
		c.markerWidth = (float)controls.markerWidth;
		c.spaceBetween = c.markerWidth/4;
		c.gridWidth = controls.gridWidth;
		c.gridFill = controls.fillGrid;
		c.hideInfo = controls.hideInfo;
		c.numbers = new Long[controls.patterns.size];
		for (int i = 0; i < controls.patterns.size; i++) {
			c.numbers[i] = controls.patterns.get(i);
		}

		if( sendToPrinter ) {
			if (controls.format.compareToIgnoreCase("pdf") != 0) {
				JOptionPane.showMessageDialog(this, "Must select PDF document type to print");
				return;
			}
		} else {
			File f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f,"binary."+controls.format);

			f = BoofSwingUtil.fileChooser(this,false,f.getPath());
			if (f == null) {
				return;
			}
			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
			c.fileName = f.getAbsolutePath();
		}

		c.finishParsing();
		try {
			c.run();
		} catch (IOException e) {
			BoofSwingUtil.warningDialog(this,e);
		}
	}

	private void showHelp() {

	}

	private void renderPreview() {
		long pattern = controls.selectedPattern;
		if( pattern <= 0 ) {
			imagePanel.setImageRepaint(null);
		} else {
			generator.setBlackBorder(controls.borderFraction);
			generator.generate(controls.selectedPattern,controls.gridWidth);
			ConvertBufferedImage.convertTo(render.getGray(),buffered,true);
			imagePanel.setImageRepaint(buffered);
		}
	}

	@Override
	public void controlsUpdates() {
		renderPreview();
	}

	class ControlPanel extends CreateSquareMarkerControlPanel {

		DefaultListModel<Long> listModel = new DefaultListModel<>();
		JList<Long> listPatterns = new JList<>(listModel);
		GrowQueue_I64 patterns = new GrowQueue_I64();
		JSpinner spinnerGridWidth;

		long selectedPattern =-1;
		int gridWidth = 4;

		public ControlPanel(Listener listener) {
			super(listener);

			listPatterns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listPatterns.setLayoutOrientation(JList.VERTICAL);
//			listPatterns.setVisibleRowCount(-1);
			listPatterns.addListSelectionListener(e -> {
				int s = listPatterns.getSelectedIndex();
				if( s >= 0 ) {
					selectedPattern = patterns.get(s);
				} else {
					selectedPattern = -1;
				}
				renderPreview();
			});

			spinnerGridWidth = spinner(gridWidth,2,8,1,
					e->{
						gridWidth=((Number)spinnerGridWidth.getValue()).intValue();
						if( listener != null )
							listener.controlsUpdates();
					});

			add( new JScrollPane(listPatterns));
			addLabeled(spinnerGridWidth,"Grid Width");
			layoutComponents();
		}

		@Override
		public void handleAddPattern() {
			String text = JOptionPane.showInputDialog("Enter ID","1234");
			try {
				long lvalue = Long.parseLong(text);

				long maxValue = (long)(Math.pow(2,gridWidth*gridWidth)-4);
				if( lvalue > maxValue )
					lvalue = maxValue;
				else if( lvalue < 0 )
					lvalue = 0;

				listModel.add(listModel.size(), lvalue);
				patterns.add(lvalue);
				listPatterns.setSelectedIndex(listModel.size()-1);
			} catch( NumberFormatException e ) {
				JOptionPane.showMessageDialog(this,"Must be an integer!");
			}
		}

		@Override
		public void handleRemovePattern() {
			int selected = listPatterns.getSelectedIndex();
			if( selected >= 0 ) {
				listModel.removeElementAt(selected);
				patterns.remove(selected);
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(CreateFiducialSquareBinaryGui::new);
	}
}

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

package boofcv.app.qrcode;

import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.app.CreateQrCodeDocument;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Peter Abeles
 */
public class CreateQrCodeGui extends JPanel implements  CreateQrCodeControlPanel.Listener{

	CreateQrCodeControlPanel controls = new CreateQrCodeControlPanel(this);
	ImagePanel imagePanel = new ImagePanel();

	JFrame frame;

	public CreateQrCodeGui() {
		setLayout(new BorderLayout());

		imagePanel.setCentering(true);
		imagePanel.setScaling(ScaleOptions.DOWN);

		add(BorderLayout.WEST,controls);
		add(BorderLayout.CENTER,imagePanel);
		createMenuBar();

		setPreferredSize(new Dimension(700,500));

		frame = ShowImages.showWindow(this,"QR Code Document Creator",true);

		// Render the QR Code
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				renderPreview();
			}
		});
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		JMenuItem menuSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuSave,KeyEvent.VK_S,KeyEvent.VK_S);
		menuSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveFile(false);
			}
		});

		JMenuItem menuPrint = new JMenuItem("Print...");
		BoofSwingUtil.setMenuItemKeys(menuPrint,KeyEvent.VK_P,KeyEvent.VK_P);
		menuPrint.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveFile(true);
			}
		});

		JMenuItem menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit,KeyEvent.VK_Q,KeyEvent.VK_Q);
		menuQuit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		JMenuItem menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showHelp();
			}
		});

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

		add(BorderLayout.NORTH,menuBar);
	}

	private void showHelp() {
		JOptionPane.showMessageDialog(this,"Many more options and better documentation available through commandline");
	}

	private void saveFile( boolean sendToPrinter ) {
		// grab the focus and force what the user is editing to be saved

		File f;

		// see where the document is to be sent
		if( sendToPrinter ) {
			if (controls.format.compareToIgnoreCase("pdf") != 0) {
				JOptionPane.showMessageDialog(this, "Must select PDF document type to print");
				return;
			}
			f = new File(""); // dummy to make the code below happy and less complex
		} else {
			f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f,"qrcode."+controls.format);

			f = BoofSwingUtil.fileChooser(this,false,f.getPath());
			if (f == null) {
				return;
			}

			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
		}

		CreateQrCodeDocument generator = new CreateQrCodeDocument();

		// Make sure the file has the correct extension
		String outputFile = f.getAbsolutePath();
		String ext = FilenameUtils.getExtension(outputFile);
		if( ext.compareToIgnoreCase(controls.format) != 0 ) {
			outputFile = FilenameUtils.removeExtension(outputFile);
			outputFile += "." + controls.format;
		}

		generator.fileName = outputFile;
		generator.error = controls.error;
		generator.mask = controls.mask;
		generator.encoding = controls.mode;
		generator.version = controls.version;
		generator.paperSize = controls.paperSize;
		generator.gridFill = controls.fillGrid;
		generator.hideInfo = controls.hideInfo;
		generator.messages = new ArrayList<>();
		generator.messages.add( controls.message );
		generator.unit = controls.documentUnits;
		generator.markerWidth = (float)controls.markerWidth;
		generator.sendToPrinter = sendToPrinter;

		try {
			generator.finishParsing();
			generator.run();
		} catch( IOException | RuntimeException e){
			System.out.println("Exception!!!");
			BoofSwingUtil.warningDialog(this,e);
		}
	}

	private void renderPreview() {
		QrCodeEncoder encoder = new QrCodeEncoder();

		if (controls.error != null) {
			encoder.setError(controls.error);
		}
		if (controls.mask != null) {
			encoder.setMask(controls.mask);
		}
		if( controls.version > 0 ) {
			encoder.setVersion(controls.version);
		}

		if( controls.mode != null ){
			switch( controls.mode ) {
				case NUMERIC:encoder.addNumeric(controls.message);break;
				case ALPHANUMERIC:encoder.addAlphanumeric(controls.message);break;
				case BYTE:encoder.addBytes(controls.message);break;
				case KANJI:encoder.addKanji(controls.message);break;
				default: encoder.addAutomatic(controls.message);break;
			}
		} else {
			encoder.addAutomatic(controls.message);
		}

		boolean failed = false;
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(10);
		try {
			generator.render(encoder.fixate());
		} catch( RuntimeException e ) {
			failed = true;
			System.err.println("Render Failed! "+e.getClass().getSimpleName()+" "+e.getMessage());
//			e.printStackTrace();
		}

		if( failed ) {
			BufferedImage output = new BufferedImage(100,100, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = output.createGraphics();
			g2.setColor(Color.RED);
			g2.fillRect(0,0,output.getWidth(),output.getHeight());
			imagePanel.setImageRepaint(output);
		} else {
			GrayU8 gray = generator.getGray();
			BufferedImage output = new BufferedImage(gray.width, gray.height, BufferedImage.TYPE_INT_RGB);
			ConvertBufferedImage.convertTo(gray, output);
			imagePanel.setImageRepaint(output);
		}

	}

	@Override
	public void controlsUpdates() {
		new Thread() {
			public void run() {
				renderPreview();
			}
		}.start();
	}

	public static void main(String[] args) {
		CreateQrCodeGui gui = new CreateQrCodeGui();
	}
}

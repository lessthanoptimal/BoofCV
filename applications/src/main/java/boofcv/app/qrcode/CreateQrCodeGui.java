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

import javax.swing.*;
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
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		JMenuItem menuSavePDF = new JMenuItem("Save as PDF", KeyEvent.VK_D);
		menuSavePDF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				savePDF();
			}
		});
		menuSavePDF.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));

		JMenuItem menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuQuit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		JMenuItem menuHelp = new JMenuItem("Help");
		menuHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showHelp();
			}
		});


		menu.addSeparator();
		menu.add(menuSavePDF);
		menu.add(menuHelp);
		menu.add(menuQuit);

		add(BorderLayout.NORTH,menuBar);
	}

	private void showHelp() {
		JOptionPane.showMessageDialog(this,"Many more options and better documentation available through commandline");
	}

	private void savePDF() {
		File f = BoofSwingUtil.saveFileChooser(this);
		if( f == null ) {
			return;
		}

		if( f.isDirectory() ) {
			JOptionPane.showMessageDialog(this,"Can't save to a directory!");
			return;
		}

		CreateQrCodeDocument generator = new CreateQrCodeDocument();

		generator.fileName = f.getAbsolutePath();
		generator.error = controls.error;
		generator.mask = controls.mask;
		generator.encoding = controls.mode;
		generator.version = controls.version;
		generator.paperSize = controls.paperSize;
		generator.gridFill = controls.fillGrid;
		generator.hideInfo = controls.hideInfo;
		generator.messages = new ArrayList<>();
		generator.messages.add( controls.message );

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
		} else if (controls.mask != null) {
			encoder.setMask(controls.mask);
		} else if( controls.version > 0 ) {
			encoder.setVersion(controls.version);
		}

		if( controls.mode != null ){
			switch( controls.mode ) {
				case NUMERIC:encoder.numeric(controls.message);break;
				case ALPHANUMERIC:encoder.alphanumeric(controls.message);break;
				case BYTE:encoder.bytes(controls.message);break;
				case KANJI:encoder.kanji(controls.message);break;
				default: encoder.encodeAuto(controls.message);break;
			}
		} else {
			encoder.encodeAuto(controls.message);
		}

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(10);
		try {
			generator.render(encoder.fixate());
		} catch( RuntimeException e ) {
			return;
		}
		GrayU8 gray = generator.getGray();
		BufferedImage output = new BufferedImage(gray.width,gray.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(gray,output);

		imagePanel.setImageRepaint(output);
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

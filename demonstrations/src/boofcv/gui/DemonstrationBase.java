/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Provides some common basic functionality for demonstrations
 *
 * @author Peter Abeles
 */
public abstract class DemonstrationBase extends JPanel {
	protected JMenuBar menuBar;
	JMenuItem menuFile, menuWebcam, menuQuit;
	final JFileChooser fc = new JFileChooser();

	protected InputMethod inputMethod = InputMethod.NONE;

	WebcamThread webcamThread;

	volatile boolean processRunning = false;
	volatile boolean processRequested = false;

	final Object processLock = new Object();
	BufferedImage imageCopy0;
	BufferedImage imageCopy1;

	public DemonstrationBase(List<String> exampleInputs) {
		setLayout(new BorderLayout());

		createMenuBar(exampleInputs);
	}

	private void createMenuBar(List<String> exampleInputs) {
		menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		ActionListener listener = createActionListener();

		menuFile = new JMenuItem("Open File", KeyEvent.VK_O);
		menuFile.addActionListener(listener);
		menuFile.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		menuWebcam = new JMenuItem("Open Webcam", KeyEvent.VK_W);
		menuWebcam.addActionListener(listener);
		menuWebcam.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuQuit.addActionListener(listener);
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		menu.add(menuFile);
		menu.add(menuWebcam);
		menu.addSeparator();
		menu.add(menuQuit);

		menu = new JMenu("Examples");
		menuBar.add(menu);

		for (final String path : exampleInputs) {
			JMenuItem menuItem = new JMenuItem(new File(path).getName());
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					openFile(new File(path));
				}
			});
			menu.add(menuItem);
		}

		add(BorderLayout.NORTH, menuBar);
	}

	/**
	 * Process the image.  Will be called in its own thread, but doesn't need to be re-entrant.  If image
	 * is null then reprocess the previous image.
	 */
	public abstract void processImage( BufferedImage image );

	protected void processImageThread( final BufferedImage image ) {
		synchronized (processLock) {
			if( image != null )
				imageCopy1 = checkCopyBuffered(image, imageCopy1);

			if( processRunning ) {
				processRequested = true;
			} else {
				processRequested = true;
				processRunning = true;

				new Thread() {
					@Override
					public void run() {
						while( true ) {
							synchronized (processLock) {
								if( !processRequested ) {
									break;
								}
								processRequested = false;
								if( image != null )
									imageCopy0 = checkCopyBuffered(imageCopy1, imageCopy0);
							}
							if( image != null )
								processImage(imageCopy0);
							else
								processImage(null);
						}

						synchronized (processLock) {
							processRunning = false;
						}
					}
				}.start();
			}
		}
	}

	private BufferedImage checkCopyBuffered(BufferedImage src, BufferedImage dst) {
		dst = conditionalDeclare(src, dst);
		dst.createGraphics().drawImage(src,0,0,null);
		return dst;
	}

	public static BufferedImage conditionalDeclare(BufferedImage template, BufferedImage output) {
		if( output == null ||
				output.getWidth() != template.getWidth() ||
				output.getHeight() != template.getHeight() ) {
			int type = template.getType();
			if( type == 0 ) {
				type = BufferedImage.TYPE_INT_RGB;
			}
			output = new BufferedImage(template.getWidth(),template.getHeight(),type);
		}
		return output;
	}

	public static BufferedImage conditionalDeclare(BufferedImage template, BufferedImage output, int type ) {
		if( output == null ||
				output.getWidth() != template.getWidth() ||
				output.getHeight() != template.getHeight() ) {
			output = new BufferedImage(template.getWidth(),template.getHeight(),type);
		}
		return output;
	}

	private void stopPreviousInput() {
		// TODO Open dialog saying "Shutting Down Webcam" and wait for it to close
		switch ( inputMethod ) {
			case WEBCAM:
				System.out.println("Stopping webcam!");
				webcamThread.requestStop = true;
		}
	}

	public void openFile(File file) {
		System.out.println("Opening file!!  "+file.getPath());

		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample(file.getPath()));
		if( buffered == null ) {
			// TODO see if it's a video instead
			System.err.println("Couldn't read "+file.getPath());
		} else {
			stopPreviousInput();
			inputMethod = InputMethod.IMAGE;
			processImageThread(buffered);
		}
	}

	public void openWebcam() {
		stopPreviousInput();

		// TODO open dialog saying "Opening WEbcam"
		inputMethod = InputMethod.WEBCAM;
		Webcam webcam = UtilWebcamCapture.openDefault(640,480);
		if( webcam.open() ) {
			webcamThread = new WebcamThread(webcam);
			webcamThread.start();
		}
	}

	private ActionListener createActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (menuFile == e.getSource()) {
					int returnVal = fc.showOpenDialog(DemonstrationBase.this);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						openFile(file);
					} else {
					}
				} else if (menuWebcam == e.getSource()) {
					openWebcam();
				} else if (menuQuit == e.getSource()) {
					System.exit(0);
				}
			}
		};
	}

	class WebcamThread extends Thread {

		boolean requestStop = false;
		boolean running = true;

		Webcam webcam;

		public WebcamThread(Webcam webcam) {
			this.webcam = webcam;
		}

		@Override
		public void run() {
			while( !requestStop ) {
				BufferedImage image = webcam.getImage();
				if( image == null ) {
					break;
				} else {
					processImageThread(image);
					Thread.yield();
				}
			}
			System.out.println("Exit webcam thread");
			webcam.close();
			running = false;
		}
	}

	protected enum InputMethod {
		NONE,
		IMAGE,
		VIDEO,
		WEBCAM
	}
}

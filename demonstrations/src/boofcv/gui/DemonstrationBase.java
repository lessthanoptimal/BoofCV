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

import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

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
public abstract class DemonstrationBase<T extends ImageBase> extends JPanel {
	protected JMenuBar menuBar;
	JMenuItem menuFile, menuWebcam, menuQuit;
	final JFileChooser fc = new JFileChooser();

	protected InputMethod inputMethod = InputMethod.NONE;

	ImageSequenceThread sequenceThread;

	volatile boolean waitingToOpenImage = false;
	final Object waitingLock = new Object();

	volatile boolean processRunning = false;
	volatile boolean processRequested = false;

	final Object processLock = new Object();
	BufferedImage imageCopy0;
	BufferedImage imageCopy1;

	protected ImageType<T> imageType;
	T input;
	MediaManager media = new DefaultMediaManager();

	public DemonstrationBase(List<String> exampleInputs, ImageType<T> imageType) {
		super(new BorderLayout());
		createMenuBar(exampleInputs);

		this.input = imageType.createImage(1,1);
		this.imageType = imageType;
	}

	private void createMenuBar(List<String> exampleInputs) {
		menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		ActionListener listener = createActionListener();

		menuFile = new JMenuItem("Open File", KeyEvent.VK_O);
		menuFile.addActionListener(listener);
		menuFile.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_O, ActionEvent.CTRL_MASK));
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
	public abstract void processImage(final BufferedImage buffered , final T input  );

	protected void processImageThread( final BufferedImage buffered , final T input ) {
		synchronized (processLock) {
			if( buffered != null )
				imageCopy1 = checkCopyBuffered(buffered, imageCopy1);

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
								if( buffered != null )
									imageCopy0 = checkCopyBuffered(imageCopy1, imageCopy0);
							}
							synchronized (waitingLock) {
								waitingToOpenImage = false;
							}
							if( buffered != null )
								processImage(imageCopy0,input);
							else
								processImage(null,null);
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
			int type;
			if( output == null ) {
				type = template.getType();
				if (type == 0) {
					type = BufferedImage.TYPE_INT_RGB;
				}
			} else {
				type = output.getType();
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
		switch ( inputMethod ) {
			case WEBCAM: stopSequenceRunning("Shutting down webcam");break;
			case VIDEO: stopSequenceRunning("Shutting down video");break;
		}
	}

	private void stopSequenceRunning( String message ) {
		sequenceThread.requestStop = true;
		WaitingThread waiting = new WaitingThread(message);
		waiting.start();
		while( sequenceThread.running ) {
			Thread.yield();
		}
		waiting.closeRequested = true;
	}

	/**
	 * Opens a file.  First it will attempt to open it as an image.  If that fails it will try opening it as a
	 * video.  If all else fails tell the user it has failed.  If a streaming source was running before it will
	 * be stopped.
	 */
	public void openFile(File file) {
		synchronized (waitingLock) {
			if (waitingToOpenImage) {
				System.out.println("Waiting to open an image");
				return;
			}
			waitingToOpenImage = true;
		}

		stopPreviousInput();

		String filePath = UtilIO.pathExample(file.getPath());
		// mjpegs can be opened up as images.  so override the default behavior
		BufferedImage buffered = filePath.endsWith("mjpeg") ? null : UtilImageIO.loadImage(filePath);
		if( buffered == null ) {
			SimpleImageSequence<T> sequence = media.openVideo(filePath, imageType);
			if( sequence != null ) {
				inputMethod = InputMethod.VIDEO;
				sequenceThread = new ImageSequenceThread(sequence,30);
				sequenceThread.start();
			} else {
				inputMethod = InputMethod.NONE;
				waitingToOpenImage = false;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(DemonstrationBase.this, "Can't open file");
					}
				});
			}
		} else {

			inputMethod = InputMethod.IMAGE;
			input.reshape(buffered.getWidth(),buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered,input,true);
			processImageThread(buffered, input);
		}
	}

	/**
	 * waits until the processing thread is done.
	 */
	public void waitUntilDoneProcessing() {
		while( processRunning ) {
			Thread.yield();
		}
	}

	public void openWebcam() {
		if(waitingToOpenImage)
			return;

		stopPreviousInput();

		waitingToOpenImage = true;
		inputMethod = InputMethod.WEBCAM;

		new WaitingThread("Opening Webcam").start();
		new Thread() {
			public void run() {
				SimpleImageSequence<T> sequence = media.openCamera(null,640,480,imageType);
				if(sequence != null) {
					sequenceThread = new ImageSequenceThread(sequence,0);
					sequenceThread.start();
				} else {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane.showMessageDialog(DemonstrationBase.this, "Failed to open webcam");
						}
					});
				}
			}
		}.start();
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

	class WaitingThread extends Thread {
		ProgressMonitor progress;
		public volatile boolean closeRequested = false;
		public WaitingThread( String message ) {
			progress = new ProgressMonitor(DemonstrationBase.this,message,"", 0, 100);
		}

		@Override
		public void run() {
			while( waitingToOpenImage && !closeRequested ) {
				SwingUtilities.invokeLater(new Runnable() { @Override public void run() { progress.setProgress(0); } });
				try {Thread.sleep(250);} catch (InterruptedException ignore) {}
			}
			SwingUtilities.invokeLater(
					new Runnable() { @Override public void run() { progress.close(); } }
			);
		}
	}

	class ImageSequenceThread extends Thread {

		boolean requestStop = false;
		boolean running = true;

		SimpleImageSequence<T> sequence;
		long pause;

		public ImageSequenceThread(SimpleImageSequence<T> sequence, long pause ) {
			this.sequence = sequence;
			this.pause = pause;
		}

		@Override
		public void run() {
			long before = System.currentTimeMillis();
			while( !requestStop && sequence.hasNext() ) {
				T input = sequence.next();

				if( input == null ) {
					break;
				} else {
					BufferedImage buffered = sequence.getGuiImage();
					processImageThread(buffered,input);
					if( pause > 0 ) {
						long time = Math.max(0,pause-(System.currentTimeMillis()-before));
						if( time > 0 ) {
							try {Thread.sleep(time);} catch (InterruptedException ignore) {}
						} else {
							Thread.yield();
						}
					}
					Thread.yield();
					before = System.currentTimeMillis();
				}
			}
			sequence.close();
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

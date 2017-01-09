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

package boofcv.gui;

import boofcv.io.MediaManager;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides some common basic functionality for demonstrations
 *
 * @author Peter Abeles
 */
public abstract class DemonstrationBase2 extends JPanel {
	protected JMenuBar menuBar;
	JMenuItem menuFile, menuWebcam, menuQuit;
	final JFileChooser fc = new JFileChooser();

	// controls by synchornized(inputStreams)
	protected InputMethod inputMethod = InputMethod.NONE;

	// When set to true the input's size is known and the GUI should be adjusted
	volatile boolean inputSizeKnown = false;

	protected String inputFilePath;

	// Storage for input list of input streams.  always synchronize before manipulating
	private final List<CacheSequenceStream> inputStreams = new ArrayList<>();

	private ProcessThread threadProcess; // controls by synchronized(inputStreams)

	// lock to ensure it doesn't try to start multiple processes at the same time
	private final Object lockStartingProcess = new Object();
	private volatile boolean startingProcess = false;

	protected MediaManager media = new DefaultMediaManager();

	// If true then any stream will be paused.  If a webcam is running it will skip new images
	// if a video it will stop processing the input
	protected volatile boolean streamPaused = false;

	// minimum elapsed time between the each stream frame being processed, in milliseconds
	protected volatile long streamPeriod = 30;

	public DemonstrationBase2(boolean openFile , boolean openWebcam, List<?> exampleInputs, ImageType ...defaultTypes) {
		super(new BorderLayout());
		createMenuBar(openFile, openWebcam, exampleInputs);

		setImageTypes(defaultTypes);
	}

	/**
	 * Constructor that specifies examples and input image type
	 *
	 * @param exampleInputs List of paths to examples.  Either a String file path or {@link PathLabel}.
	 * @param defaultTypes Type of image in each stream
	 */
	public DemonstrationBase2(List<?> exampleInputs, ImageType ...defaultTypes) {
		this(true,true,exampleInputs, defaultTypes);
	}

	public void setImageTypes( ImageType ...defaultTypes ) {
		synchronized ( inputStreams ) {
			for (ImageType type : defaultTypes) {
				inputStreams.add(new CacheSequenceStream(type));
			}
		}
	}

	/**
	 * Get input input type for a stream safely
	 */
	protected <T extends ImageBase> ImageType<T> getImageType( int which ) {
		synchronized ( inputStreams ) {
			return inputStreams.get(which).imageType;
		}
	}

	private void createMenuBar(boolean openFile , boolean openWebcam , List<?> exampleInputs) {
		menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		ActionListener listener = createActionListener();

		if( openFile ) {
			menuFile = new JMenuItem("Open File", KeyEvent.VK_O);
			menuFile.addActionListener(listener);
			menuFile.setAccelerator(KeyStroke.getKeyStroke(
					KeyEvent.VK_O, ActionEvent.CTRL_MASK));
			menu.add(menuFile);
		}
		if( openWebcam ) {
			menuWebcam = new JMenuItem("Open Webcam", KeyEvent.VK_W);
			menuWebcam.addActionListener(listener);
			menuWebcam.setAccelerator(KeyStroke.getKeyStroke(
					KeyEvent.VK_W, ActionEvent.CTRL_MASK));
			menu.add(menuWebcam);
		}
		menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuQuit.addActionListener(listener);
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		menu.addSeparator();
		menu.add(menuQuit);

		if( exampleInputs != null && exampleInputs.size() > 0 ) {
			menu = new JMenu("Examples");
			menuBar.add(menu);

			for (final Object o : exampleInputs) {
				String name;

				if (o instanceof PathLabel) {
					name = ((PathLabel) o).getLabel();
				} else if (o instanceof String) {
					name = new File((String) o).getName();
				} else {
					name = o.toString();
				}
				JMenuItem menuItem = new JMenuItem(name);
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						openExample(o);
					}
				});
				menu.add(menuItem);
			}
		}

		add(BorderLayout.NORTH, menuBar);
	}

	/**
	 * Function that is invoked when an example has been selected
	 */
	public void openExample( Object o ) {
		String path;
		if (o instanceof PathLabel) {
			path = ((PathLabel) o).getPath();
		} else if (o instanceof String) {
			path = (String) o;
		} else {
			throw new IllegalArgumentException("Unknown example object type.  Please override openExample()");
		}
		openFile(new File(path));
	}

	/**
	 * Blocks until it kills all input streams from running
	 */
	public void stopAllInputProcessing() {
		ProcessThread threadProcess;
		synchronized (inputStreams) {
			threadProcess = this.threadProcess;
			if( threadProcess != null ) {
				if( threadProcess.running ) {
					threadProcess.requestStop = true;
				} else {
					threadProcess = this.threadProcess = null;
				}
			}
		}

		inputSizeKnown = false;

		if( threadProcess == null ) {
			return;
		}

		long timeout = System.currentTimeMillis()+5000;
		while( threadProcess.running && timeout >= System.currentTimeMillis() ) {
			synchronized (inputStreams) {
				if( threadProcess != this.threadProcess ) {
					throw new RuntimeException("BUG! the thread got modified by anotehr process");
				}
			}

			BoofMiscOps.sleep(100);
		}

		if( timeout < System.currentTimeMillis() )
			throw new RuntimeException("Took too long to stop input processing thread");

		this.threadProcess = null;
	}

	/**
	 * Override to be notified when the input has changed.  This is also a good location to change the default
	 * max FPS for streaming data.  It will be 0 for webcam and 30 FPS for videos
	 *
	 * @param method Type of input source
	 * @param width Width of input image
	 * @param height Height of input image
	 */
	protected void handleInputChange( int source , InputMethod method , int width , int height ) {

	}

	/**
	 * Process the image.  Will be called in its own thread, but doesn't need to be re-entrant.  If image
	 * is null then reprocess the previous image.
	 */
	public abstract void processImage(int sourceID, long frameID, final BufferedImage buffered , final ImageBase input  );

	/**
	 * Opens a file.  First it will attempt to open it as an image.  If that fails it will try opening it as a
	 * video.  If all else fails tell the user it has failed.  If a streaming source was running before it will
	 * be stopped.
	 */
	public void openFile(File file) {
		// maybe it's an example file
		if( !file.exists() ) {
			file = new File(UtilIO.pathExample(file.getPath()));
		}
		if( !file.exists() ) {
			System.err.println("Can't find file "+file.getPath());
			return;
		}

		// mjpegs can be opened up as images.  so override the default behavior
		String inputFilePath = file.getPath();
		BufferedImage buffered = inputFilePath.endsWith("mjpeg") ? null : UtilImageIO.loadImage(inputFilePath);
		if( buffered == null ) {
			openVideo(inputFilePath);
		} else {
			openImage(inputFilePath, buffered);
		}
	}

	/**
	 * Before invoking this function make sure waitingToOpenImage is false AND that the previous input has beens topped
	 */
	protected void openVideo(String filePath) {
		synchronized (lockStartingProcess) {
			if( startingProcess ) {
				System.out.println("Ignoring video request.  Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != 1)
				throw new IllegalArgumentException("Input streams not equal to 1.  Override openVideo()");
		}

		stopAllInputProcessing();

		CacheSequenceStream cache = inputStreams.get(0);
		SimpleImageSequence sequence = media.openVideo(filePath, cache.getImageType());

		if (sequence != null) {
			synchronized (inputStreams) {
				inputFilePath = filePath;
				inputMethod = InputMethod.VIDEO;
				streamPeriod = 33; // default to 33 FPS for a video
				cache.reset();
				cache.setSequence(sequence);
				if( threadProcess != null )
					throw new RuntimeException("There was still an active stream thread!");
				threadProcess = new SynchronizedStreamsThread();
			}
			threadProcess.start();
		} else {
			synchronized (inputStreams) {
				inputMethod = InputMethod.NONE;
				inputFilePath = null;
			}
			synchronized (lockStartingProcess) {
				startingProcess = false;
			}
			showRejectDiaglog("Can't open file");
		}
	}

	protected void openImage(String filePath , BufferedImage buffered ) {
		synchronized (lockStartingProcess) {
			if( startingProcess ) {
				System.out.println("Ignoring image request.  Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != 1)
				throw new IllegalArgumentException("Input streams not equal to 1.  Override openImage()");
		}

		stopAllInputProcessing();

		synchronized (inputStreams) {
			inputMethod = InputMethod.IMAGE;
			inputFilePath = filePath;

			// copy the image into the cache
			CacheSequenceStream cache = inputStreams.get(0);
			cache.reset();
			ImageBase boof = cache.getBoofImage();
			boof.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, boof, true);
			cache.setBufferedImage(buffered);

			if( threadProcess != null )
				throw new RuntimeException("There was still an active stream thread!");
			threadProcess = new ProcessImageThread();
		}
		threadProcess.start();
	}

	public void openWebcam() {
		synchronized (lockStartingProcess) {
			if( startingProcess ) {
				System.out.println("Ignoring webcam request.  Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != 1)
				throw new IllegalArgumentException("Input streams not equal to 1.  Override openImage()");
		}

		stopAllInputProcessing();

		synchronized (inputStreams) {
			inputMethod = InputMethod.WEBCAM;
			inputFilePath = null;
			streamPeriod = 0; // default to no delay in processing for a real time stream

			CacheSequenceStream cache = inputStreams.get(0);
			SimpleImageSequence sequence = media.openCamera(null, 640, 480, cache.getImageType());

			if (sequence == null) {
				showRejectDiaglog("Can't open webcam");
			} else {
				cache.reset();
				cache.setSequence(sequence);

				if (threadProcess != null)
					throw new RuntimeException("There was still an active stream thread!");
				threadProcess = new SynchronizedStreamsThread();
				threadProcess.start();
			}
		}
	}

	/**
	 * waits until the processing thread is done.
	 */
	public void waitUntilInputSizeIsKnown() {
		while( !inputSizeKnown ) {
			BoofMiscOps.sleep(5);
		}
	}

	/**
	 * Displays a dialog box letting the user know it can't perform the requested action
	 */
	private void showRejectDiaglog( String message ) {
		JOptionPane.showMessageDialog(null, message);
	}

	private ActionListener createActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (menuFile == e.getSource()) {
					int returnVal = fc.showOpenDialog(DemonstrationBase2.this);

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

	class ProcessThread extends Thread {
		volatile boolean requestStop = false;
		volatile boolean running = true;
	}

	class ProcessImageThread extends ProcessThread {

		@Override
		public void run() {
			for (int i = 0; i < inputStreams.size() ; i++) {
				CacheSequenceStream cache = inputStreams.get(i);
				handleInputChange(i, inputMethod, cache.getWidth(), cache.getHeight());
				inputSizeKnown = true;

				ImageBase boof = cache.getBoofImage();
				BufferedImage buff = cache.getBufferedImage();

				processImage(i,0, buff, boof);
			}

			// Request spam prevention.  Must complete the request before it will accept the new one
			synchronized (lockStartingProcess) {
				startingProcess = false;
			}

			running = false;
		}
	}

	class SynchronizedStreamsThread extends ProcessThread {
		@Override
		public void run() {
			for (int i = 0; i < inputStreams.size() ; i++) {
				CacheSequenceStream sequence = inputStreams.get(i);
				handleInputChange(i, inputMethod, sequence.getWidth(), sequence.getHeight());
			}
			inputSizeKnown = true;

			boolean first = true;

			long before = System.currentTimeMillis();
			while( !requestStop ) {
				// see if all the streams have more data available
				boolean allNext = true;
				for (int i = 0; i < inputStreams.size() ; i++) {
					if( !inputStreams.get(i).hasNext() ) {
						allNext = false;
						break;
					}
				}

				// stop processing if they don't all have data available
				if( !allNext ) {
					break;
				}

				// grab images from all the streams and save local copy
				for (int i = 0; i < inputStreams.size() ; i++) {
					inputStreams.get(i).cacheNext();
				}

				if( first ) { // process at least one image before letting it try to process another source
					first = false;
					synchronized (lockStartingProcess) {
						startingProcess = false;
					}
				}

				// feed images to client - They will own the image data until they are passed new image data
				for (int i = 0; i < inputStreams.size() ; i++) {
					CacheSequenceStream cache = inputStreams.get(i);
					int frameID = cache.sequence.getFrameNumber();
					ImageBase boof = cache.getBoofImage();
					BufferedImage buff = cache.getBufferedImage();

					processImage(i,frameID, buff, boof);
				}

				// Throttle speed if requested
				if( streamPeriod > 0 ) {
					long time = Math.max(0, streamPeriod -(System.currentTimeMillis()-before));
					if( time > 0 ) {
						try {Thread.sleep(time);} catch (InterruptedException ignore) {}
					} else {
						try {Thread.sleep(5);} catch (InterruptedException ignore) {}
					}
				} else {
					try {Thread.sleep(5);} catch (InterruptedException ignore) {}
				}
				before = System.currentTimeMillis();

				// Check to see if paused and wait
				if( streamPaused && inputMethod == InputMethod.VIDEO ) {
					while( streamPaused && !requestStop ) {
						try {Thread.sleep(5);} catch (InterruptedException ignore) {}
					}
				}
			}

			// clean up
			for (int i = 0; i < inputStreams.size() ; i++) {
				inputStreams.get(i).sequence.close();
			}

			running = false;
		}
	}

	/**
	 * If just a single image was processed it will process it again.  If it's a stream
	 * there is no need to reprocess, the next image will be handled soon enough.
	 */
	public void reprocessInput() {
		if ( inputMethod == InputMethod.VIDEO ) {
			openVideo(inputFilePath);
		} else if( inputMethod == InputMethod.IMAGE ) {
			BufferedImage buff = inputStreams.get(0).getBufferedImage();
			openImage(inputFilePath,buff);// TODO still does a pointless image conversion
		}
	}

	protected enum InputMethod {
		NONE,
		IMAGE,
		VIDEO,
		WEBCAM
	}
}

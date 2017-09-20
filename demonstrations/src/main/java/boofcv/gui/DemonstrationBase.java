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

import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.OpenWebcamDialog;
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
public abstract class DemonstrationBase extends JPanel {
	protected JMenuBar menuBar;
	JMenuItem menuFile, menuWebcam, menuQuit;
	JMenu menuRecent;

	// Window the application is shown in
	JFrame window;

	// name of the application
	String appName;

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
	protected boolean allowVideos = true;
	protected boolean allowImages = true;

	// If true then any stream will be paused.  If a webcam is running it will skip new images
	// if a video it will stop processing the input
	protected volatile boolean streamPaused = false;

	// specifies how many frames it should move before pausing
	protected volatile int streamStepCounter = 0;

	// minimum elapsed time between the each stream frame being processed, in milliseconds
	protected volatile long streamPeriod = 30;

	public DemonstrationBase(boolean openFile , boolean openWebcam, List<?> exampleInputs, ImageType ...defaultTypes) {
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
	public DemonstrationBase(List<?> exampleInputs, ImageType ...defaultTypes) {
		this(true,true,exampleInputs, defaultTypes);
	}

	public void setImageTypes( ImageType ...defaultTypes ) {
		synchronized ( inputStreams ) {
			inputStreams.clear();
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

			menuRecent = new JMenu("Open Recent");
			menu.add(menuRecent);
			updateRecentItems();
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
	 * Updates the list in recent menu
	 */
	private void updateRecentItems() {
		if( menuRecent == null )
			return;
		menuRecent.removeAll();
		List<String> recentFiles = BoofSwingUtil.getListOfRecentFiles(this);
		for( String filePath : recentFiles ) {
			final File f = new File(filePath);
			JMenuItem recentItem = new JMenuItem(f.getName());
			recentItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					openFile(f);
				}
			});
			menuRecent.add(recentItem);
		}
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
	 * A streaming source of images has closed.
	 * @param source
	 */
	protected void handleInputClose( int source ) {

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

		// update recent items menu
		final File _file = file;
		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				BoofSwingUtil.addToRecentFiles(DemonstrationBase.this,_file.getAbsolutePath());
				updateRecentItems();
			}
		});

		// mjpegs can be opened up as images.  so override the default behavior
		inputFilePath = file.getPath();
		BufferedImage buffered = inputFilePath.endsWith("mjpeg") ? null : UtilImageIO.loadImage(inputFilePath);
		if( buffered == null ) {
			if( allowVideos )
				openVideo(false,inputFilePath);
		} else if( allowImages ){
			openImage(false,inputFilePath, buffered);
		}
	}

	/**
	 * Before invoking this function make sure waitingToOpenImage is false AND that the previous input has beens topped
	 */
	protected void openVideo(boolean reopen , String ...filePaths) {
		synchronized (lockStartingProcess) {
			if( startingProcess ) {
				System.out.println("Ignoring video request.  Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != filePaths.length)
				throw new IllegalArgumentException("Input streams not equal to "+filePaths.length+".  Override openVideo()");
		}

		stopAllInputProcessing();

		boolean failed = false;
		for( int which = 0; which < filePaths.length; which++ ) {
			CacheSequenceStream cache = inputStreams.get(which);

			SimpleImageSequence sequence = media.openVideo(filePaths[which], cache.getImageType());

			if( sequence == null ) {
				failed = true;
				break;
			} else {
				synchronized (inputStreams) {
					cache.reset();
					cache.setSequence(sequence);
				}
			}
		}

		if (!failed) {
			setInputName(new File(filePaths[0]).getName());
			synchronized (inputStreams) {
				inputMethod = InputMethod.VIDEO;
				streamPeriod = 33; // default to 33 FPS for a video
				if( threadProcess != null )
					throw new RuntimeException("There was still an active stream thread!");
				threadProcess = new SynchronizedStreamsThread();
			}
			if( !reopen ) {
				for (int i = 0; i < inputStreams.size(); i++) {
					CacheSequenceStream stream = inputStreams.get(i);
					handleInputChange(i, inputMethod, stream.getWidth(), stream.getHeight());
				}
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

	protected void openImage(boolean reopen , String filePath , BufferedImage buffered ) {
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
		if( !reopen ) {
			setInputName(new File(filePath).getName());
			handleInputChange(0, inputMethod, buffered.getWidth(), buffered.getHeight());
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

		// Let he user select and configure the webcam. If canceled it will return null
		OpenWebcamDialog.Selection s = OpenWebcamDialog.showDialog(null);
		if( s == null ) {
			synchronized (lockStartingProcess) {
				startingProcess = false;
			}
			return;
		}

		synchronized (inputStreams) {
			inputMethod = InputMethod.WEBCAM;
			inputFilePath = null;
			streamPeriod = 0; // default to no delay in processing for a real time stream

			CacheSequenceStream cache = inputStreams.get(0);
			SimpleImageSequence sequence =
					media.openCamera(s.camera.getName(), s.width, s.height, cache.getImageType());

			if (sequence == null) {
				showRejectDiaglog("Can't open webcam");
			} else {
				cache.reset();
				cache.setSequence(sequence);

				if (threadProcess != null)
					throw new RuntimeException("There was still an active stream thread!");
				setInputName("Webcam");
				handleInputChange(0, inputMethod, sequence.getNextWidth(), sequence.getNextHeight());
				threadProcess = new SynchronizedStreamsThread();
				threadProcess.start();
			}
		}
	}

	private void setInputName( String name ) {
		if( window != null ) {
			window.setTitle(appName+":  "+name);
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
	 * Opens a window with this application inside of it
	 * @param appName Name of the application
	 */
	public void display(String appName ) {
		waitUntilInputSizeIsKnown();
		this.appName = appName;
		window = ShowImages.showWindow(this,appName,true);
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
					File file = BoofSwingUtil.openFileChooseDialog(DemonstrationBase.this);
					if (file != null) {
						openFile(file);
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

				if( streamStepCounter > 0 ) {
					if( --streamStepCounter == 0 )
						streamPaused = true;
				}

				// Check to see if paused and wait
				if( streamPaused && inputMethod == InputMethod.VIDEO ) {
					enterPausedState();
					while( streamPaused && !requestStop ) {
						try {Thread.sleep(5);} catch (InterruptedException ignore) {}
					}
				}
			}

			// clean up
			for (int i = 0; i < inputStreams.size() ; i++) {
				inputStreams.get(i).sequence.close();
				handleInputClose(i);
			}

			running = false;
		}
	}

	protected void enterPausedState() {}

	/**
	 * If just a single image was processed it will process it again.  If it's a stream
	 * there is no need to reprocess, the next image will be handled soon enough.
	 */
	public void reprocessInput() {
		if ( inputMethod == InputMethod.VIDEO ) {
			openVideo(true,inputFilePath);
		} else if( inputMethod == InputMethod.IMAGE ) {
			BufferedImage buff = inputStreams.get(0).getBufferedImage();
			openImage(true,inputFilePath,buff);// TODO still does a pointless image conversion
		}
	}

	/**
	 * Invokes {@link #reprocessInput()} only if the input is an IMAGE
	 */
	public void reprocessImageOnly() {
		if( inputMethod == InputMethod.IMAGE ) {
			reprocessInput();
		}
	}

	protected enum InputMethod
	{
		NONE,
		IMAGE,
		VIDEO,
		WEBCAM
	}
}

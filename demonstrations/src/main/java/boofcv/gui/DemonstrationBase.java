/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.gui.settings.GlobalSettingsControls;
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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static boofcv.io.UtilIO.UTF8;
import static boofcv.io.UtilIO.systemToUnix;

/**
 * Provides some common basic functionality for demonstrations
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class DemonstrationBase extends JPanel {
	protected JMenuBar menuBar;
	JMenuItem menuItemFile, menuItemWebcam, menuItemQuit;
	JMenu menuRecent;

	// Window the application is shown in
	protected JFrame window;

	// name of the application
	String appName;

	// controls by synchornized(inputStreams)
	protected InputMethod inputMethod = InputMethod.NONE;

	// if set to true then the input isn't assumed to be images any more and references to files is passed in
	protected boolean inputAsFile = false;

	// When set to true the input's size is known and the GUI should be adjusted
	volatile boolean inputSizeKnown = false;

	protected @Nullable String inputFilePath;
	protected String[] inputFileSet;

	// Storage for input list of input streams. always synchronize before manipulating
	private final List<CacheSequenceStream> inputStreams = new ArrayList<>();

	private @Nullable ProcessThread threadProcess; // controls by synchronized(inputStreams)
	// threadpool is used mostly for profiling purposes. This way there isn't a million threads being created
	protected LinkedBlockingQueue threadQueue = new LinkedBlockingQueue();
	protected ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 50, TimeUnit.MILLISECONDS,
			threadQueue);

	// lock to ensure it doesn't try to start multiple processes at the same time
	private final Object lockStartingProcess = new Object();
	private volatile boolean startingProcess = false;

	protected MediaManager media = new DefaultMediaManager();
	// Will force a custom function for opening a file. This is required if multiple files are selected as input
	protected boolean useCustomOpenFiles = false;
	protected boolean allowVideos = true;
	protected boolean allowImages = true;

	// If true then any stream will be paused. If a webcam is running it will skip new images
	// if a video it will stop processing the input
	protected volatile boolean streamPaused = false;

	// specifies how many frames it should move before pausing
	protected int streamStepCounter = 0;

	// minimum elapsed time between the each stream frame being processed, in milliseconds
	protected volatile long streamPeriod = 30;

	// If the input is an image set, this specifies how many elements are in the image set
	protected int imageSetSize;

	{
		BoofSwingUtil.initializeSwing();
	}

	protected DemonstrationBase( boolean openFile, boolean openWebcam, List<?> exampleInputs, ImageType... defaultTypes ) {
		super(new BorderLayout());

		createMenuBar(openFile, openWebcam, exampleInputs);

		setImageTypes(defaultTypes);
	}

	/**
	 * Constructor that specifies examples and input image type
	 *
	 * @param exampleInputs List of paths to examples. Either a String file path or {@link PathLabel}.
	 * @param defaultTypes Type of image in each stream
	 */
	protected DemonstrationBase( List<?> exampleInputs, ImageType... defaultTypes ) {
		this(true, true, exampleInputs, defaultTypes);
	}

	public void setImageTypes( ImageType... defaultTypes ) {
		synchronized (inputStreams) {
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
		synchronized (inputStreams) {
			return inputStreams.get(which).imageType;
		}
	}

	private void createMenuBar( boolean openFile, boolean openWebcam, List<?> exampleInputs ) {
		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menuFile);

		ActionListener listener = createActionListener();

		if (openFile) {
			this.menuItemFile = new JMenuItem("Open File");
			BoofSwingUtil.setMenuItemKeys(menuItemFile, KeyEvent.VK_O, KeyEvent.VK_O);
			this.menuItemFile.addActionListener(listener);
			menuFile.add(this.menuItemFile);

			JMenuItem menuItemNext = new JMenuItem("Open Next File");
			BoofSwingUtil.setMenuItemKeys(menuItemNext, KeyEvent.VK_N, KeyEvent.VK_I);
			menuItemNext.addActionListener(e -> openNextFile());
			menuFile.add(menuItemNext);

			JMenuItem menuItemReopen = new JMenuItem("Reprocess");
			BoofSwingUtil.setMenuItemKeys(menuItemReopen, KeyEvent.VK_R, KeyEvent.VK_R);
			menuItemReopen.addActionListener(e -> reprocessInput());
			menuFile.add(menuItemReopen);

			menuRecent = new JMenu("Open Recent");
			menuFile.add(menuRecent);
			updateRecentItems();
		}
		if (openWebcam) {
			menuItemWebcam = new JMenuItem("Open Webcam");
			BoofSwingUtil.setMenuItemKeys(menuItemWebcam, KeyEvent.VK_W, KeyEvent.VK_W);
			menuItemWebcam.addActionListener(listener);
			menuFile.add(menuItemWebcam);
		}

		customAddToFileMenu(menuFile);

		JMenuItem menuSettings = new JMenuItem("Settings");
		menuSettings.addActionListener(e -> new GlobalSettingsControls().showDialog(window, this));

		menuItemQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuItemQuit.addActionListener(listener);
		BoofSwingUtil.setMenuItemKeys(menuItemQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);

		menuFile.addSeparator();
		menuFile.add(menuSettings);
		menuFile.add(menuItemQuit);

		if (exampleInputs != null && exampleInputs.size() > 0) {
			JMenu menuExamples = new JMenu("Examples");
			menuExamples.setMnemonic(KeyEvent.VK_E);
			menuBar.add(menuExamples);

			for (final Object o : exampleInputs) {
				String name;

				if (o instanceof PathLabel) {
					name = ((PathLabel)o).getLabel();
				} else if (o instanceof String) {
					name = new File((String)o).getName();
				} else {
					name = o.toString();
				}
				JMenuItem menuItem = new JMenuItem(name);
				menuItem.addActionListener(e -> openExample(o));
				menuExamples.add(menuItem);
			}
		}
	}

	/**
	 * Override this method to add custom items to the file menu
	 */
	protected void customAddToFileMenu( JMenu menuFile ) {}

	protected void setMenuBarEnabled( boolean enabled ) {

		// The commented out code is to be a reminder why this was done this way.
		// In OS X it was possible to disable the menu bar but it would not become re-enabled. You had to switch
		// away from the app and come back. Obvious workarounds did not work. What did work was disabling menu items
		// instead of everything.
		// https://stackoverflow.com/questions/32085966/jmenubar-does-not-enable-after-being-disabled

		//		menuBar.setEnabled(enabled);
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			//		menuBar.getMenu(i).setEnabled(enabled);
			JMenu menu = menuBar.getMenu(i);
			if (menu == null)
				continue;
			for (int j = 0; j < menu.getItemCount(); j++) {
				if (menu.getItem(j) == null)
					continue;
				menu.getItem(j).setEnabled(enabled);
			}
		}
	}

	/**
	 * Updates the list in recent menu
	 */
	protected void updateRecentItems() {
		BoofSwingUtil.updateRecentItems(this, menuRecent, ( info ) -> {
			if (useCustomOpenFiles) {
				openFiles(BoofMiscOps.toFileList(info.files), true);
			} else if (info.files.size() == 1) {
				openFile(new File(info.files.get(0)), true);
			} else {
				openFiles(BoofMiscOps.toFileList(info.files), true);
			}
		});
	}

	/**
	 * Function that is invoked when an example has been selected
	 */
	public void openExample( Object o ) {
		if (o instanceof PathLabel) {
			PathLabel p = (PathLabel)o;
			for (int i = 0; i < p.path.length; i++) {
				p.path[i] = massageExampleFilePath(p.path[i]);
			}

			if (useCustomOpenFiles) {
				openFiles(p.getPathFiles(), false);
				return;
			}

			if (p.path.length == 1)
				openFile(new File(p.path[0]), false);
			else {
//				openFile(new File(p.path[0]));
				if (allowImages)
					openImageSet(false, p.path);
				else
					openVideo(false, p.path);
			}
		} else if (o instanceof String) {
			String path = Objects.requireNonNull(massageExampleFilePath((String)o));
			openFile(new File(path), false);
		} else {
			throw new IllegalArgumentException("Unknown example object type. Please override openExample()");
		}
	}

	/**
	 * Blocks until it kills all input streams from running
	 */
	public void stopAllInputProcessing() {
		ProcessThread threadProcess;
		synchronized (inputStreams) {
			threadProcess = this.threadProcess;
			if (threadProcess != null) {
				if (threadProcess.running) {
					threadProcess.requestStop = true;
				} else {
					threadProcess = this.threadProcess = null;
				}
			}
		}

		inputSizeKnown = false;

		if (threadProcess == null) {
			return;
		}

		long timeout = System.currentTimeMillis() + 5000;
		while (threadProcess.running && timeout >= System.currentTimeMillis()) {
			synchronized (inputStreams) {
				if (threadProcess != this.threadProcess) {
					throw new RuntimeException("BUG! the thread got modified by anotehr process");
				}
			}

			BoofMiscOps.sleep(100);
		}

		if (timeout < System.currentTimeMillis())
			throw new RuntimeException("Took too long to stop input processing thread");

		this.threadProcess = null;
	}

	/**
	 * Override to be notified when the input has changed. This is also a good location to change the default
	 * max FPS for streaming data. It will be 0 for webcam and 30 FPS for videos.
	 *
	 * If overloaded you don't need to call the super
	 *
	 * @param method Type of input source
	 * @param width Width of input image
	 * @param height Height of input image
	 */
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {

	}

	/**
	 * Couldn't read an input source. This reports the failure
	 */
	protected void handleInputFailure( int source, String error ) {
		System.err.println(error);
	}

	/**
	 * A streaming source of images has closed.
	 */
	protected void handleInputClose( int source ) {

	}

	/**
	 * Process the image. Will be called in its own thread, but doesn't need to be re-entrant. If image
	 * is null then reprocess the previous image.
	 */
	public abstract void processImage( int sourceID, long frameID, final BufferedImage buffered, final ImageBase input );

	/**
	 * Called when the input is a set of files and not image based
	 */
	public void processFiles( String[] filePaths ) {}

	/**
	 * Opens a file. First it will attempt to open it as an image. If that fails it will try opening it as a
	 * video. If all else fails tell the user it has failed. If a streaming source was running before it will
	 * be stopped.
	 */
	public void openFile( File file ) {
		openFile(file, true);
	}

	/**
	 * Opens a file. First it will attempt to open it as an image. If that fails it will try opening it as a
	 * video. If all else fails tell the user it has failed. If a streaming source was running before it will
	 * be stopped.
	 */
	public void openFile( File file, boolean addToRecent ) {
		inputFilePath = systemToUnix(file.getPath());

		// update recent items menu
		if (addToRecent) {
			String path = inputFilePath;
			BoofSwingUtil.invokeNowOrLater(() -> {
				BoofSwingUtil.addToRecentFiles(DemonstrationBase.this,
						selectRecentFileName(BoofMiscOps.asList(file)), BoofMiscOps.asList(path));
				updateRecentItems();
			});
		}

		BufferedImage buffered = inputFilePath.endsWith("mjpeg") || !allowImages ? null : UtilImageIO.loadImage(inputFilePath);
		if (buffered == null) {
			if (allowVideos)
				openVideo(false, inputFilePath);
		} else if (allowImages) {
			openImage(false, file.getName(), buffered);
		}
	}

	public @Nullable String massageExampleFilePath( final String path ) {
		String modifiedPath = systemToUnix(path);
//		System.out.println("demo.openFile() = "+path);
		URL url = null;

		try {
			url = new URL(modifiedPath);
			if (!UtilIO.validURL(url)) {
//				System.out.println("  invalid URL");
				url = null;
			}
		} catch (MalformedURLException ignore) {
		}

		if (url == null) {
//			System.out.println("Invalid URL");
			try {
				url = new File(UtilIO.pathExample(path)).toURI().toURL();
				if (!UtilIO.validURL(url)) {
					System.err.println("Can't open " + path);
					return null;
				}
				return url.toString();
			} catch (MalformedURLException e) {
				System.err.println(e.getMessage());
				return null;
			}
		} else {
			// input URL was valid, so use it directly
			return modifiedPath;
//			System.out.println("Valid URL using "+inputFilePath);
		}
	}

	/**
	 * Opens a set of images
	 */
	public void openImageSet( boolean reopen, String... files ) {
		synchronized (lockStartingProcess) {
			if (startingProcess) {
				System.out.println("Ignoring openImageSet() request. Detected spamming");
				return;
			}
			startingProcess = true;
		}

		stopAllInputProcessing();

		if (inputAsFile) {
			inputFileSet = files;
			threadProcess = new ProcessFileSetThread(files);
			imageSetSize = files.length;
		} else {
			synchronized (inputStreams) {
				inputMethod = InputMethod.IMAGE_SET;
				inputFileSet = files;
				if (threadProcess != null)
					throw new RuntimeException("There is still an active stream thread!");
				threadProcess = new ProcessImageSetThread(files);
				imageSetSize = files.length;
			}
		}
		threadPool.execute(threadProcess);
	}

	/**
	 * Opens the next file in the directory by lexicographical order.
	 */
	public void openNextFile() {
		if (inputFilePath == null || inputMethod != InputMethod.IMAGE)
			return;

		String path;
		try {
			// need to remove annoying %20 from the path is there is whitespace
			path = URLDecoder.decode(inputFilePath, UTF8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		File current = new File(UtilIO.ensureUrlNotNull(path).getFile());
		File parent = current.getParentFile();
		if (parent == null)
			return;

		File[] files = parent.listFiles();
		if (files == null || files.length <= 1)
			return;
		File closest = null;

		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			String name = f.getName().toLowerCase();
			if (inputMethod == InputMethod.IMAGE) {
				if (!f.isFile())
					continue;
				if (!UtilImageIO.isImage(f))
					continue;
			} else {
				// filter out common non image/video files
				if (name.endsWith(".txt") || name.endsWith(".yaml") || name.endsWith(".xml"))
					continue;
			}

			if (current.compareTo(f) < 0) {
				if (closest == null || closest.compareTo(f) > 0) {
					closest = f;
				}
			}
		}

		if (closest != null && closest.isFile()) {
			openFile(closest, true);
		} else {
			if (closest != null) {
				System.err.println("Next file isn't a file. name=" + closest.getName());
			} else {
				System.err.println("No valid closest file found.");
			}
		}
	}

	protected void openFiles( List<File> filePaths ) {
		this.openFiles(filePaths, true);
	}

	/**
	 * Opens a set of files using a custom function to extract images and sequences
	 */
	protected void openFiles( List<File> filePaths, boolean addToRecent ) {
		if (filePaths.size() == 0)
			return;

		// Need to massage the file path to work inside of Jars
		inputFileSet = new String[filePaths.size()];
		for (int i = 0; i < filePaths.size(); i++) {
			inputFileSet[i] = filePaths.get(i).getPath();
		}
		inputFilePath = inputFileSet[0];
		String[] copyFileSet = inputFileSet.clone();

		// update recent items menu
		if (addToRecent) {
			BoofSwingUtil.invokeNowOrLater(() -> {
				BoofSwingUtil.addToRecentFiles(DemonstrationBase.this,
						selectRecentFileName(filePaths), BoofMiscOps.asList(inputFileSet));
				updateRecentItems();
			});
		}

		stopAllInputProcessing();

		List<String> sequences = new ArrayList<>();
		List<String> images = new ArrayList<>();

		if (!openCustomFiles(inputFileSet, sequences, images))
			return;

		if (!sequences.isEmpty() && !images.isEmpty())
			throw new IllegalArgumentException("Only one of these can be not empty");

		if (!sequences.isEmpty()) {
			openVideo(false, sequences.toArray(new String[0]));
		} else {
			openImageSet(false, images.toArray(new String[0]));
		}

		// openVideo() and openImageSet() will mangle the inputFileSet and potentially remove files
		inputFileSet = copyFileSet;
	}

	protected String selectRecentFileName( List<File> filePaths ) {
		if (filePaths.size() == 1)
			return filePaths.get(0).getName();
		else
			return filePaths.get(0).getParentFile().getName();
	}

	/**
	 * Opens either image sequences of images given file paths. This also allows other information, such as calibration
	 * to be opened and processed. Can only handle images OR sequences and not both.
	 *
	 * @return true if it was successful or false if it failed
	 */
	protected boolean openCustomFiles( String[] filePaths,
									   List<String> outSequence,
									   List<String> outImages ) {
		throw new RuntimeException("Override this function to implement custom file opening");
	}

	/**
	 * Before invoking this function make sure waitingToOpenImage is false AND that the previous input has been stopped
	 */
	protected void openVideo( boolean reopen, String... filePaths ) {
		synchronized (lockStartingProcess) {
			if (startingProcess) {
				System.out.println("Ignoring video request. Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != filePaths.length)
				throw new IllegalArgumentException("inputStreams.size() != filePaths.length. Override openVideo(). " +
						inputStreams.size() + " != " + filePaths.length);
		}
		inputFileSet = filePaths;

		stopAllInputProcessing();

		streamPaused = false;

		boolean failed = false;
		for (int which = 0; which < filePaths.length; which++) {
			CacheSequenceStream cache = inputStreams.get(which);

			SimpleImageSequence sequence = media.openVideo(filePaths[which], cache.getImageType());
			if (sequence == null) {
				failed = true;
				System.out.println("Can't find file. " + filePaths[which]);
				break;
			}
			configureVideo(which, sequence);

			synchronized (inputStreams) {
				cache.reset();
				cache.setSequence(sequence);
			}
		}

		if (!failed) {
			setInputName(new File(filePaths[0]).getName());
			synchronized (inputStreams) {
				inputMethod = InputMethod.VIDEO;
				streamPeriod = 33; // default to 33 FPS for a video
				if (threadProcess != null)
					throw new RuntimeException("There was still an active stream thread!");
				threadProcess = new SynchronizedStreamsThread();
			}
			for (int i = 0; i < inputStreams.size(); i++) {
				CacheSequenceStream stream = inputStreams.get(i);
				// load the first image to get the size then reset
				// so that it starts processing at the first image
				int width = 0, height = 0;
				if (stream.hasNext()) {
					stream.cacheNext();
					width = stream.getWidth();
					height = stream.getHeight();
					stream.reset();
				}
				handleInputChange(i, inputMethod, width, height);
			}
			threadPool.execute(threadProcess);
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

	protected void openImage( boolean reopen, String name, BufferedImage buffered ) {
		synchronized (lockStartingProcess) {
			if (startingProcess) {
				System.out.println("Ignoring image request. Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != 1)
				throw new IllegalArgumentException("Input streams not equal to 1. Override openImage()");
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

			if (threadProcess != null)
				throw new RuntimeException("There was still an active stream thread!");
			threadProcess = new ProcessImageThread();
		}
		if (!reopen) {
			setInputName(name);
			handleInputChange(0, inputMethod, buffered.getWidth(), buffered.getHeight());
		}
		threadPool.execute(threadProcess);
	}

	public void openWebcam() {
		synchronized (lockStartingProcess) {
			if (startingProcess) {
				System.out.println("Ignoring webcam request. Detected spamming");
				return;
			}
			startingProcess = true;
		}

		synchronized (inputStreams) {
			if (inputStreams.size() != 1)
				throw new IllegalArgumentException("Input streams not equal to 1. Override openImage()");
		}

		stopAllInputProcessing();

		// Let he user select and configure the webcam. If canceled it will return null
		OpenWebcamDialog.Selection s = OpenWebcamDialog.showDialog(window);
		if (s == null) {
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
				handleInputChange(0, inputMethod, sequence.getWidth(), sequence.getHeight());
				threadProcess = new SynchronizedStreamsThread();
				threadPool.execute(threadProcess);
			}
		}
	}

	/**
	 * Provides access to an image sequence so that its configuration can be customized
	 */
	protected void configureVideo( int which, SimpleImageSequence sequence ) {

	}

	private void setInputName( String name ) {
		if (window != null) {
			window.setTitle(appName + ":  " + name);
		}
	}

	/**
	 * waits until the processing thread is done.
	 */
	public void waitUntilInputSizeIsKnown() {
		while (!inputSizeKnown) {
			BoofMiscOps.sleep(5);
		}
	}

	/**
	 * Opens a window with this application inside of it
	 *
	 * @param appName Name of the application
	 */
	public void display( String appName ) {
		waitUntilInputSizeIsKnown();
		displayImmediate(appName);
	}

	public void displayImmediate( String appName ) {
		this.appName = appName;
		window = ShowImages.showWindow(this, appName, true);
		window.setJMenuBar(menuBar);
	}

	/**
	 * Displays a dialog box letting the user know it can't perform the requested action
	 */
	private void showRejectDiaglog( String message ) {
		JOptionPane.showMessageDialog(null, message);
	}

	private ActionListener createActionListener() {
		return e -> {
			if (menuItemFile == e.getSource()) {
				openFileMenuBar();
			} else if (menuItemWebcam == e.getSource()) {
				openWebcam();
			} else if (menuItemQuit == e.getSource()) {
				System.exit(0);
			}
		};
	}

	/**
	 * Open file in the menu bar was invoked by the user
	 */
	protected void openFileMenuBar() {
		if (useCustomOpenFiles) {
			throw new RuntimeException("If customFileInput you must overload and provide a custom function");
		}

		List<BoofSwingUtil.FileTypes> types = new ArrayList<>();
		if (allowImages)
			types.add(BoofSwingUtil.FileTypes.IMAGES);
		if (allowVideos)
			types.add(BoofSwingUtil.FileTypes.VIDEOS);
		BoofSwingUtil.FileTypes[] array = types.toArray(new BoofSwingUtil.FileTypes[0]);

		File file = BoofSwingUtil.openFileChooser(DemonstrationBase.this, array);
		if (file != null) {
			openFile(file, true);
		}
	}

	abstract static class ProcessThread implements Runnable {
		volatile boolean requestStop = false;
		volatile boolean running = true;
	}

	class ProcessFileSetThread extends ProcessThread {

		String[] files;

		public ProcessFileSetThread( String[] files ) {
			this.files = files;
		}

		@Override
		public void run() {
			processFiles(files);
			inputSizeKnown = true;
			synchronized (lockStartingProcess) {
				startingProcess = false;
			}

			running = false;
		}
	}

	class ProcessImageSetThread extends ProcessThread {

		String[] files;

		public ProcessImageSetThread( String[] files ) {
			this.files = files;
		}

		@Override
		public void run() {
			ImageBase boof = getImageType(0).createImage(1, 1);

			boolean first = true;
			for (int i = 0; i < files.length && !requestStop; i++) {
				String path = files[i];
				inputFilePath = path;
				if (path == null) {
					System.err.println("Error[" + i + "] path " + files[i]);
					continue;
				}

				BufferedImage buffered = UtilImageIO.loadImage(path);
				if (buffered == null) {
					handleInputFailure(i, "Couldn't open " + path);
					continue;
				}
				if (first) {
					setInputName(new File(files[i]).getName());
				}
				handleInputChange(i, inputMethod, buffered.getWidth(), buffered.getHeight());

				ConvertBufferedImage.convertFrom(buffered, boof, true);

				processImage(i, 0, buffered, boof);

				if (first) {
					first = false;
					inputSizeKnown = true;
					synchronized (lockStartingProcess) {
						startingProcess = false;
					}
				}
			}

			running = false;
		}
	}

	class ProcessImageThread extends ProcessThread {

		@Override
		public void run() {
			try {
				for (int i = 0; i < inputStreams.size(); i++) {
					CacheSequenceStream cache = inputStreams.get(i);
					inputSizeKnown = true;

					ImageBase boof = cache.getBoofImage();
					BufferedImage buff = cache.getBufferedImage();

					processImage(i, 0, buff, boof);
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			}

			// Request spam prevention. Must complete the request before it will accept the new one
			synchronized (lockStartingProcess) {
				startingProcess = false;
			}

			running = false;
		}
	}

	class SynchronizedStreamsThread extends ProcessThread {
		@Override
		public void run() {
			inputSizeKnown = true;

			boolean first = true;

			long before = System.currentTimeMillis();
			while (!requestStop) {
				// see if all the streams have more data available
				boolean allNext = true;
				for (int i = 0; i < inputStreams.size(); i++) {
					if (!inputStreams.get(i).hasNext()) {
						allNext = false;
						break;
					}
				}

				// stop processing if they don't all have data available
				if (!allNext) {
					break;
				}

				// grab images from all the streams and save local copy
				for (int i = 0; i < inputStreams.size(); i++) {
					inputStreams.get(i).cacheNext();
				}

				if (first) { // process at least one image before letting it try to process another source
					first = false;
					synchronized (lockStartingProcess) {
						startingProcess = false;
					}
				}

				// feed images to client - They will own the image data until they are passed new image data
				for (int i = 0; i < inputStreams.size(); i++) {
					CacheSequenceStream cache = inputStreams.get(i);
					int frameID = cache.sequence.getFrameNumber();
					ImageBase boof = cache.getBoofImage();
					BufferedImage buff = cache.getBufferedImage();

					try {
						processImage(i, frameID, buff, boof);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}

				// Throttle speed if requested
				if (streamPeriod > 0) {
					long time = Math.max(0, streamPeriod - (System.currentTimeMillis() - before));
					BoofMiscOps.sleep(time > 0 ? time : 5);
				} else {
					BoofMiscOps.sleep(5);
				}
				before = System.currentTimeMillis();

				if (streamStepCounter > 0) {
					if (--streamStepCounter == 0)
						streamPaused = true;
				}

				// Check to see if paused and wait
				if (streamPaused && inputMethod == InputMethod.VIDEO) {
					enterPausedState();
					while (streamPaused && !requestStop) {
						BoofMiscOps.sleep(5);
					}
				}
			}

			// clean up
			for (int i = 0; i < inputStreams.size(); i++) {
				inputStreams.get(i).sequence.close();
				handleInputClose(i);
			}

			running = false;
		}
	}

	protected void enterPausedState() {}

	/**
	 * If just a single image was processed it will process it again. If it's a stream
	 * there is no need to reprocess, the next image will be handled soon enough.
	 */
	public void reprocessInput() {
		if (useCustomOpenFiles) {
			openFiles(BoofMiscOps.toFileList(inputFileSet), true);
			return;
		}
		if (inputMethod == InputMethod.VIDEO) {
			if (inputFilePath != null)
				openVideo(true, inputFilePath);
			else
				openVideo(true, inputFileSet);
		} else if (inputMethod == InputMethod.IMAGE) {
			Objects.requireNonNull(inputFilePath);
			BufferedImage buff = inputStreams.get(0).getBufferedImage();
			openImage(true, new File(inputFilePath).getName(), buff);// TODO still does a pointless image conversion
		} else if (inputMethod == InputMethod.IMAGE_SET) {
			openImageSet(true, inputFileSet);
		}
	}

	/**
	 * Invokes {@link #reprocessInput()} only if the input is an IMAGE
	 *
	 * TODO expand this to work with paused streams. Right now it re-opens the stream and starts again.
	 */
	public void reprocessImageOnly() {
		if (inputMethod == InputMethod.IMAGE) {
			reprocessInput();
		}
	}

	protected enum InputMethod {
		NONE,
		IMAGE,
		VIDEO,
		WEBCAM,
		IMAGE_SET
	}
}

///*
// * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package boofcv.gui;
//
//import boofcv.io.MediaManager;
//import boofcv.io.PathLabel;
//import boofcv.io.UtilIO;
//import boofcv.io.image.ConvertBufferedImage;
//import boofcv.io.image.SimpleImageSequence;
//import boofcv.io.image.UtilImageIO;
//import boofcv.io.wrapper.DefaultMediaManager;
//import boofcv.struct.image.ImageBase;
//import boofcv.struct.image.ImageType;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.KeyEvent;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Provides some common basic functionality for demonstrations
// *
// * @author Peter Abeles
// */
//public abstract class DemonstrationBase2 extends JPanel {
//	protected JMenuBar menuBar;
//	JMenuItem menuFile, menuWebcam, menuQuit;
//	final JFileChooser fc = new JFileChooser();
//
//	protected InputMethod inputMethod = InputMethod.NONE;
//
//	volatile boolean waitingToOpenImage = false;
//	final Object waitingLock = new Object();
//
//	volatile boolean processRunning = false;
//	volatile boolean processRequested = false;
//
//	final Object processLock = new Object();
//	List<CacheSequenceStream> inputStreams = new ArrayList<>();
//
//	List<ImageSequenceThread> sequenceThreads = new ArrayList<>();
//
//	protected MediaManager media = new DefaultMediaManager();
//
//	// If true then any stream will be paused.  If a webcam is running it will skip new images
//	// if a video it will stop processing the input
//	protected volatile boolean streamPaused = false;
//
//	// minimum elapsed time between the each stream frame being processed, in milliseconds
//	protected volatile long streamPeriod = 30;
//
//	// File path to previously opened image or video.  null if webcam
//	protected String inputFilePath;
//
//	public DemonstrationBase2(boolean openFile , boolean openWebcam, int bufferLength, List<?> exampleInputs, ImageType ...defaultTypes) {
//		super(new BorderLayout());
//		createMenuBar(openFile, openWebcam, exampleInputs);
//
//		setImageTypes(bufferLength, defaultTypes);
//	}
//
//	/**
//	 * Constructor that specifies examples and input image type
//	 *
//	 * @param exampleInputs List of paths to examples.  Either a String file path or {@link PathLabel}.
//	 * @param defaultTypes Type of image in each stream
//	 */
//	public DemonstrationBase2(List<?> exampleInputs, ImageType ...defaultTypes) {
//		this(true,true,5,exampleInputs, defaultTypes);
//	}
//
//	public void setImageTypes( int bufferLength , ImageType ...defaultTypes ) {
//		// TODO add lock to make sure it's not actively streaming when this is invoked
//		for( ImageType type : defaultTypes ) {
//			inputStreams.add( new CacheSequenceStream(bufferLength, type));
//		}
//	}
//
//	private void createMenuBar(boolean openFile , boolean openWebcam , List<?> exampleInputs) {
//		menuBar = new JMenuBar();
//
//		JMenu menu = new JMenu("File");
//		menuBar.add(menu);
//
//		ActionListener listener = createActionListener();
//
//		if( openFile ) {
//			menuFile = new JMenuItem("Open File", KeyEvent.VK_O);
//			menuFile.addActionListener(listener);
//			menuFile.setAccelerator(KeyStroke.getKeyStroke(
//					KeyEvent.VK_O, ActionEvent.CTRL_MASK));
//			menu.add(menuFile);
//		}
//		if( openWebcam ) {
//			menuWebcam = new JMenuItem("Open Webcam", KeyEvent.VK_W);
//			menuWebcam.addActionListener(listener);
//			menuWebcam.setAccelerator(KeyStroke.getKeyStroke(
//					KeyEvent.VK_W, ActionEvent.CTRL_MASK));
//			menu.add(menuWebcam);
//		}
//		menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
//		menuQuit.addActionListener(listener);
//		menuQuit.setAccelerator(KeyStroke.getKeyStroke(
//				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
//
//		menu.addSeparator();
//		menu.add(menuQuit);
//
//		if( exampleInputs != null && exampleInputs.size() > 0 ) {
//			menu = new JMenu("Examples");
//			menuBar.add(menu);
//
//			for (final Object o : exampleInputs) {
//				String name;
//
//				if (o instanceof PathLabel) {
//					name = ((PathLabel) o).getLabel();
//				} else if (o instanceof String) {
//					name = new File((String) o).getName();
//				} else {
//					name = o.toString();
//				}
//				JMenuItem menuItem = new JMenuItem(name);
//				menuItem.addActionListener(new ActionListener() {
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						openExample(o);
//					}
//				});
//				menu.add(menuItem);
//			}
//		}
//
//		add(BorderLayout.NORTH, menuBar);
//	}
//
//	/**
//	 * Function that is invoked when an example has been selected
//	 */
//	public void openExample( Object o ) {
//		String path;
//		if (o instanceof PathLabel) {
//			path = ((PathLabel) o).getPath();
//		} else if (o instanceof String) {
//			path = (String) o;
//		} else {
//			throw new IllegalArgumentException("Unknown example object type.  Please override openExample()");
//		}
//		openFile(new File(path));
//	}
//
//	/**
//	 * Blocks until it kills all input streams from running
//	 */
//	public void blockStopInputStreams() {
//		// TODO implement
//		// TODO see stopPreviousInput
//	}
//
//	/**
//	 * Override to be notified when the input has changed.  This is also a good location to change the default
//	 * max FPS for streaming data.  It will be 0 for webcam and 30 FPS for videos
//	 *
//	 * @param method Type of input source
//	 * @param width Width of input image
//	 * @param height Height of input image
//	 */
//	protected void handleInputChange( int source , InputMethod method , int width , int height ) {
//
//	}
//
//	/**
//	 * Process the image.  Will be called in its own thread, but doesn't need to be re-entrant.  If image
//	 * is null then reprocess the previous image.
//	 */
//	public abstract void processImage(int sourceID, long frameID, final BufferedImage buffered , final ImageBase input  );
//
//	protected void processImageThread(int sourceID, long frameID, final BufferedImage buffered , final ImageBase input ) {
//
//		// See if there is already a thread running that's processing an image.  If so copy
//		// the new image into storage for the new image that is to be processed after it finishes
//		// processing the current image
//		synchronized (processLock) {
//			if(processRunning) {
//				if( buffered != null ) {
//					imageCopy1 = ConvertBufferedImage.checkCopy(buffered, imageCopy1);
//					boofCopy1.setTo(input);
//				} else
//					imageCopy1 = null;
//				processRequested = true;
//				return;
//			} else {
//				processRunning = true;
//				processRequested = false;
//				if( buffered != null )
//					imageCopy0 = ConvertBufferedImage.checkCopy(buffered, imageCopy0);
//				else
//					imageCopy0 = null;
//			}
//		}
//
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					while (true) {
//						synchronized (waitingLock) {
//							waitingToOpenImage = false;
//						}
//						if (imageCopy0 != null)
//							processImage(0,0,imageCopy0, input);
//						else
//							processImage(0,0,null, null);
//
//						synchronized (processLock) {
//							if (!processRequested) {
//								processRunning = false;
//								break;
//							}
//							processRequested = false;
//							if (imageCopy1 != null) {
//								imageCopy0 = ConvertBufferedImage.checkCopy(imageCopy1, imageCopy0);
//								input.setTo(boofCopy1);
//							} else
//								imageCopy0 = null;
//						}
//					}
//				} catch( RuntimeException e ) {
//					e.printStackTrace();
//					System.out.println(e.getMessage());
//					System.out.println("Thread crashed!  If possible, saving image to crashed_image.png");
//					if( imageCopy0 != null )
//						UtilImageIO.saveImage(imageCopy0,"crashed_image.png");
//					synchronized (processLock) {
//						processRunning = false;
//					}
//				}
//			}
//		}.start();
//	}
//
//	private void stopPreviousInput() {
//		switch ( inputMethod ) {
//			case WEBCAM: stopSequenceRunning("Shutting down webcam");break;
//			case VIDEO: stopSequenceRunning("Shutting down video");break;
//		}
//	}
//
//	private void stopSequenceRunning( String message ) {
//		sequenceThread.requestStop = true;
//		WaitingThread waiting = new WaitingThread(message);
//		waiting.start();
//		while( sequenceThread.running ) {
//			Thread.yield();
//		}
//		waiting.closeRequested = true;
//	}
//
//	/**
//	 * Opens a file.  First it will attempt to open it as an image.  If that fails it will try opening it as a
//	 * video.  If all else fails tell the user it has failed.  If a streaming source was running before it will
//	 * be stopped.
//	 */
//	public void openFile(File file) {
//		// maybe it's an example file
//		if( !file.exists() ) {
//			file = new File(UtilIO.pathExample(file.getPath()));
//		}
//		if( !file.exists() ) {
//			System.err.println("Can't find file "+file.getPath());
//			return;
//		}
//
//		synchronized (waitingLock) {
//			if (waitingToOpenImage) {
//				System.out.println("Already opening an image.  Ignoring request");
//				return;
//			}
//			waitingToOpenImage = true;
//		}
//
//		stopPreviousInput();
//
//		String filePath = file.getPath();
//		inputFilePath = filePath;
//		// mjpegs can be opened up as images.  so override the default behavior
//		BufferedImage buffered = filePath.endsWith("mjpeg") ? null : UtilImageIO.loadImage(filePath);
//		if( buffered == null ) {
//			openVideo(filePath);
//		} else {
//			openImage(filePath, buffered);
//		}
//	}
//
//	/**
//	 * Before invoking this function make sure waitingToOpenImage is false AND that the previous input has beens topped
//	 */
//	protected void openVideo(String filePath) {
//		if( inputStreams.size() != 1 )
//			throw new IllegalArgumentException("Input streams not equal to 1.  Override openVideo()");
//
//		CacheSequenceStream cache = inputStreams.get(0);
//		SimpleImageSequence sequence = media.openVideo(filePath, cache.getImageType());
//
//		if (sequence != null) {
//			synchronized (sequenceThreads) {
//				inputMethod = InputMethod.VIDEO;
//				streamPeriod = 33; // default to 33 FPS for a video
//				ImageSequenceThread thread = new ImageSequenceThread(cache);
//				thread.start();
//				sequenceThreads.add( thread);
//			}
//		} else {
//			inputMethod = InputMethod.NONE;
//			waitingToOpenImage = false;
//			SwingUtilities.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					JOptionPane.showMessageDialog(DemonstrationBase2.this, "Can't open file");
//				}
//			});
//		}
//	}
//
//	protected void openImage(String filePath , BufferedImage buffered ) {
//		if( inputStreams.size() != 1 )
//			throw new IllegalArgumentException("Input streams not equal to 1.  Override openImage()");
//
//		inputMethod = InputMethod.IMAGE;
//		input.reshape(buffered.getWidth(),buffered.getHeight());
//		inputBuffered = buffered;
//		ConvertBufferedImage.convertFrom(buffered,input,true);
//		handleInputChange(inputMethod,buffered.getWidth(),buffered.getHeight());
//		processImageThread(0,0,buffered, input);
//	}
//
//	/**
//	 * waits until the processing thread is done.
//	 */
//	public void waitUntilDoneProcessing() {
//		while( processRunning ) {
//			Thread.yield();
//		}
//	}
//
//	public void openWebcam() {
//		if( !allowWebcameras ) {
//			showRejectDiaglog("Can't process webcams");
//			return;
//		}
//
//		synchronized (waitingLock) {
//			if (waitingToOpenImage)
//				return;
//			waitingToOpenImage = true;
//		}
//
//		stopPreviousInput();
//
//		inputFilePath = null;
//		inputMethod = InputMethod.WEBCAM;
//		streamPeriod = 0; // default to no delay in processing for a real time stream
//
//		new WaitingThread("Opening Webcam").start();
//		new Thread() {
//			public void run() {
//				SimpleImageSequence<T> sequence = media.openCamera(null,640,480, defaultType);
//				if(sequence != null) {
//					sequenceThread = new ImageSequenceThread(sequence);
//					sequenceThread.start();
//				} else {
//					SwingUtilities.invokeLater(new Runnable() {
//						@Override
//						public void run() {
//							JOptionPane.showMessageDialog(DemonstrationBase2.this, "Failed to open webcam");
//						}
//					});
//				}
//			}
//		}.start();
//	}
//
//	/**
//	 * Displays a dialog box letting the user know it can't perform the requested action
//	 */
//	private void showRejectDiaglog( String message ) {
//		JOptionPane.showMessageDialog(null, message);
//	}
//
//	private ActionListener createActionListener() {
//		return new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (menuFile == e.getSource()) {
//					int returnVal = fc.showOpenDialog(DemonstrationBase2.this);
//
//					if (returnVal == JFileChooser.APPROVE_OPTION) {
//						File file = fc.getSelectedFile();
//						openFile(file);
//					} else {
//					}
//				} else if (menuWebcam == e.getSource()) {
//					openWebcam();
//				} else if (menuQuit == e.getSource()) {
//					System.exit(0);
//				}
//			}
//		};
//	}
//
//	class WaitingThread extends Thread {
//		ProgressMonitor progress;
//		public volatile boolean closeRequested = false;
//		public WaitingThread( String message ) {
//			progress = new ProgressMonitor(DemonstrationBase2.this,message,"", 0, 100);
//		}
//
//		@Override
//		public void run() {
//			while( waitingToOpenImage && !closeRequested ) {
//				SwingUtilities.invokeLater(new Runnable() { @Override public void run() { progress.setProgress(0); } });
//				try {Thread.sleep(250);} catch (InterruptedException ignore) {}
//			}
//			SwingUtilities.invokeLater(
//					new Runnable() { @Override public void run() { progress.close(); } }
//			);
//		}
//	}
//
//	class SynchronizedStreamsThread extends Thread {
//		boolean requestStop = false;
//		boolean running = true;
//
//		@Override
//		public void run() {
//			for (int i = 0; i < inputStreams.size() ; i++) {
//				CacheSequenceStream sequence = inputStreams.get(i);
//				handleInputChange(i, inputMethod, sequence.getWidth(), sequence.getHeight());
//			}
//
//			long before = System.currentTimeMillis();
//			while( !requestStop ) {
//				// see if all the streams have more data available
//				boolean allNext = true;
//				for (int i = 0; i < inputStreams.size() ; i++) {
//					if( !inputStreams.get(i).hasNext() ) {
//						allNext = false;
//						break;
//					}
//				}
//
//				// stop processing if they don't all have data available
//				if( !allNext ) {
//					break;
//				}
//
//				// TODO grab images from all the streams and save local copy
//
//				// TODO feed images to client - They will own the image data until they are passed new image data
//
//				// TODO throttle speed if requested
//
//				// TODO check to see if paused and wait
//
//
//			}
//
//			// clean up
//			for (int i = 0; i < inputStreams.size() ; i++) {
//				inputStreams.get(i).sequence.close();
//			}
//
//		}
//	}
//
//	class ImageSequenceThread extends Thread {
//
//		boolean requestStop = false;
//		boolean running = true;
//
//		CacheSequenceStream sequence;
//
//		int source;
//
//		public ImageSequenceThread(int source , CacheSequenceStream sequence) {
//			this.sequence = sequence;
//			this.source = source;
//		}
//
//		@Override
//		public void run() {
//			handleInputChange(source, inputMethod, sequence.getWidth(), getHeight());
//
//			long before = System.currentTimeMillis();
//			while( !requestStop && sequence.hasNext() ) {
//				boolean skipFrame = false;
//				if( sequence.isCacheFull() ) {
//					skipFrame = true;
//					// If the cache is full and it's a live webcam skip the frame
//					if( inputMethod == InputMethod.WEBCAM ) {
//						sequence.discardNext();
//					}
//					// otherwise just wait until it's done processing
//				} else if( streamPeriod > 0 ) {
//
//				}
//
//				if( skipFrame ) {
//
//				}
//				T input = sequence.next();
//
//				// if it's a webcam and paused, just don't process the video frame
//				boolean skipFrame = streamPaused && inputMethod == InputMethod.WEBCAM;
//
//				if( input == null ) {
//					break;
//				} else if( skipFrame ) {
//					// do nothing just don't process it
//					before = System.currentTimeMillis();
//				} else {
//					BufferedImage buffered = sequence.getGuiImage();
//					processImageThread(0,0,buffered,input);
//					if( streamPeriod > 0 ) {
//						long time = Math.max(0, streamPeriod -(System.currentTimeMillis()-before));
//						if( time > 0 ) {
//							try {Thread.sleep(time);} catch (InterruptedException ignore) {}
//						} else {
//							try {Thread.sleep(5);} catch (InterruptedException ignore) {}
//						}
//					} else {
//						try {Thread.sleep(5);} catch (InterruptedException ignore) {}
//					}
//					before = System.currentTimeMillis();
//				}
//
//				// If paused and is a video, do thing until unpaused or a stop is requested
//				if( streamPaused && inputMethod == InputMethod.VIDEO ) {
//					while( streamPaused && !requestStop ) {
//						try {Thread.sleep(5);} catch (InterruptedException ignore) {}
//					}
//				}
//			}
//			sequence.close();
//			running = false;
//		}
//	}
//
//	/**
//	 * If just a single image was processed it will process it again.  If it's a stream
//	 * there is no need to reprocess, the next image will be handled soon enough.
//	 */
//	public void reprocessSingleImage() {
//		if( sequenceThread == null ) {
//			// hmm if it's reprocessing the last image in a sequence this might not work
//			processImageThread(0,0, inputBuffered, input);
//		}
//	}
//
//	/**
//	 * If the current input source is a video it will reload it from the start
//	 */
//	public void replayVideo() {
//		if( inputMethod == InputMethod.VIDEO ) {
//			synchronized (waitingLock) {
//				if (waitingToOpenImage) {
//					System.out.println("Waiting to open an image");
//					return;
//				}
//				waitingToOpenImage = true;
//			}
//
//			stopPreviousInput();
//			openVideo(inputFilePath);
//		}
//	}
//
//	protected enum InputMethod {
//		NONE,
//		IMAGE,
//		VIDEO,
//		WEBCAM
//	}
//}

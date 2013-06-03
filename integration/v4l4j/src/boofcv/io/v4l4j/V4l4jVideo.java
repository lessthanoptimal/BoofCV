/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.io.v4l4j;

import au.edu.jcu.v4l4j.*;
import au.edu.jcu.v4l4j.exceptions.StateException;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.VideoCallBack;
import boofcv.io.VideoController;
import boofcv.struct.image.ImageBase;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;

/**
 * Easy to use wrapper around V4L4J for processing videos.
 *
 * @author Peter Abeles
 */
public class V4l4jVideo<T extends ImageBase> extends WindowAdapter
		implements CaptureCallback, VideoController<T>
{
	private static int std = V4L4JConstants.STANDARD_WEBCAM, channel = 0;

	VideoCallBack<T> callback;

	private VideoDevice     videoDevice;
	private FrameGrabber frameGrabber;
	private T imageBoof;


	{
		System.setProperty( "java.library.path", "/home/pja/projects/boofcv/integration/v4l4j/v4l4j-0.9.0" );

		Field fieldSysPath = null;
		try {
			fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		System.load("/home/pja/projects/boofcv/integration/v4l4j/v4l4j-0.9.0/libvideo.so");
	}


	public static void main(String args[]) throws NoSuchFieldException, IllegalAccessException {

		System.setProperty( "java.library.path", "/home/pja/iai/kinesys/boofcv/lib/v4l4j-0.9.0" );

		Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
		fieldSysPath.setAccessible( true );
		fieldSysPath.set( null, null );

		System.load("/home/pja/iai/kinesys/boofcv/lib/v4l4j-0.9.0/libvideo.so");

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new V4l4jVideo();
			}
		});
	}

	/**
	 * Builds a WebcamViewer object
	 * @throws V4L4JException if any parameter if invalid
	 */
	public V4l4jVideo(){

	}

	@Override
	public boolean start( String device , int width, int height, VideoCallBack<T> callback) {
		this.callback = callback;

		// Initialise video device and frame grabber
		try {
			initFrameGrabber(device,width,height);
		} catch (V4L4JException e1) {
			System.err.println("Error setting up capture");
			e1.printStackTrace();

			// cleanup and exit
			cleanupCapture();
			return false;
		}

		// start capture
		try {
			frameGrabber.startCapture();
			return true;
		} catch (V4L4JException e){
			System.err.println("Error starting the capture");
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Initialises the FrameGrabber object
	 * @throws V4L4JException if any parameter if invalid
	 */
	private void initFrameGrabber( String device, int width , int height ) throws V4L4JException {
		videoDevice = new VideoDevice(device);
		frameGrabber = videoDevice.getJPEGFrameGrabber(width, height, channel, std, 80);
		frameGrabber.setCaptureCallback(this);
		width = frameGrabber.getWidth();
		height = frameGrabber.getHeight();

		// declare storage and initialize the callback
		imageBoof = callback.getImageDataType().createImage(width,height);
		callback.init(width, height);
	}

	/**
	 * this method stops the capture and releases the frame grabber and video device
	 */
	private void cleanupCapture() {
		try {
			frameGrabber.stopCapture();
		} catch (StateException ex) {
			// the frame grabber may be already stopped, so we just ignore
			// any exception and simply continue.
		}

		// release the frame grabber and video device
		videoDevice.releaseFrameGrabber();
		videoDevice.release();

		callback.stopped();
	}

	/**
	 * Catch window closing event so we can free up resources before exiting
	 * @param e
	 */
	public void windowClosing(WindowEvent e) {
		cleanupCapture();
	}


	@Override
	public void exceptionReceived(V4L4JException e) {
		// This method is called by v4l4j if an exception
		// occurs while waiting for a new frame to be ready.
		// The exception is available through e.getCause()
		e.printStackTrace();
	}

	@Override
	public void nextFrame(VideoFrame frame) {
		// This method is called when a new frame is ready.
		// Don't forget to recycle it when done dealing with the frame.

		ConvertBufferedImage.convertFrom(frame.getBufferedImage(),imageBoof);
		callback.nextFrame(imageBoof,frame.getBufferedImage(),frame.getCaptureTime());

		// recycle the frame
		frame.recycle();

		if( callback.stopRequested() ) {
			cleanupCapture();
		}
	}
}

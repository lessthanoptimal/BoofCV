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

package boofcv.openkinect;

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.openkinect.freenect.*;

import java.nio.ByteBuffer;

/**
 * High-level interface for streaming RGB + depth images from a Kinect.  Images are "synchronized" by passing in the
 * two most recent images.  The class has been designed such that the listener can spend as much time inside the listen
 * function without interrupting the streaming process since it's run in a separate thread.
 *
 * @author Peter Abeles
 */
public class StreamOpenKinectRgbDepth {

	// time out used in some places
	private long timeout=10000;

	// time stamps for raw data
	private volatile long timeDepthData;
	private volatile long timeRgbData;

	// byte buffer for depth image.  stored in this format until
	private byte[] dataDepth;
	private byte[] dataRgb;

	private Listener listener;

	// image with depth information
	private GrayU16 depth = new GrayU16(1,1);
	// image with color information
	private Planar<GrayU8> rgb = new Planar<>(GrayU8.class,1,1,3);

	// thread which synchronized video streams
	private CombineThread thread;
	// the Kinect device being used
	private Device device;

	/**
	 * Adds listeners to the device and sets its resolutions.
	 * @param device Kinect device
	 * @param resolution Resolution that images are being processed at.  Must be medium for now
	 * @param listener Listener for data
	 */
	public void start( Device device , Resolution resolution , Listener listener )
	{
		if( resolution != Resolution.MEDIUM ) {
			throw new IllegalArgumentException("Depth image is always at medium resolution.  Possible bug in kinect driver");
		}

		this.device = device;
		this.listener = listener;

		// Configure the kinect
		device.setDepthFormat(DepthFormat.REGISTERED,resolution);
		device.setVideoFormat(VideoFormat.RGB, resolution);

		// declare data structures
		int w = UtilOpenKinect.getWidth(resolution);
		int h = UtilOpenKinect.getHeight(resolution);

		dataDepth = new byte[ w*h*2 ];
		dataRgb = new byte[ w*h*3 ];
		depth.reshape(w,h);
		rgb.reshape(w,h);

		thread = new CombineThread();
		thread.start();
		// make sure the thread is running before moving on
		while(!thread.running)
			Thread.yield();

		// start the streaming
		device.startDepth(new DepthHandler() {
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				processDepth(frame,timestamp);
			}
		});

		device.startVideo(new VideoHandler() {
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				processRgb(frame,timestamp);
			}
		});
	}

	/**
	 * Stops all the threads from running and closes the video channels and video device
	 */
	public void stop() {
		thread.requestStop = true;
		long start = System.currentTimeMillis()+timeout;
		while( start > System.currentTimeMillis() && thread.running )
			Thread.yield();

		device.stopDepth();
		device.stopVideo();
		device.close();
	}

	protected void processDepth( ByteBuffer frame, int timestamp ) {
		synchronized ( dataDepth ) {
			for( int i = 0; i < dataDepth.length; i++ ) {
				dataDepth[i] = frame.get(i);
			}
			timeDepthData = timestamp & 0xFFFFFFFFFFL;
		}
	}

	protected void processRgb(ByteBuffer frame, int timestamp ) {

		synchronized ( dataRgb ) {
			for( int i = 0; i < dataRgb.length; i++ ) {
				dataRgb[i] = frame.get(i);
			}
			timeRgbData = timestamp & 0xFFFFFFFFFFL;
		}
		thread.interrupt();
	}

	private class CombineThread extends Thread {

		public volatile boolean running = false;
		public volatile boolean requestStop = false;

		@Override
		public void run() {
			running = true;
			long previousTimeStamp = 0;

			while( !requestStop ) {
				synchronized ( this ) {
					try {
						wait(200);
					} catch (InterruptedException ignore) {}
				}
				// don't process the same thread twice
				if( previousTimeStamp == timeRgbData )
					continue;

				// Convert the two most recent images
				long timeDepth,timeRgb;
				synchronized ( dataDepth ) {
					timeDepth = timeDepthData;
					UtilOpenKinect.bufferDepthToU16(dataDepth,depth);
				}
				synchronized ( dataRgb ) {
					previousTimeStamp = timeRgbData;
					timeRgb = timeRgbData;
					UtilOpenKinect.bufferRgbToMsU8(dataRgb,rgb);
				}

				listener.processKinect(rgb, depth, timeRgb, timeDepth);
			}

			running = false;
		}
	}

	/**
	 * Listener for kinect data
	 */
	public interface Listener {
		/**
		 * Function for processing synchronized kinect data. The two most recent depth and rgb images are passed along
		 * with their time stamps.  The user can spend as much time inside this function without screwing up the
		 * video feeds.  Just make sure you exit it before calling stop.
		 * @param rgb Color image
		 * @param depth Depth image
		 * @param timeRgb Time-stamp for rgb image
		 * @param timeDepth Time-stamp for depth image
		 */
		public void processKinect(Planar<GrayU8> rgb, GrayU16 depth, long timeRgb, long timeDepth);
	}
}

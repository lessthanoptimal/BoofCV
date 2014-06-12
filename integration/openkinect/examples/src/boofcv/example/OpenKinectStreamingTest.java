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

package boofcv.examples;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import com.sun.jna.NativeLibrary;
import org.openkinect.freenect.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Example demonstrating how to process and display data from the Kinect.
 *
 * @author Peter Abeles
 */
public class OpenKinectStreamingTest {

	{
		// Modify this link to be where you store your shared library
		NativeLibrary.addSearchPath("freenect", "/home/pja/libfreenect/build/lib");
	}

	MultiSpectral<ImageUInt8> rgb = new MultiSpectral<ImageUInt8>(ImageUInt8.class,1,1,3);
	ImageUInt16 depth = new ImageUInt16(1,1);

	BufferedImage outRgb;
	ImagePanel guiRgb;

	BufferedImage outDepth;
	ImagePanel guiDepth;

	public void process() {
		Context kinect = Freenect.createContext();

		if( kinect.numDevices() < 0 )
			throw new RuntimeException("No kinect found!");

		Device device = kinect.openDevice(0);

		device.setDepthFormat(DepthFormat.REGISTERED);
		device.setVideoFormat(VideoFormat.RGB);

		device.startDepth(new DepthHandler() {
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				processDepth(mode,frame,timestamp);
			}
		});
		device.startVideo(new VideoHandler() {
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				processRgb(mode,frame,timestamp);
			}
		});

		long starTime = System.currentTimeMillis();
		while( starTime+100000 > System.currentTimeMillis() ) {}
		System.out.println("100 Seconds elapsed");

		device.stopDepth();
		device.stopVideo();
		device.close();

	}

	protected void processDepth( FrameMode mode, ByteBuffer frame, int timestamp ) {
		System.out.println("Got depth! "+timestamp);

		if( outDepth == null ) {
			depth.reshape(mode.getWidth(),mode.getHeight());
			outDepth = new BufferedImage(depth.width,depth.height,BufferedImage.TYPE_INT_RGB);
			guiDepth = ShowImages.showWindow(outDepth,"Depth Image");
		}

		UtilOpenKinect.bufferDepthToU16(frame, depth);

		VisualizeImageData.grayUnsigned(depth,outDepth,1000);
		guiDepth.repaint();
	}

	protected void processRgb( FrameMode mode, ByteBuffer frame, int timestamp ) {
		if( mode.getVideoFormat() != VideoFormat.RGB ) {
			System.out.println("Bad rgb format!");
		}

		System.out.println("Got rgb! "+timestamp);

		if( outRgb == null ) {
			rgb.reshape(mode.getWidth(),mode.getHeight());
			outRgb = new BufferedImage(rgb.width,rgb.height,BufferedImage.TYPE_INT_RGB);
			guiRgb = ShowImages.showWindow(outRgb,"RGB Image");
		}

		UtilOpenKinect.bufferRgbToMsU8(frame, rgb);
		ConvertBufferedImage.convertTo_U8(rgb,outRgb,true);

		guiRgb.repaint();
	}

	public static void main( String args[] ) {
		OpenKinectStreamingTest app = new OpenKinectStreamingTest();

		app.process();
	}
}

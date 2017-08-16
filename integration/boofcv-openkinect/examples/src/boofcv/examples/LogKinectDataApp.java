/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.openkinect.StreamOpenKinectRgbDepth;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import com.sun.jna.NativeLibrary;
import org.ddogleg.struct.GrowQueue_I8;
import org.openkinect.freenect.Context;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.Resolution;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class LogKinectDataApp implements StreamOpenKinectRgbDepth.Listener {
	{
		// be sure to set OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY to the location of your shared library!
		NativeLibrary.addSearchPath("freenect", OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY);
	}

	int maxImages;
	boolean showImage;
	Resolution resolution = Resolution.MEDIUM;

	BufferedImage buffRgb;
	int frameNumber;

	DataOutputStream logFile;

	GrowQueue_I8 buffer = new GrowQueue_I8(1);

	ImagePanel gui;

	public LogKinectDataApp(int maxImages, boolean showImage) {
		this.maxImages = maxImages;
		this.showImage = showImage;
	}

	public void process() throws IOException {

		logFile = new DataOutputStream(new FileOutputStream("log/timestamps.txt"));
		logFile.write("# Time stamps for rgb and depth cameras.\n".getBytes());

		int w = UtilOpenKinect.getWidth(resolution);
		int h = UtilOpenKinect.getHeight(resolution);

		buffRgb = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);

		if( showImage ) {
			gui = ShowImages.showWindow(buffRgb,"Kinect RGB");
		}

		StreamOpenKinectRgbDepth stream = new StreamOpenKinectRgbDepth();
		Context kinect = Freenect.createContext();

		if( kinect.numDevices() < 0 )
			throw new RuntimeException("No kinect found!");

		Device device = kinect.openDevice(0);

		stream.start(device,resolution,this);

		if( maxImages > 0 ) {
			while( frameNumber < maxImages ) {
				System.out.printf("Total saved %d\n",frameNumber);
				BoofMiscOps.pause(100);
			}
			stream.stop();
			System.out.println("Exceeded max images");
			System.exit(0);
		}
	}

	@Override
	public void processKinect(Planar<GrayU8> rgb, GrayU16 depth, long timeRgb, long timeDepth) {
		System.out.println(frameNumber+"  "+timeRgb);
		try {
			logFile.write(String.format("%10d %d %d\n",frameNumber,timeRgb,timeDepth).getBytes());
			logFile.flush();
			UtilImageIO.savePPM(rgb, String.format("log/rgb%07d.ppm", frameNumber), buffer);
			UtilOpenKinect.saveDepth(depth, String.format("log/depth%07d.depth", frameNumber), buffer);
			frameNumber++;

			if( showImage ) {
				ConvertBufferedImage.convertTo_U8(rgb,buffRgb,true);
				gui.repaint();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) throws IOException {
		LogKinectDataApp app = new LogKinectDataApp(1000000,false);
		app.process();
	}
}

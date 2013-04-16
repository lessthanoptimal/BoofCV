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

package boofcv.example;

import boofcv.openkinect.StreamOpenKinectRgbDepth;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import com.sun.jna.NativeLibrary;
import org.openkinect.freenect.Context;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.Resolution;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class LogKinectDataApp implements StreamOpenKinectRgbDepth.Listener {
	{
		// Modify this link to be where you store your shared library
		if( UtilOpenKinect.PATH_TO_SHARED_LIBRARY != null )
			NativeLibrary.addSearchPath("freenect", UtilOpenKinect.PATH_TO_SHARED_LIBRARY);
	}

	Resolution resolution = Resolution.MEDIUM;

	BufferedImage buffRgb;
	int frameNumber;

	DataOutputStream logFile;

	byte buffer[];

	public void process() throws IOException {

		logFile = new DataOutputStream(new FileOutputStream("log/timestamps.txt"));
		logFile.write("# Time stamps for rgb and depth cameras.".getBytes());

		int w = UtilOpenKinect.getWidth(resolution);
		int h = UtilOpenKinect.getHeight(resolution);

		buffRgb = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);

		buffer = new byte[w*h*3];

		StreamOpenKinectRgbDepth stream = new StreamOpenKinectRgbDepth();
		Context kinect = Freenect.createContext();

		if( kinect.numDevices() < 0 )
			throw new RuntimeException("No kinect found!");

		Device device = kinect.openDevice(0);

		stream.start(device,resolution,this);
	}

	public void savePPM( MultiSpectral<ImageUInt8> rgb ) throws IOException {
		File out = new File(String.format("log/rgb%07d.ppm",frameNumber));
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("P6\n%d %d\n255\n", rgb.width, rgb.height);
		os.write(header.getBytes());

		ImageUInt8 band0 = rgb.getBand(0);
		ImageUInt8 band1 = rgb.getBand(1);
		ImageUInt8 band2 = rgb.getBand(2);

		int indexOut = 0;
		for( int y = 0; y < rgb.height; y++ ) {
			int index = rgb.startIndex + y*rgb.stride;
			for( int x = 0; x < rgb.width; x++ , index++) {
				buffer[indexOut++] = band0.data[index];
				buffer[indexOut++] = band1.data[index];
				buffer[indexOut++] = band2.data[index];
			}
		}

		os.write(buffer,0,rgb.width*rgb.height*3);

		os.close();
	}

	public void saveDepth( ImageUInt16 depth ) throws IOException {
		File out = new File(String.format("log/depth%07d.depth",frameNumber));
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("%d %d\n", depth.width, depth.height);
		os.write(header.getBytes());

		int indexOut = 0;
		for( int y = 0; y < depth.height; y++ ) {
			int index = depth.startIndex + y*depth.stride;
			for( int x = 0; x < depth.width; x++ , index++) {
				int pixel = depth.data[index];
				buffer[indexOut++] = (byte)(pixel&0xFF);
				buffer[indexOut++] = (byte)((pixel>>8) & 0xFF);
			}
		}
		os.write(buffer,0,depth.width*depth.height*2);
		os.close();
	}

	@Override
	public void processKinect(MultiSpectral<ImageUInt8> rgb, ImageUInt16 depth, long timeRgb, long timeDepth) {
		System.out.println(frameNumber+"  "+timeRgb);
		try {
			logFile.write(String.format("%10d %d %d\n",frameNumber,timeRgb,timeDepth).getBytes());
			logFile.flush();
			savePPM(rgb);
			saveDepth(depth);
			frameNumber++;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) throws IOException {
		LogKinectDataApp app = new LogKinectDataApp();
		app.process();
	}
}

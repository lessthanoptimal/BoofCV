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

import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.misc.BoofMiscOps;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class PlaybackKinectLogApp {
	ImageGridPanel gui;

	String directory;

	byte[] data = new byte[0];

	// image with depth information
	private ImageUInt16 depth = new ImageUInt16(1,1);
	// image with color information
	private MultiSpectral<ImageUInt8> rgb = new MultiSpectral<ImageUInt8>(ImageUInt8.class,1,1,3);

	BufferedImage outRgb;
	BufferedImage outDepth;

	public PlaybackKinectLogApp(String directory) {
		this.directory = directory;
	}

	public void process() throws IOException {
		parseFrame(0);

		outRgb = new BufferedImage(rgb.getWidth(),rgb.getHeight(),BufferedImage.TYPE_INT_RGB);
		outDepth = new BufferedImage(depth.getWidth(),depth.getHeight(),BufferedImage.TYPE_INT_RGB);

		gui = new ImageGridPanel(1,2,outRgb,outDepth);
		ShowImages.showWindow(gui,"Kinect Data");

		int frame = 1;
		while( true ) {
			parseFrame(frame++);
			ConvertBufferedImage.convertTo_U8(rgb,outRgb);
			VisualizeImageData.disparity(depth, outDepth, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE, 0);
			gui.repaint();
			BoofMiscOps.pause(30);
		}
	}

	private void parseFrame(int frameNumber ) throws IOException {
		parsePPM(String.format("%s/rgb%07d.ppm",directory,frameNumber));
		parseDepth(String.format("%s/depth%07d.depth", directory, frameNumber));
	}


	private void parsePPM( String fileName ) throws IOException {
		DataInputStream in = new DataInputStream(new FileInputStream(fileName));

		readLine(in);
		String s[] = readLine(in).split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);
		readLine(in);

		int length = w*h*3;
		if( data.length < length )
			data = new byte[length];
		in.read(data,0,length);


		rgb.reshape(w,h);
		UtilOpenKinect.bufferRgbToMsU8(data,rgb);
	}

	private void parseDepth( String fileName ) throws IOException {
		DataInputStream in = new DataInputStream(new FileInputStream(fileName));

		String s[] = readLine(in).split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);

		int length = w*h*2;
		if( data.length < length )
			data = new byte[length];
		in.read(data,0,length);


		depth.reshape(w,h);
		UtilOpenKinect.bufferDepthToU16(data, depth);
	}

	private String readLine( DataInputStream in ) throws IOException {
		String s = "";
		while( true ) {
			int b = in.read();

			if( b == '\n' )
				return s;
			else
				s += (char)b;
		}
	}

	public static void main( String args[] ) throws IOException {
		PlaybackKinectLogApp app = new PlaybackKinectLogApp("log");

		app.process();
	}
}

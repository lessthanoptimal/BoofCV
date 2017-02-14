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

import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.ddogleg.struct.GrowQueue_I8;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class PlaybackKinectLogApp {
	ImageGridPanel gui;

	String directory;

	GrowQueue_I8 data = new GrowQueue_I8();
	boolean depthIsPng = false;

	// image with depth information
	private GrayU16 depth = new GrayU16(1,1);
	// image with color information
	private Planar<GrayU8> rgb = new Planar<>(GrayU8.class,1,1,3);

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
			ConvertBufferedImage.convertTo_U8(rgb,outRgb,true);
			VisualizeImageData.disparity(depth, outDepth, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE, 0);
			gui.repaint();
			BoofMiscOps.pause(30);
		}
	}

	private void parseFrame(int frameNumber ) throws IOException {
		UtilImageIO.loadPPM_U8(String.format("%s/rgb%07d.ppm", directory, frameNumber), rgb, data);
		if( depthIsPng ) {
			BufferedImage image = UtilImageIO.loadImage(String.format("%s/depth%07d.png", directory, frameNumber));
			ConvertBufferedImage.convertFrom(image,depth,true);
		} else {
			UtilOpenKinect.parseDepth(String.format("%s/depth%07d.depth", directory, frameNumber), depth, data);
		}

	}

	public static void main( String args[] ) throws IOException {
		PlaybackKinectLogApp app = new PlaybackKinectLogApp("log");

		app.process();
	}
}

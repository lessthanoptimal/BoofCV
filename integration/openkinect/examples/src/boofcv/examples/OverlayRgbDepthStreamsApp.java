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
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.openkinect.StreamOpenKinectRgbDepth;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import com.sun.jna.NativeLibrary;
import org.openkinect.freenect.Context;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.Resolution;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class OverlayRgbDepthStreamsApp implements StreamOpenKinectRgbDepth.Listener {
	{
		// be sure to set OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY to the location of your shared library!
		NativeLibrary.addSearchPath("freenect", OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY);
	}

	Resolution resolution = Resolution.MEDIUM;

	BufferedImage buffRgb;
	BufferedImage buffDepth;

	ImagePanel gui;

	public void process() {

		int w = UtilOpenKinect.getWidth(resolution);
		int h = UtilOpenKinect.getHeight(resolution);

		buffRgb = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		buffDepth = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);

		gui = ShowImages.showWindow(buffRgb,"Kinect Overlay");

		StreamOpenKinectRgbDepth stream = new StreamOpenKinectRgbDepth();
		Context kinect = Freenect.createContext();

		if( kinect.numDevices() < 0 )
			throw new RuntimeException("No kinect found!");

		Device device = kinect.openDevice(0);
		stream.start(device,resolution,this);
	}

	@Override
	public void processKinect(Planar<GrayU8> rgb, GrayU16 depth, long timeRgb, long timeDepth) {
		VisualizeImageData.disparity(depth, buffDepth, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE,0);
		ConvertBufferedImage.convertTo_U8(rgb,buffRgb,true);

		Graphics2D g2 = buffRgb.createGraphics();
		float alpha = 0.5f;
		int type = AlphaComposite.SRC_OVER;
		AlphaComposite composite =
				AlphaComposite.getInstance(type, alpha);
		g2.setComposite(composite);
		g2.drawImage(buffDepth,0,0,null);

		gui.repaint();
	}

	public static void main( String args[] ) {
		OverlayRgbDepthStreamsApp app = new OverlayRgbDepthStreamsApp();
		app.process();
	}
}

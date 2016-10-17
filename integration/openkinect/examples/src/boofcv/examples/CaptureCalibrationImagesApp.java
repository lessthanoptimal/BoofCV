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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Displays RGB image from the kinect and then pauses after a set period of time.  At which point the user can press
 * 'y' or 'n' to indicate yes or no for saving the RGB and depth images.  Useful when collecting calibration images
 *
 * @author Peter Abeles
 */
public class CaptureCalibrationImagesApp implements KeyListener, StreamOpenKinectRgbDepth.Listener {
	{
		// be sure to set OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY to the location of your shared library!
		NativeLibrary.addSearchPath("freenect", OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY);
	}

	String directory;
	int period;
	Resolution resolution = Resolution.MEDIUM;

	BufferedImage buffRgb;
	int frameNumber;

	GrowQueue_I8 buffer = new GrowQueue_I8(1);

	ImagePanel gui;

	Planar<GrayU8> savedRgb;
	GrayU16 savedDepth;

	volatile boolean updateDisplay;
	volatile boolean savedImages;
	volatile int userChoice;

	String text;
	long timeText;

	public CaptureCalibrationImagesApp(int period, String directory) {
		this.period = period;
		this.directory = directory;
	}

	public void process() throws IOException {

		// make sure there is a "log" directory
		new File("log").mkdir();

		int w = UtilOpenKinect.getWidth(resolution);
		int h = UtilOpenKinect.getHeight(resolution);

		buffRgb = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);

		savedRgb = new Planar<>(GrayU8.class,w,h,3);
		savedDepth = new GrayU16(w,h);

		gui = ShowImages.showWindow(buffRgb, "Kinect RGB");
		gui.addKeyListener(this);
		gui.requestFocus();

		StreamOpenKinectRgbDepth stream = new StreamOpenKinectRgbDepth();
		Context kinect = Freenect.createContext();

		if( kinect.numDevices() < 0 )
			throw new RuntimeException("No kinect found!");

		Device device = kinect.openDevice(0);

		stream.start(device,resolution,this);

		long targetTime = System.currentTimeMillis() + period;
		updateDisplay = true;
		while( true ) {
			BoofMiscOps.pause(100);

			if( targetTime < System.currentTimeMillis() ) {
				userChoice = -1;
				savedImages = false;
				updateDisplay = false;
				while( true ) {
					if( savedImages && userChoice != -1 ) {
						if( userChoice == 1 ) {
							UtilImageIO.savePPM(savedRgb, String.format(directory + "rgb%07d.ppm", frameNumber), buffer);
							UtilOpenKinect.saveDepth(savedDepth, String.format(directory + "depth%07d.depth", frameNumber), buffer);
							frameNumber++;
							text = "Image Saved!";
						} else {
							text = "Image Discarded!";
						}
						timeText = System.currentTimeMillis()+500;
						updateDisplay = true;
						targetTime = System.currentTimeMillis()+period;
						break;
					}
					BoofMiscOps.pause(50);
				}
			}
		}
	}

	@Override
	public void processKinect(Planar<GrayU8> rgb, GrayU16 depth, long timeRgb, long timeDepth) {

		if( updateDisplay ) {
			ConvertBufferedImage.convertTo_U8(rgb,buffRgb,true);

			if( timeText >= System.currentTimeMillis() ) {
				Graphics2D g2 = buffRgb.createGraphics();
				g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
				g2.setColor(Color.RED);
				g2.drawString(text,rgb.width/2-100,rgb.height/2);
			}

			gui.repaint();
		} else if( !savedImages ) {
			savedRgb.setTo(rgb);
			savedDepth.setTo(depth);
			savedImages = true;
		}
	}

	public static void main( String args[] ) throws IOException {
		CaptureCalibrationImagesApp app = new CaptureCalibrationImagesApp(3000,"log/");
		app.process();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if( e.getKeyChar() == 'y' )
			userChoice = 1;
		else
			userChoice = 0;
	}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}
}

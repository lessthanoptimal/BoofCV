/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.d3;

import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * <p>
 * Renders a disparity image in 3D as viewed from above.
 * </p>
 *
 * <p>
 * Rendering speed is improved by first rendering onto a grid and only accepting the highest
 * (closest to viewing camera) point as being visible.
 * </p>
 *
 * @author Peter Abeles
 */
public class PointCloudSideViewer extends JPanel {
	FastQueue<ColorPoint3D> cloud = new FastQueue<ColorPoint3D>(200,ColorPoint3D.class,true);

	// distance between the two camera centers
	double baseline;

	// intrinsic camera parameters
	double focalLengthX;
	double focalLengthY;
	double centerX;
	double centerY;

	// minimum disparity
	int minDisparity;
	// maximum minus minimum disparity
	int rangeDisparity;

	// scale, adjust to zoom in and out
	double scale = 1;

	// view offset
	double offsetX;
	double offsetY;

	// Data structure that contains the visible point at each pixel
	// size = width*height, row major format
	Pixel data[] = new Pixel[0];

	// tilt angle in degrees
	public int tiltAngle = 0;

	/**
	 * Stereo and intrinsic camera parameters
	 * @param baseline Stereo baseline (world units)
	 * @param focalLengthX Focal length parameter x-axis (pixels)
	 * @param focalLengthY Focal length parameter y-axis (pixels)
	 * @param centerX Optical center x-axis (pixels)
	 * @param centerY Optical center y-axis  (pixels)
	 * @param minDisparity Minimum disparity that's computed (pixels)
	 * @param maxDisparity Maximum disparity that's computed (pixels)
	 */
	public void configure(double baseline,
						  double focalLengthX, double focalLengthY,
						  double centerX, double centerY,
						  int minDisparity, int maxDisparity) {
		this.baseline = baseline;
		this.focalLengthX = focalLengthX;
		this.focalLengthY = focalLengthY;
		this.centerX = centerX;
		this.centerY = centerY;
		this.minDisparity = minDisparity;

		this.rangeDisparity = maxDisparity-minDisparity;
	}

	/**
	 * Given the disparity image compute the 3D location of valid points and save pixel colors
	 * at that point
	 *
	 * @param disparity Disparity image
	 * @param color Color image of left camera
	 */
	public void process( ImageSingleBand disparity , BufferedImage color ) {
		if( disparity instanceof ImageUInt8 )
			process((ImageUInt8)disparity,color);
		else
			process((ImageFloat32)disparity,color);
	}

	private void process( ImageUInt8 disparity , BufferedImage color ) {

		cloud.reset();

		for( int y = 0; y < disparity.height; y++ ) {
			int index = disparity.startIndex + disparity.stride*y;

			for( int x = 0; x < disparity.width; x++ ) {
				int value = disparity.data[index++] & 0xFF;

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				ColorPoint3D p = cloud.pop();

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;
				p.rgb = color.getRGB(x,y);
			}
		}
	}

	private void process( ImageFloat32 disparity , BufferedImage color ) {

		cloud.reset();

		for( int y = 0; y < disparity.height; y++ ) {
			int index = disparity.startIndex + disparity.stride*y;

			for( int x = 0; x < disparity.width; x++ ) {
				float value = disparity.data[index++];

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				ColorPoint3D p = cloud.pop();

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;
				p.rgb = color.getRGB(x,y);
			}
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		projectScene();

		int width = getWidth();
		int h = getHeight();

		int r = 2;
		int w = r*2+1;

		Graphics2D g2 = (Graphics2D)g;

		int index = 0;
		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < width; x++ ) {
				Pixel p = data[index++];
				if( p.rgb == 0xFFFFFF )
					continue;

				g2.setColor(new Color(p.rgb));
				g2.fillRect(x - r, y - r, w, w);
			}
		}
	}

	private void projectScene() {
		int w = getWidth();
		int h = getHeight();

		int N = w*h;

		if( data.length < N ) {
			data = new Pixel[ N ];
			for( int i = 0; i < N; i++ )
				data[i] = new Pixel();
		} else {
			for( int i = 0; i < N; i++ )
				data[i].reset();
		}

		int centerX = getWidth()/2;

		double c = Math.cos(tiltAngle*Math.PI/180.0);
		double s = Math.sin(tiltAngle*Math.PI/180.0);

		for( int i = 0; i < cloud.size(); i++ ) {
			ColorPoint3D p = cloud.get(i);

			double X = p.x;
			double Y = c*p.z - s*p.y;
			double Z = s*p.z + c*p.y;

			int x = (int)((X+offsetX)*scale ) + centerX;
			int y = h - (int)((Y+offsetY)*scale) - 1;

			if( x < 0 || y < 0 || x >= w || y >= h )
				continue;

			Pixel d = data[y*w+x];
			if( d.height > Z ) {
				d.height = Z;
				d.rgb = p.rgb;
			}
		}
	}

	/**
	 * Contains information on visible pixels
	 */
	private static class Pixel
	{
		// the pixel's height.  used to see if it is closer to the  camera or not
		public double height;
		// Color of the pixel
		public int rgb;

		private Pixel() {
			reset();
		}

		public void reset() {
			height = Double.MAX_VALUE;
			rgb = 0xFFFFFF;
		}
	}
}

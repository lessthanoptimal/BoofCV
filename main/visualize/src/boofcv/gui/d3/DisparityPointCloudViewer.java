/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * <p>
 * Renders a 3D point cloud using a perspective pin hole camera model.
 * </p>
 *
 * <p>
 * Rendering speed is improved by first rendering onto a grid and only accepting the highest
 * (closest to viewing camera) point as being visible.
 * </p>
 *
 * @author Peter Abeles
 */
public class DisparityPointCloudViewer extends JPanel {
	FastQueue<ColorPoint3D> cloud = new FastQueue<>(200, ColorPoint3D.class, true);

	// distance between the two camera centers
	double baseline;

	// intrinsic camera parameters
	DenseMatrix64F K;
	double focalLengthX;
	double focalLengthY;
	double centerX;
	double centerY;

	// minimum disparity
	int minDisparity;
	// maximum minus minimum disparity
	int rangeDisparity;

	// How far out it should zoom.
	double range = 1;

	// view offset
	double offsetX;
	double offsetY;

	// Data structure that contains the visible point at each pixel
	// size = width*height, row major format
	Pixel data[] = new Pixel[0];

	// tilt angle in degrees
	public int tiltAngle = 0;
	public double radius = 5;

	// converts from rectified pixels into color image pixels
	Point2Transform2_F64 rectifiedToColor;
	// storage for color image coordinate
	Point2D_F64 colorPt = new Point2D_F64();

	/**
	 * Stereo and intrinsic camera parameters
	 * @param baseline Stereo baseline (world units)
	 * @param K Intrinsic camera calibration matrix of rectified camera
	 * @param rectifiedToColor Transform from rectified pixels to the color image pixels.
	 * @param minDisparity Minimum disparity that's computed (pixels)
	 * @param maxDisparity Maximum disparity that's computed (pixels)
	 */
	public void configure(double baseline,
						  DenseMatrix64F K,
						  Point2Transform2_F64 rectifiedToColor,
						  int minDisparity, int maxDisparity) {
		this.K = K;
		this.rectifiedToColor = rectifiedToColor;
		this.baseline = baseline;
		this.focalLengthX = K.get(0,0);
		this.focalLengthY = K.get(1,1);
		this.centerX = K.get(0,2);
		this.centerY = K.get(1,2);
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
	public void process(ImageGray disparity , BufferedImage color ) {
		if( disparity instanceof GrayU8)
			process((GrayU8)disparity,color);
		else
			process((GrayF32)disparity,color);
	}

	private void process(GrayU8 disparity , BufferedImage color ) {

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

				ColorPoint3D p = cloud.grow();

				// Note that this will be in the rectified left camera's reference frame.
				// An additional rotation is needed to put it into the original left camera frame.
				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;

				getColor(disparity, color, x, y, p);
			}
		}
	}

	private void process(GrayF32 disparity , BufferedImage color ) {

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

				ColorPoint3D p = cloud.grow();

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;

				getColor(disparity, color, x, y, p);
			}
		}
	}

	private void getColor(ImageBase disparity, BufferedImage color, int x, int y, ColorPoint3D p) {
		rectifiedToColor.compute(x,y,colorPt);
		if( BoofMiscOps.checkInside(disparity, colorPt.x, colorPt.y, 0) ) {
			p.rgb = color.getRGB((int)colorPt.x,(int)colorPt.y);
		} else {
			p.rgb = 0x000000;
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
				if( p.rgb == -1 )
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

		Se3_F64 pose = createWorldToCamera();
		Point3D_F64 cameraPt = new Point3D_F64();
		Point2D_F64 pixel = new Point2D_F64();

		for( int i = 0; i < cloud.size(); i++ ) {
			ColorPoint3D p = cloud.get(i);

			SePointOps_F64.transform(pose,p,cameraPt);
			pixel.x = cameraPt.x/cameraPt.z;
			pixel.y = cameraPt.y/cameraPt.z;

			GeometryMath_F64.mult(K,pixel,pixel);

			int x = (int)pixel.x;
			int y = (int)pixel.y;

			if( x < 0 || y < 0 || x >= w || y >= h )
				continue;

			Pixel d = data[y*w+x];
			if( d.height > cameraPt.z ) {
				d.height = cameraPt.z;
				d.rgb = p.rgb;
			}
		}
	}

	public Se3_F64 createWorldToCamera() {
		// pick an appropriate amount of motion for the scene
		double z = baseline*focalLengthX/(minDisparity+rangeDisparity);

		double adjust = baseline/20.0;

		Vector3D_F64 rotPt = new Vector3D_F64(offsetX*adjust,offsetY*adjust,z* range);

		double radians = tiltAngle*Math.PI/180.0;
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,radians,0,0,null);

		Se3_F64 a = new Se3_F64(R,rotPt);

		return a;
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
			rgb = -1;
		}
	}
}

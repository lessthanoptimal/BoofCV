/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F32;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

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
public class DisparityToColorPointCloud {
	// distance between the two camera centers
	float baseline;

	// intrinsic camera parameters
	DMatrixRMaj K;
	float focalLengthX;
	float focalLengthY;
	float centerX;
	float centerY;
	FMatrixRMaj rectifiedR = new FMatrixRMaj(3,3);

	// minimum disparity
	int minDisparity;
	// maximum minus minimum disparity
	int rangeDisparity;

	// How far out it should zoom.
	double range = 1;

	// Storage for point cloud
	GrowQueue_F32 cloudXyz = new GrowQueue_F32();
	GrowQueue_I32 cloudRgb = new GrowQueue_I32();

	// tilt angle in degrees
	public int tiltAngle = 0;
	public double radius = 5;

	// converts from rectified pixels into color image pixels
	Point2Transform2_F64 rectifiedToColor;
	// storage for color image coordinate
	Point2D_F64 colorPt = new Point2D_F64();

	Point3D_F32 p = new Point3D_F32();

	/**
	 * Stereo and intrinsic camera parameters
	 * @param baseline Stereo baseline (world units)
	 * @param K Intrinsic camera calibration matrix of rectified camera
	 * @param rectifiedToColor Transform from rectified pixels to the color image pixels.
	 * @param minDisparity Minimum disparity that's computed (pixels)
	 * @param maxDisparity Maximum disparity that's computed (pixels)
	 */
	public void configure(double baseline,
						  DMatrixRMaj K, DMatrixRMaj rectifiedR,
						  Point2Transform2_F64 rectifiedToColor,
						  int minDisparity, int maxDisparity) {
		this.K = K;
		ConvertMatrixData.convert(rectifiedR,this.rectifiedR);
		this.rectifiedToColor = rectifiedToColor;
		this.baseline = (float)baseline;
		this.focalLengthX = (float)K.get(0,0);
		this.focalLengthY = (float)K.get(1,1);
		this.centerX = (float)K.get(0,2);
		this.centerY = (float)K.get(1,2);
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
		cloudRgb.setMaxSize(disparity.width*disparity.height);
		cloudXyz.setMaxSize(disparity.width*disparity.height*3);
		cloudRgb.reset();
		cloudXyz.reset();

		if( disparity instanceof GrayU8)
			process((GrayU8)disparity,color);
		else
			process((GrayF32)disparity,color);
	}

	private void process(GrayU8 disparity , BufferedImage color ) {

		for( int pixelY = 0; pixelY < disparity.height; pixelY++ ) {
			int index = disparity.startIndex + disparity.stride*pixelY;

			for( int pixelX = 0; pixelX < disparity.width; pixelX++ ) {
				int value = disparity.data[index++] & 0xFF;

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				// Note that this will be in the rectified left camera's reference frame.
				// An additional rotation is needed to put it into the original left camera frame.
				p.z = baseline*focalLengthX/value;
				p.x = p.z*(pixelX - centerX)/focalLengthX;
				p.y = p.z*(pixelY - centerY)/focalLengthY;

				// Bring it back into left camera frame
				GeometryMath_F32.multTran(rectifiedR,p,p);

				cloudRgb.add(getColor(disparity, color, pixelX, pixelY));
				cloudXyz.add(p.x);
				cloudXyz.add(p.y);
				cloudXyz.add(p.z);
			}
		}
	}

	private void process(GrayF32 disparity , BufferedImage color ) {

		for( int pixelY = 0; pixelY < disparity.height; pixelY++ ) {
			int index = disparity.startIndex + disparity.stride*pixelY;

			for( int pixelX = 0; pixelX < disparity.width; pixelX++ ) {
				float value = disparity.data[index++];

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(pixelX - centerX)/focalLengthX;
				p.y = p.z*(pixelY - centerY)/focalLengthY;

				// Bring it back into left camera frame
				GeometryMath_F32.multTran(rectifiedR,p,p);

				cloudRgb.add(getColor(disparity, color, pixelX, pixelY));
				cloudXyz.add(p.x);
				cloudXyz.add(p.y);
				cloudXyz.add(p.z);
			}
		}
	}

	private int getColor(ImageBase disparity, BufferedImage color, int x, int y ) {
		rectifiedToColor.compute(x,y,colorPt);
		if( BoofMiscOps.checkInside(disparity, colorPt.x, colorPt.y) ) {
			return color.getRGB((int)colorPt.x,(int)colorPt.y);
		} else {
			return 0x000000;
		}
	}

	public GrowQueue_F32 getCloud() {
		return cloudXyz;
	}

	public GrowQueue_I32 getCloudColor() {
		return cloudRgb;
	}

}

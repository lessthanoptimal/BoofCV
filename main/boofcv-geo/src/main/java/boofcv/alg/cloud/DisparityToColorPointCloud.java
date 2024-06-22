/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.cloud;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Renders a 3D point cloud using a perspective pin hole camera model. An ROI can be specified if only part
 * of the image is desired
 * </p>
 *
 * <p>
 * Rendering speed is improved by first rendering onto a grid and only accepting the highest
 * (closest to viewing camera) point as being visible.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DisparityToColorPointCloud {
	// distance between the two camera centers
	float baseline;

	// intrinsic camera parameters
	DMatrixRMaj K;
	float focalLengthX;
	float focalLengthY;
	float centerX;
	float centerY;
	FMatrixRMaj rectifiedR = new FMatrixRMaj(3, 3);

	// minimum disparity
	int disparityMin;
	// maximum minus minimum disparity
	int disparityRange;

	// tilt angle in degrees
	public double radius = 5;

	// converts from rectified pixels into color image pixels
	Point2Transform2_F64 rectifiedToColor;
	// storage for color image coordinate
	Point2D_F64 colorPt = new Point2D_F64();

	Point3D_F32 p = new Point3D_F32();

	// region of interest
	Rectangle2D_I32 roi = new Rectangle2D_I32();

	/**
	 * Stereo and intrinsic camera parameters
	 *
	 * @param baseline Stereo baseline (world units)
	 * @param K Intrinsic camera calibration matrix of rectified camera
	 * @param rectifiedToColor Transform from rectified pixels to the color image pixels. If null then no transform.
	 * @param disparityMin Minimum disparity that's computed (pixels)
	 * @param disparityRange Number of possible disparity values (pixels)
	 */
	public void configure( double baseline,
						   DMatrixRMaj K, DMatrixRMaj rectifiedR,
						   @Nullable Point2Transform2_F64 rectifiedToColor,
						   int disparityMin, int disparityRange ) {
		if (rectifiedToColor == null)
			rectifiedToColor = new DoNothing2Transform2_F64();
		this.K = K;
		ConvertMatrixData.convert(rectifiedR, this.rectifiedR);
		this.rectifiedToColor = rectifiedToColor;
		this.baseline = (float)baseline;
		this.focalLengthX = (float)K.get(0, 0);
		this.focalLengthY = (float)K.get(1, 1);
		this.centerX = (float)K.get(0, 2);
		this.centerY = (float)K.get(1, 2);
		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;

		clearRegionOfInterest();
	}

	/**
	 * Convenience function which allows you to use {@link DisparityParameters} and calls
	 * {@link #configure(double, DMatrixRMaj, DMatrixRMaj, Point2Transform2_F64, int, int)}.
	 */
	public void configure( DisparityParameters param, @Nullable Point2Transform2_F64 rectifiedToColor ) {
		if (rectifiedToColor == null)
			rectifiedToColor = new DoNothing2Transform2_F64();
		this.configure(param.baseline,
				PerspectiveOps.pinholeToMatrix(param.pinhole, K),
				param.rotateToRectified,
				rectifiedToColor,
				param.disparityMin,
				param.disparityRange);
	}

	/**
	 * Given the disparity image compute the 3D location of valid points and save pixel colors
	 * at that point
	 *
	 * @param disparity Disparity image
	 * @param color Color image of left camera
	 */
	public void process( ImageGray<?> disparity, ColorImage color, PointCloudWriter output ) {
		if (disparity instanceof GrayU8)
			process((GrayU8)disparity, color, output);
		else if (disparity instanceof GrayF32)
			process((GrayF32)disparity, color, output);
		else
			throw new IllegalArgumentException("Unsupported image type " + disparity.getClass().getSimpleName());
	}

	/**
	 * Converts the disparity image into a color point cloud
	 *
	 * @param disparity (Input) Disparity image
	 * @param color (Input) Color image
	 * @param output (Output) destination for the colorized point cloud
	 */
	private void process( GrayU8 disparity, ColorImage color, PointCloudWriter output ) {
		final int x0 = Math.max(roi.x0, 0);
		final int y0 = Math.max(roi.y0, 0);
		final int x1 = Math.min(roi.x1, disparity.width);
		final int y1 = Math.min(roi.y1, disparity.height);

		for (int pixelY = y0; pixelY < y1; pixelY++) {
			int index = disparity.startIndex + disparity.stride*pixelY + x0;

			for (int pixelX = x0; pixelX < x1; pixelX++) {
				int value = disparity.data[index++] & 0xFF;

				if (value >= disparityRange)
					continue;

				value += disparityMin;

				// The point lies at infinity.
				if (value == 0)
					continue;

				// Note that this will be in the rectified left camera's reference frame.
				// An additional rotation is needed to put it into the original left camera frame.
				p.z = baseline*focalLengthX/value;
				p.x = p.z*(pixelX - centerX)/focalLengthX;
				p.y = p.z*(pixelY - centerY)/focalLengthY;

				// Bring it back into left camera frame
				GeometryMath_F32.multTran(rectifiedR, p, p);

				output.startPoint();
				output.location(p.x, p.y, p.z);
				output.color(getColor(color, pixelX, pixelY));
				output.stopPoint();
			}
		}
	}

	/**
	 * Converts the disparity image into a color point cloud
	 *
	 * @param disparity (Input) Disparity image
	 * @param color (Input) Color image
	 * @param output (Output) destination for the colorized point cloud
	 */
	private void process( GrayF32 disparity, ColorImage color, PointCloudWriter output ) {

		final int x0 = Math.max(roi.x0, 0);
		final int y0 = Math.max(roi.y0, 0);
		final int x1 = Math.min(roi.x1, disparity.width);
		final int y1 = Math.min(roi.y1, disparity.height);

		for (int pixelY = y0; pixelY < y1; pixelY++) {
			int index = disparity.startIndex + disparity.stride*pixelY + x0;

			for (int pixelX = x0; pixelX < x1; pixelX++) {
				float value = disparity.data[index++];

				// invalid disparity
				if (value >= disparityRange)
					continue;

				value += disparityMin;

				// The point lies at infinity.
				if (value == 0)
					continue;

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(pixelX - centerX)/focalLengthX;
				p.y = p.z*(pixelY - centerY)/focalLengthY;

				// Bring it back into left camera frame
				GeometryMath_F32.multTran(rectifiedR, p, p);

				output.startPoint();
				output.location(p.x, p.y, p.z);
				output.color(getColor(color, pixelX, pixelY));
				output.stopPoint();
			}
		}
	}

	/**
	 * @param color color image
	 * @param x coordinate in disparity image
	 * @param y coordinate in disparity image
	 * @return RGB
	 */
	private int getColor( ColorImage color, int x, int y ) {
		rectifiedToColor.compute(x, y, colorPt);
		int xx = (int)colorPt.getX();
		int yy = (int)colorPt.getY();

		if (color.isInBounds(xx, yy)) {
			return color.getRGB(xx, yy);
		} else {
			return 0x000000;
		}
	}

	/**
	 * Specifies a ROI in the disparity image the cloud should come from
	 *
	 * @param x0 lower extent, x-axis, inclusive
	 * @param y0 lower extent, y-axis, inclusive
	 * @param x1 upper extent, x-axis, exclusive
	 * @param y1 upper extent, y-axis, exclusive
	 */
	public void setRegionOfInterest( int x0, int y0, int x1, int y1 ) {
		roi.setTo(x0, y0, x1, y1);
	}

	/**
	 * Removes the ROI
	 */
	public void clearRegionOfInterest() {
		roi.setTo(-1, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Interface for accessing RGB values inside an image
	 */
	public interface ColorImage {
		boolean isInBounds( int x, int y );

		int getRGB( int x, int y );
	}
}

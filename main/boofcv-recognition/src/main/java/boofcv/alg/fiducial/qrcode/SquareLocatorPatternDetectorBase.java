/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.interpolate.InterpolatePixelDistortS;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.MovingAverage;
import boofcv.struct.ConfigLength;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Several fiducials use square objects as locator patterns for the markers. This contains common code.
 *
 * @author Peter Abeles
 */
public abstract class SquareLocatorPatternDetectorBase<T extends ImageGray<T>> implements VerbosePrint {
	/** used to subsample the input image */
	@Getter protected InterpolatePixelS<T> interpolate;

	/** Used to prune very large contours. This is tuned for QR codes which have two position patterns side by side */
	@Getter @Setter protected double maxContourFraction = 4.0/3.0;

	/** Used to detect black squares */
	@Getter protected DetectPolygonBinaryGrayRefine<T> squareDetector;

	/** runtime profiling */
	@Getter protected MovingAverage profilingMS = new MovingAverage(0.8);

	@Nullable protected PrintStream verbose = null;

	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 */
	protected SquareLocatorPatternDetectorBase( DetectPolygonBinaryGrayRefine<T> squareDetector ) {
		this.squareDetector = squareDetector;

		squareDetector.getDetector().setConvex(true);
		squareDetector.getDetector().setOutputClockwiseUpY(false);
		squareDetector.getDetector().setNumberOfSides(4, 4);

		interpolate = FactoryInterpolation.bilinearPixelS(squareDetector.getInputType(), BorderType.EXTENDED);
	}

	/**
	 * Detects position patterns inside the image and forms a graph.
	 *
	 * @param gray Gray scale input image
	 * @param binary Binary version of gray image.
	 */
	public void process( T gray, GrayU8 binary ) {
		// don't sanity check binary shape here since it may or may not be padded. See square detector
		configureContourDetector(gray);
		interpolate.setImage(gray);

		// detect squares
		squareDetector.process(gray, binary);

		long time0 = System.nanoTime();
		findLocatorPatternsFromSquares();
		long time1 = System.nanoTime();

		profilingMS.update((time1 - time0)*1e-6);
	}

	/** Called after squares have been detected and you are using detected squares to identify locator patterns */
	protected abstract void findLocatorPatternsFromSquares();

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates. The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width. Used in sanity check only.
	 * @param height Input image height. Used in sanity check only.
	 * @param model distortion model. Null to remove a distortion model.
	 */
	public void setLensDistortion( int width, int height, @Nullable LensDistortionNarrowFOV model ) {
		interpolate = FactoryInterpolation.bilinearPixelS(
				squareDetector.getInputType(), BorderType.EXTENDED);

		if (model != null) {
			PixelTransform<Point2D_F32> distToUndist = new PointToPixelTransform_F32(model.undistort_F32(true, true));
			PixelTransform<Point2D_F32> undistToDist = new PointToPixelTransform_F32(model.distort_F32(true, true));

			squareDetector.setLensDistortion(width, height, distToUndist, undistToDist);

			// needs to sample the original image when the
			Point2Transform2_F32 u2d = model.distort_F32(true, true);
			this.interpolate = new InterpolatePixelDistortS<>(this.interpolate, u2d);
		} else {
			squareDetector.setLensDistortion(width, height, null, null);
		}
	}

	/**
	 * Configures the contour detector based on the image size. Setting a maximum contour and turning off recording
	 * of inner contours and improve speed and reduce the memory footprint significantly.
	 */
	protected void configureContourDetector( T gray ) {
		// determine the maximum possible size of a position pattern
		// contour size is maximum when viewed head one. Assume the smallest qrcode is 3x this width
		// 4 side in a square
		int maxContourSize = (int)(Math.min(gray.width, gray.height)*maxContourFraction);
		BinaryContourFinder contourFinder = squareDetector.getDetector().getContourFinder();
		contourFinder.setMaxContour(ConfigLength.fixed(maxContourSize));
		contourFinder.setSaveInnerContour(false);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}

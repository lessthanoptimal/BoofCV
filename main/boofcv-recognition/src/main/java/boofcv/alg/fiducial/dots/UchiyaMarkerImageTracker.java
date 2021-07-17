/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.dots;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.alg.shapes.ellipse.EdgeIntensityEllipse;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link UchiyaMarkerTracker} that includes image processing. Ellipses are detected by thresholding
 * the input image. Some basic filtering is done to find the most ellipse like objects. Then the center of each
 * ellipse is used as the location of a dot in the image.
 *
 * @author Peter Abeles
 */
public class UchiyaMarkerImageTracker<T extends ImageGray<T>> {
	// Storage the input image after it has been converted into a binary image
	@Getter GrayU8 binary = new GrayU8(1, 1);

	// Used to find ellipses in the image
	@Getter InputToBinary<T> inputToBinary;
	@Getter BinaryEllipseDetectorPixel ellipseDetector;
	@Getter EdgeIntensityEllipse<T> intensityCheck;

	@Getter UchiyaMarkerTracker tracker;

	@Getter List<Point2D_F64> foundDots = new ArrayList<>();

	/** Time to convert input image into a binary one */
	@Getter double timeBinary;
	/** Time to find ellipses */
	@Getter double timeEllipse;
	/** Time to reject ellipses */
	@Getter double timeReject;

	public UchiyaMarkerImageTracker( InputToBinary<T> inputToBinary,
									 BinaryEllipseDetectorPixel ellipseDetector,
									 EdgeIntensityEllipse<T> intensityCheck,
									 UchiyaMarkerTracker tracker ) {
		this.inputToBinary = inputToBinary;
		this.ellipseDetector = ellipseDetector;
		this.intensityCheck = intensityCheck;
		this.tracker = tracker;

		ellipseDetector.setInternalContour(false);
	}

	/**
	 * Processes the image looking for dots and from those Uchiya markers
	 *
	 * @param input Gray scale image
	 */
	public void detect( T input ) {
		// Filter out huge objects since they are very unlikely to be valid dots
		ellipseDetector.setMaximumContour(Math.min(input.width, input.height)/4);

		// Find the ellipses inside a binary image
		final long nano0 = System.nanoTime();
		inputToBinary.process(input, binary);
		final long nano1 = System.nanoTime();
		ellipseDetector.process(binary);
		final long nano2 = System.nanoTime();

		intensityCheck.setImage(input);

		// Use the centers as dots
		List<BinaryEllipseDetectorPixel.Found> foundRaw = ellipseDetector.getFound();
		foundDots.clear();
		for (int i = 0; i < foundRaw.size(); i++) {
			BinaryEllipseDetectorPixel.Found f = foundRaw.get(i);

			// Reject dots with low contrast
			if (!intensityCheck.process(f.ellipse)) {
				// mark the ellipse as pruned for visualization
				f.ellipse.a = f.ellipse.b = 0;
				continue;
			}
			// NOTE: These centers will not be the geometric centers. The geometric center could be found using
			//       tangent points and this would make it more accurate. Not sure it's worth the effort...
			foundDots.add(f.ellipse.center);
		}
		final long nano3 = System.nanoTime();

		// Save timing info for profiling
		timeBinary = (nano1 - nano0)*1e-6;
		timeEllipse = (nano2 - nano1)*1e-6;
		timeReject = (nano3 - nano2)*1e-6;

		// run the tracker
		tracker.process(foundDots);
	}

	/**
	 * Specify lens distortion. The ellipse will be fit to the undistorted image.
	 *
	 * @param distortion Distortion model.
	 * @param width Input image width
	 * @param height Input image height
	 */
	public void setLensDistortion( LensDistortionNarrowFOV distortion, int width, int height ) {
		if (distortion == null) {
			ellipseDetector.setLensDistortion(null);
			intensityCheck.setTransform(null);
		} else {
			Point2Transform2_F32 pointDistToUndist = distortion.undistort_F32(true, true);
			Point2Transform2_F32 point_undist_to_dist = distortion.distort_F32(true, true);
			PixelTransform<Point2D_F32> distToUndist = new PointToPixelTransform_F32(pointDistToUndist);
			PixelTransform<Point2D_F32> undist_to_dist = new PointToPixelTransform_F32(point_undist_to_dist);

			ellipseDetector.setLensDistortion(distToUndist);
			intensityCheck.setTransform(undist_to_dist);
		}
	}

	public void reset() {
		tracker.resetTracking();
	}

	/**
	 * Returns list of actively tracked markers
	 */
	public DogArray<UchiyaMarkerTracker.Track> getTracks() {
		return tracker.getCurrentTracks();
	}
}

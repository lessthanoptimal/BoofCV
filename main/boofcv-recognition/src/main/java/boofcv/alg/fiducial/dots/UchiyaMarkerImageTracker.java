/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;

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
	@Getter GrayU8 binary = new GrayU8(1,1);

	// Used to find ellipses in the image
	@Getter InputToBinary<T> inputToBinary;
	@Getter BinaryEllipseDetectorPixel ellipseDetector;

	@Getter UchiyaMarkerTracker tracker;

	@Getter List<Point2D_F64> foundDots = new ArrayList<>();

	public UchiyaMarkerImageTracker(InputToBinary<T> inputToBinary,
									BinaryEllipseDetectorPixel ellipseDetector,
									UchiyaMarkerTracker tracker) {
		this.inputToBinary = inputToBinary;
		this.ellipseDetector = ellipseDetector;
		this.tracker = tracker;

		ellipseDetector.setInternalContour(false);
		ellipseDetector.setMinimumContour(10);
	}

	/**
	 * Processes the image looking for dots and from those Uchiya markers
	 * @param input Gray scale image
	 */
	public void detect(T input) {
		// Filter out huge objects since they are very unlikely to be valid dots
		ellipseDetector.setMaximumContour(Math.min(input.width,input.height)/4);

		// Find the ellipses inside a binary image
		inputToBinary.process(input,binary);
		ellipseDetector.process(binary);

		// Use the centers as dots
		List<BinaryEllipseDetectorPixel.Found> foundRaw = ellipseDetector.getFound();
		foundDots.clear();
		for (int i = 0; i < foundRaw.size(); i++) {
			// NOTE: These centers will not be the geometric centers. The geometric center could be found using
			//       tangent points and this would make it more accurate. Not sure it's worth the effort...
			foundDots.add( foundRaw.get(i).ellipse.center );
		}

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
	public void setLensDistortion(LensDistortionNarrowFOV distortion, int width, int height) {
		if( distortion == null )
			ellipseDetector.setLensDistortion(null);
		else {
			Point2Transform2_F32 pointDistToUndist = distortion.undistort_F32(true, true);
			PixelTransform<Point2D_F32> distToUndist = new PointToPixelTransform_F32(pointDistToUndist);

			ellipseDetector.setLensDistortion(distToUndist);
		}
	}

	public void reset() {
		tracker.resetTracking();
	}

	/**
	 * Returns list of actively tracked markers
	 */
	public FastQueue<UchiyaMarkerTracker.Track> getTracks() {
		return tracker.getCurrentTracks();
	}
}

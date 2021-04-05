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

package boofcv.alg.mvs;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Scores different views to act as a common view based on coverage of rectified image. It works by using a shrunk
 * down image to compute the area which would be covered by multiple stereo images. An image
 * with more coverage and more images overlaying it is scored higher.
 *
 * @author Peter Abeles
 */
public class ScoreRectifiedViewCoveragePixels {

	/** Maximum length the largest side can be */
	public @Getter @Setter int maxSide = 100;

	/** The found area covered score. Computed from fraction of area covered and average count */
	public @Getter @Setter double score = 0.0;

	/** This is used to adjust the influence of average. Larger the value less influence */
	public @Getter @Setter double scoreAverageOffset = 10.0;

	/** The actual fraction of the image covered by neighboring views */
	public @Getter @Setter double covered = 0.0;

	/** Average value of covered pixels */
	public @Getter @Setter double averageValue = 0.0;

	// Calibrated intrinsic parameters for "left" camera
	double scale;
	// Indicates the number of times a 3D disparity lands on this pixel. -1 means the pixel is invalid
	final GrayF32 viewed = new GrayF32(1, 1);
	// Precomputed transform from distorted pixels to undistorted pixels
	final DogArray<Point2D_F64> pixel_to_undist = new DogArray<>(Point2D_F64::new, p -> p.setTo(0.0, 0.0));

	// internal work space variables
	Homography2D_F64 pixel_to_rect = new Homography2D_F64();
	Point2D_F64 rectified = new Point2D_F64();

	/**
	 * Initializes data structures and specifies lens distortion for the target view
	 *
	 * @param width Width of original image in this view
	 * @param height Height of original image in this view
	 * @param transform_pixel_to_undist Lens distortion model from distorted to undistorted pixels
	 */
	public void initialize( int width, int height,
							PixelTransform<Point2D_F64> transform_pixel_to_undist ) {

		score = 0.0;
		covered = 0.0;

		// Compute the scale of the small image relative to the original
		scale = Math.min(maxSide/(double)width, maxSide/(double)height);

		// Update the binary image's shape
		viewed.reshape((int)(scale*width), (int)(scale*height));
		ImageMiscOps.fill(viewed, 0);

		// precompute pixel to undistorted normalized image coordinates
		this.pixel_to_undist.resize(viewed.width*viewed.height);
		for (int y = 0, index = 0; y < viewed.height; y++) {
			for (int x = 0; x < viewed.width; x++, index++) {
				// Compute undistorted pixel after scaling it back to the original resolution
				Point2D_F64 undist = this.pixel_to_undist.get(index);
				transform_pixel_to_undist.compute((int)(x/scale), (int)(y/scale), undist);

				// If the distortion model can't handle this pixel mark it as invalid
				if (UtilEjml.isUncountable(undist.x) || UtilEjml.isUncountable(undist.y))
					viewed.unsafe_set(x, y, -1);
			}
		}
	}

	/**
	 * Adds a view by specifying how target view would be rectified for this stereo pair.
	 *
	 * @param width (Input) Width of the new view being added
	 * @param height (Input) Height of the new view being added
	 * @param rect (Input) Homography from undistorted pixels to rectified pixels in this view.
	 * @param quality3D (Input) Signifies the quality of 3D information available. Higher numbers mean more 3D
	 * information from this view. A value of 0 indicates no 3D information. Typically this ranges from 0 to 1.
	 */
	public void addView( int width, int height, DMatrixRMaj rect, float quality3D, Operation op ) {
		// if the quality is zero it can't contribute
		if (quality3D == 0.0f)
			return;
		checkTrue(quality3D >= 0.0f, "Quality must be positive");
		checkTrue(scale != 0.0, "You must call initialize() first");

		pixel_to_rect.setTo(rect);

		for (int y = 0, index = 0; y < viewed.height; y++) {
			for (int x = 0; x < viewed.width; x++, index++) {
				// skip if invalid
				if (viewed.data[index] < 0)
					continue;
				Point2D_F64 fusedNorm = pixel_to_undist.get(index);

				// undistorted to rectified pixels
				HomographyPointOps_F64.transform(pixel_to_rect, fusedNorm.x, fusedNorm.y, rectified);

				// Make sure it's inside the disparity image before sampling
				if (!BoofMiscOps.isInside(width, height, rectified.x, rectified.y))
					continue;

				viewed.data[index] = op.process(viewed.data[index], quality3D);
			}
		}
	}

	/**
	 * Computes the fraction of the rectified image which is inside the image
	 */
	public double fractionIntersection( int width, int height, DMatrixRMaj rect ) {
		pixel_to_rect.setTo(rect);

		int intersectedCount = 0;

		for (int y = 0, index = 0; y < viewed.height; y++) {
			for (int x = 0; x < viewed.width; x++, index++) {
				// skip if invalid
				if (viewed.data[index] < 0)
					continue;
				Point2D_F64 fusedNorm = pixel_to_undist.get(index);

				// undistorted to rectified pixels
				HomographyPointOps_F64.transform(pixel_to_rect, fusedNorm.x, fusedNorm.y, rectified);

				// Make sure it's inside the disparity image before sampling
				if (!BoofMiscOps.isInside(width, height, rectified.x, rectified.y))
					continue;

				intersectedCount++;
			}
		}

		return intersectedCount/(double)(viewed.width*viewed.height);
	}

	/**
	 * Processes viewed image and computes a coverage score.
	 */
	public void process() {
		int totalValid = 0;
		float total = 0.0f;

		for (int y = 0, index = 0; y < viewed.height; y++) {
			for (int x = 0; x < viewed.width; x++, index++) {
				float value = viewed.data[index];
				if (value <= 0.0f)
					continue;

				totalValid++;
				total += value;
			}
		}

		covered = totalValid/(double)(viewed.width*viewed.height);

		double average = total/(double)(1 + totalValid);
		score = covered*(scoreAverageOffset + average);

		averageValue = total/(1.0f + totalValid);
	}

	@FunctionalInterface interface Operation {
		float process( float a, float b );
	}
}

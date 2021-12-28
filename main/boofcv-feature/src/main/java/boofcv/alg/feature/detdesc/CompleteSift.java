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

package boofcv.alg.feature.detdesc;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftDetector.SiftPoint;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.interest.UnrollSiftScaleSpaceGradient;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.util.List;

/**
 * SIFT combined together to simultaneously detect and describe the key points it finds. Memory is conserved by
 * only having one octave of the scale-space in memory at any given time.
 *
 * @author Peter Abeles
 * @see OrientationHistogramSift
 * @see DescribePointSift
 * @see SiftDetector
 */
public class CompleteSift {
	@Getter SiftScaleSpace scaleSpace;
	// Detects SIFT features
	@Getter SiftDetector detector;
	// estimate orientation
	@Getter OrientationHistogramSift<GrayF32> orientation;
	// describes the keypoints
	@Getter DescribePointSift<GrayF32> describe;
	// storage for found features
	DogArray<TupleDesc_F64> features;
	// found orientations and feature locations
	FastArray<ScalePoint> locations = new FastArray<>(ScalePoint.class);
	@Getter DogArray_F64 orientations = new DogArray_F64();

	UnrollSiftScaleSpaceGradient gradient = new UnrollSiftScaleSpaceGradient();

	/**
	 * Configures SIFT
	 *
	 * @param detector Feature detector
	 * @param orientation Estimates feature orientation(s)
	 * @param describe Describes a SIFT feature
	 */
	public CompleteSift( SiftScaleSpace scaleSpace,
						 SiftDetector detector,
						 OrientationHistogramSift<GrayF32> orientation,
						 DescribePointSift<GrayF32> describe ) {
		this.scaleSpace = scaleSpace;
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;

		final int dof = describe.getDescriptorLength();
		features = new DogArray<>(() -> new TupleDesc_F64(dof));
		gradient.initialize(scaleSpace);
	}

	/**
	 * Detects features inside the image and computes descriptors
	 */
	public void process( GrayF32 input ) {
		// Clear previous results
		features.reset();
		locations.reset();
		orientations.reset();

		// Compute the scale space
		scaleSpace.process(input);

		// Detect features
		detector.process(scaleSpace);

		// Precompute gradient
		gradient.process(scaleSpace);

		// Describe every point
		List<SiftPoint> detections = detector.getDetections();
		describeDetections(detections);
	}

	/**
	 * Computes one or more descriptors for every point in the passed in list
	 */
	protected void describeDetections( List<SiftPoint> detections ) {
		for (int detIdx = 0; detIdx < detections.size(); detIdx++) {
			SiftPoint p = detections.get(detIdx);

			// Would it be faster if all features that come from the same images were processed at once?
			// Would it reduce context switching?

			// Gradient image is offset by one
			GrayF32 derivX = gradient.getDerivX(p.octaveIdx, (byte)(p.scaleIdx - 1));
			GrayF32 derivY = gradient.getDerivY(p.octaveIdx, (byte)(p.scaleIdx - 1));

			orientation.setImageGradient(derivX, derivY);
			describe.setImageGradient(derivX, derivY);

			double pixelScaleToInput = scaleSpace.pixelScaleCurrentToInput(p.octaveIdx);

			// adjust the image for the down sampling in each octave
			double localX = p.pixel.x/pixelScaleToInput;
			double localY = p.pixel.y/pixelScaleToInput;
			double localSigma = p.scale/pixelScaleToInput;

			// find potential orientations first
			orientation.process(localX, localY, localSigma);

			// describe each feature
			DogArray_F64 angles = orientation.getOrientations();
			for (int angleIdx = 0; angleIdx < angles.size; angleIdx++) {
				describe.process(localX, localY, localSigma, angles.get(angleIdx), features.grow());
				orientations.add(angles.get(angleIdx));
				locations.add(p);
			}
		}
	}

	public FastAccess<ScalePoint> getLocations() {
		return locations;
	}

	public FastAccess<TupleDesc_F64> getDescriptions() {
		return features;
	}

	public int getDescriptorLength() {
		return describe.getDescriptorLength();
	}
}

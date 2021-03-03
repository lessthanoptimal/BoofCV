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

import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

/**
 * SIFT combined together to simultaneously detect and describe the key points it finds.  Memory is conserved by
 * only having one octave of the scale-space in memory at any given time.
 *
 * @author Peter Abeles
 * @see OrientationHistogramSift
 * @see DescribePointSift
 * @see SiftDetector
 */
public class CompleteSift extends SiftDetector {
	// estimate orientation
	OrientationHistogramSift<GrayF32> orientation;
	// describes the keypoints
	DescribePointSift<GrayF32> describe;
	// storage for found features
	DogArray<TupleDesc_F64> features;
	// found orientations and feature locations
	FastArray<ScalePoint> locations = new FastArray<>(ScalePoint.class);
	DogArray_F64 orientations = new DogArray_F64();

	// used to compute the image gradient
	ImageGradient<GrayF32, GrayF32> gradient = FactoryDerivative.three(GrayF32.class, null);

	// spacial derivative for the current scale in the octave
	GrayF32 derivX = new GrayF32(1, 1);
	GrayF32 derivY = new GrayF32(1, 1);

	/**
	 * Configures SIFT
	 *
	 * @param scaleSpace Scale-space that features are computed inside of
	 * @param edgeR Edge threshold.  See {@link SiftDetector#SiftDetector}
	 * @param extractor Finds minimums and maximums.  See {@link SiftDetector#SiftDetector}
	 * @param orientation Estimates feature orientation(s)
	 * @param describe Describes a SIFT feature
	 */
	public CompleteSift( SiftScaleSpace scaleSpace,
						 FeatureSelectLimitIntensity<ScalePoint> selectFeaturesAll,
						 double edgeR, NonMaxLimiter extractor,
						 OrientationHistogramSift<GrayF32> orientation,
						 DescribePointSift<GrayF32> describe ) {
		super(scaleSpace, selectFeaturesAll, edgeR, extractor);

		this.orientation = orientation;
		this.describe = describe;

		final int dof = describe.getDescriptorLength();
		features = new DogArray<>(() -> new TupleDesc_F64(dof));
	}

	@Override
	public void process( GrayF32 input ) {
		features.reset();
		locations.reset();
		orientations.reset();
		super.process(input);
	}

	@Override
	protected void detectFeatures( int scaleIndex ) {
		// compute image derivative for this scale
		GrayF32 input = scaleSpace.getImageScale(scaleIndex);
		derivX.reshape(input.width, input.height);
		derivY.reshape(input.width, input.height);
		gradient.process(input, derivX, derivY);

		// set up the orientation and description algorithms
		orientation.setImageGradient(derivX, derivY);
		describe.setImageGradient(derivX, derivY);

		super.detectFeatures(scaleIndex);
	}

	@Override
	protected void handleDetection( ScalePoint p ) {

		// adjust the image for the down sampling in each octave
		double localX = p.pixel.x/pixelScaleToInput;
		double localY = p.pixel.y/pixelScaleToInput;
		double localSigma = p.scale/pixelScaleToInput;

		// find potential orientations first
		orientation.process(localX, localY, localSigma);

		// describe each feature
		DogArray_F64 angles = orientation.getOrientations();
		for (int i = 0; i < angles.size; i++) {
			describe.process(localX, localY, localSigma, angles.get(i), features.grow());

			orientations.add(angles.get(i));
			locations.add(p);
		}
	}

	public FastAccess<ScalePoint> getLocations() {
		return locations;
	}

	public FastAccess<TupleDesc_F64> getDescriptions() {
		return features;
	}

	public DogArray_F64 getOrientations() {
		return orientations;
	}

	public int getDescriptorLength() {
		return describe.getDescriptorLength();
	}
}

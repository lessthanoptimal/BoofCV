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
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import pabeles.concurrency.GrowArray;

import java.util.List;

/**
 * Concurrent implementation of {@link CompleteSift}. The part which it makes concurrent is computing the descriptors.
 * Everything else is identical.
 *
 * @author Peter Abeles
 */
public class CompleteSift_MT extends CompleteSift {

	/** If there are fewer than this detections it will use the single threaded algorithm */
	public int minimumDetectionsThread = 50;

	// Work space for each thread
	GrowArray<ThreadHelper> helpers;

	// features from all threads combined together. Not using features in parent to avoid a copy
	FastArray<TupleDesc_F64> combinedFeatures = new FastArray<>(TupleDesc_F64.class);

	// feature degree of freedom
	int dof;

	/**
	 * Configures and specifies internal algorithms
	 *
	 * @param scaleSpace scale space computation and storage
	 * @param detector feature detector
	 * @param factoryOrientation Creates new orientation estimators to use in each thread
	 * @param factoryDescribe Creates new descriptor computers for use in each thread
	 */
	public CompleteSift_MT( SiftScaleSpace scaleSpace, SiftDetector detector,
							BoofLambdas.Factory<OrientationHistogramSift<GrayF32>> factoryOrientation,
							BoofLambdas.Factory<DescribePointSift<GrayF32>> factoryDescribe ) {
		super(scaleSpace, detector, factoryOrientation.newInstance(), factoryDescribe.newInstance());

		this.dof = super.describe.getDescriptorLength();
		helpers = new GrowArray<>(() -> new ThreadHelper(factoryOrientation.newInstance(), factoryDescribe.newInstance()));
	}

	@Override public void process( GrayF32 input ) {
		combinedFeatures.reset();
		super.process(input);
	}

	@Override protected void describeDetections( List<SiftDetector.SiftPoint> detections ) {
		// if there are very few features don't spawn threads
		if (minimumDetectionsThread >= detections.size())
			super.describeDetections(detections);

		BoofConcurrency.loopBlocks(0, detections.size(), helpers, ( helper, idx0, idx1 ) -> {

			final OrientationHistogramSift<GrayF32> orientation = helper.orientation;
			final DescribePointSift<GrayF32> describe = helper.describe;

			helper.reset();

			for (int detIdx = idx0; detIdx < idx1; detIdx++) {
				SiftDetector.SiftPoint p = detections.get(detIdx);

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
					describe.process(localX, localY, localSigma, angles.get(angleIdx), helper.features.grow());
					helper.orientations.add(angles.get(angleIdx));
					helper.locations.add(p);
				}
			}
		});

		// Stitch results from all the threads back together
		for (int i = 0; i < helpers.size(); i++) {
			ThreadHelper helper = helpers.get(i);

			locations.addAll(helper.locations);
			combinedFeatures.addAll(helper.features);
			orientations.addAll(helper.orientations);
		}
	}

	@Override public FastAccess<TupleDesc_F64> getDescriptions() {
		return combinedFeatures;
	}

	/**
	 * Contains data needed for each thread to run independently
	 */
	private class ThreadHelper {
		public final OrientationHistogramSift<GrayF32> orientation;
		public final DescribePointSift<GrayF32> describe;

		// Results for each detection
		DogArray<TupleDesc_F64> features = new DogArray<>(() -> new TupleDesc_F64(dof));
		FastArray<ScalePoint> locations = new FastArray<>(ScalePoint.class);
		DogArray_F64 orientations = new DogArray_F64();

		public ThreadHelper( OrientationHistogramSift<GrayF32> orientation,
							 DescribePointSift<GrayF32> describe ) {
			this.orientation = orientation;
			this.describe = describe;
		}

		public void reset() {
			features.reset();
			locations.reset();
			orientations.reset();
		}
	}
}

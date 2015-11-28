/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.describe.DescribePointSiftLowe;
import boofcv.alg.feature.detect.interest.SiftDetector2;
import boofcv.alg.feature.detect.interest.SiftScaleSpace2;
import boofcv.alg.feature.orientation.OrientationHistogramSift2;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageFloat32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * SIFT combined together to simultaneously detect and describe the key points it finds.  Memory is conserved by
 * only having one octave of the scale-space in memory at any given time.
 *
 * @see OrientationHistogramSift2
 * @see DescribePointSiftLowe
 * @see SiftDetector2
 *
 * @author Peter Abeles
 */
public class CompleteSift2 extends SiftDetector2
{
	// estimate orientation
	OrientationHistogramSift2<ImageFloat32> orientation;
	// describes the keypoints
	DescribePointSiftLowe<ImageFloat32> describe;
	// storage for found features
	FastQueue<BrightFeature> features;
	// found orientations and feature locations
	FastQueue<ScalePoint> locations = new FastQueue<ScalePoint>(ScalePoint.class,false);
	GrowQueue_F64 orientations = new GrowQueue_F64();

	// used to compute the image gradient
	ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.three_F32();

	// spacial derivative for the current scale in the octave
	ImageFloat32 derivX = new ImageFloat32(1,1);
	ImageFloat32 derivY = new ImageFloat32(1,1);

	/**
	 * Configures SIFT
	 *
	 * @param scaleSpace Scale-space that features are computed inside of
	 * @param edgeR Edge threshold.  See {@link SiftDetector2#SiftDetector2(SiftScaleSpace2, double, NonMaxLimiter)}
	 * @param extractor Finds minimums and maximums.  See {@link SiftDetector2#SiftDetector2(SiftScaleSpace2, double, NonMaxLimiter)}
	 * @param orientation Estimates feature orientation(s)
	 * @param describe Describes a SIFT feature
	 */
	public CompleteSift2(SiftScaleSpace2 scaleSpace, double edgeR, NonMaxLimiter extractor,
						 OrientationHistogramSift2<ImageFloat32> orientation, DescribePointSiftLowe<ImageFloat32> describe) {
		super(scaleSpace, edgeR, extractor);

		this.orientation = orientation;
		this.describe = describe;

		final int dof = describe.getDescriptorLength();
		features = new FastQueue<BrightFeature>(BrightFeature.class,true) {
			@Override
			protected BrightFeature createInstance() {
				return new BrightFeature(dof);
			}
		};
	}

	@Override
	public void process(ImageFloat32 input) {
		features.reset();
		locations.reset();
		orientations.reset();
		super.process(input);
	}

	@Override
	protected void detectFeatures(int scaleIndex) {

		// compute image derivative for this scale
		ImageFloat32 input = scaleSpace.getImageScale(scaleIndex);
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);
		gradient.process(input,derivX,derivY);

		// set up the orientation and description algorithms
		orientation.setImageGradient(derivX,derivY);
		describe.setImageGradient(derivX,derivY);

		super.detectFeatures(scaleIndex);
	}

	@Override
	protected void handleDetection(ScalePoint p) {

		// adjust the image for the down sampling in each octave
		double localX = p.x / pixelScaleToInput;
		double localY = p.y / pixelScaleToInput;
		double localSigma = p.scale / pixelScaleToInput;

		// find potential orientations first
		orientation.process(localX,localY,localSigma);

		// describe each feature
		GrowQueue_F64 angles = orientation.getOrientations();
		for (int i = 0; i < angles.size; i++) {
			BrightFeature feature = features.grow();
			feature.white = p.white;
			describe.process(localX,localY,localSigma,angles.get(i),feature);

			orientations.add(angles.get(i));
			locations.add(p);
		}
	}

	public FastQueue<ScalePoint> getLocations() {
		return locations;
	}

	public FastQueue<BrightFeature> getDescriptions() {
		return features;
	}

	public GrowQueue_F64 getOrientations() {
		return orientations;
	}

	public int getDescriptorLength() {
		return describe.getDescriptorLength();
	}
}

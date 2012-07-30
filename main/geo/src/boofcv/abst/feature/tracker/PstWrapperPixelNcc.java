/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.tracker;


import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.feature.NccFeatureQueue;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {{@link boofcv.alg.feature.describe.DescribePointPixelRegionNCC}.
 *
 * @author Peter Abeles
 */
public class PstWrapperPixelNcc<I extends ImageSingleBand>
		extends DetectAssociateTracker<I,NccFeature>
{
	private DescribePointPixelRegionNCC<I> alg;
	private InterestPointDetector<I> detector;
	private GeneralAssociation<NccFeature> association;

	public PstWrapperPixelNcc(DescribePointPixelRegionNCC<I> alg,
							  InterestPointDetector<I> detector,
							  GeneralAssociation<NccFeature> association) {
		this.alg = alg;
		this.detector = detector;
		this.association = association;

		setUpdateState(true);
	}

	@Override
	public void setInputImage(I input) {
		detector.detect(input);
		alg.setImage(input);
	}

	@Override
	public FastQueue<NccFeature> createFeatureDescQueue( boolean declareData  ) {
		if( declareData )
			return new NccFeatureQueue(alg.getDescriptorLength());
		else
			return new FastQueue<NccFeature>(100,NccFeature.class,false);
	}

	@Override
	public NccFeature createDescription() {
		return new NccFeature(alg.getDescriptorLength());
	}

	@Override
	public void detectFeatures(FastQueue<Point2D_F64> location,
							   FastQueue<NccFeature> description) {
		int N = detector.getNumberOfFeatures();

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = detector.getLocation(i);

			if( alg.isInBounds((int) p.x, (int) p.y) ) {
				alg.process((int)p.x,(int)p.y,description.pop());

				location.pop().set(p.x, p.y);
			}
		}
	}

	@Override
	public FastQueue<AssociatedIndex> associate(FastQueue<NccFeature> featSrc, FastQueue<NccFeature> featDst) {
		association.associate(featSrc,featDst);
		return association.getMatches();
	}

	@Override
	protected void setDescription(NccFeature src, NccFeature dst) {
		src.setTo(dst);
	}
}

/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.brief.BriefFeature;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BriefFeatureQueue;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

/**
 * Wrapper around {{@link DescribePointBrief}.
 *
 * @author Peter Abeles
 */
public class PstWrapperBrief <I extends ImageBase>
		extends DetectAssociateTracker<I,BriefFeature>
{
	private DescribePointBrief<I> alg;
	private InterestPointDetector<I> detector;
	private GeneralAssociation<BriefFeature> association;

	public PstWrapperBrief(DescribePointBrief<I> alg,
						   InterestPointDetector<I> detector,
						   GeneralAssociation<BriefFeature> association) {
		this.alg = alg;
		this.detector = detector;
		this.association = association;
	}

	@Override
	public void setInputImage(I input) {
		detector.detect(input);
		alg.setImage(input);
	}

	@Override
	public FastQueue<BriefFeature> createFeatureDescQueue() {
		return new BriefFeatureQueue(alg.getDefinition().getLength());
	}

	@Override
	public void detectFeatures(FastQueue<Point2D_I32> location,
							   FastQueue<BriefFeature> description) {
		int N = detector.getNumberOfFeatures();

		Point2D_I32 lp = location.pop();
		BriefFeature f = description.pop();
		for( int i = 0; i < N; i++ ) {
			Point2D_I32 p = detector.getLocation(i);

			if( alg.process(p.x,p.y,f) ) {
				lp.set(p.x,p.y);
				// create data for the next feature
				f = description.pop();
				lp = location.pop();
			}
		}
		// the tail hasn't been used
		location.removeTail();
		description.removeTail();
	}

	@Override
	public FastQueue<AssociatedIndex> associate(FastQueue<BriefFeature> featSrc, FastQueue<BriefFeature> featDst) {
		association.associate(featSrc,featDst);
		return association.getMatches();
	}
}

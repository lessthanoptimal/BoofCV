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
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BriefFeatureQueue;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {{@link DescribePointBrief}.
 *
 * @author Peter Abeles
 */
public class PstWrapperBrief <I extends ImageSingleBand>
		extends DetectAssociateTracker<I,TupleDesc_B>
{
	private DescribePointBrief<I> alg;
	private InterestPointDetector<I> detector;
	private GeneralAssociation<TupleDesc_B> association;

	public PstWrapperBrief(DescribePointBrief<I> alg,
						   InterestPointDetector<I> detector,
						   GeneralAssociation<TupleDesc_B> association) {
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
	public FastQueue<TupleDesc_B> createFeatureDescQueue(  boolean declareData  ) {
		if( declareData )
			return new BriefFeatureQueue(alg.getDefinition().getLength());
		else
			return new FastQueue<TupleDesc_B>(100,TupleDesc_B.class,false);
	}

	@Override
	public TupleDesc_B createDescription() {
		return new TupleDesc_B(alg.getDefinition().getLength());
	}

	@Override
	public void detectFeatures(FastQueue<Point2D_F64> location,
							   FastQueue<TupleDesc_B> description) {
		int N = detector.getNumberOfFeatures();

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = detector.getLocation(i);

			if( alg.isInBounds((int) p.x, (int) p.y) ) {
				alg.process((int) p.x, (int) p.y, description.pop());

				location.pop().set(p.x,p.y);
			}
		}
	}

	@Override
	public FastQueue<AssociatedIndex> associate(FastQueue<TupleDesc_B> featSrc, FastQueue<TupleDesc_B> featDst) {
		association.associate(featSrc,featDst);
		return association.getMatches();
	}

	@Override
	protected void setDescription(TupleDesc_B src, TupleDesc_B dst) {
		System.arraycopy(dst.data,0,src.data,0,src.data.length);
	}
}

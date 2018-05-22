/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;


/**
 * <p>
 * Implementation of {@link DdaFeatureManager} which uses the {@link DetectDescribePoint} interface for extracting
 * features from an image.
 * </p>
 *
 * @author Peter Abeles
 */
public class DdaManagerDetectDescribePoint<I extends ImageGray<I>, Desc extends TupleDesc>
		implements DdaFeatureManager<I, Desc> {

	// Feature detector and describer
	protected DetectDescribePoint<I, Desc> detDesc;

	/**
	 * Configures tracker
	 *
	 * @param detDesc Feature detector and descriptor
	 */
	public DdaManagerDetectDescribePoint(final DetectDescribePoint<I, Desc> detDesc) {
		this.detDesc = detDesc;
	}

	@Override
	public Desc createDescription() {
		return detDesc.createDescription();
	}

	@Override
	public Class<Desc> getDescriptionType() {
		return detDesc.getDescriptionType();
	}

	@Override
	public void detectFeatures(I input) {
		detDesc.detect(input);
	}

	@Override
	public void getFeatures(int set, FastQueue<Point2D_F64> locations, FastQueue<Desc> descriptions) {
		if( set != 0 )
			throw new RuntimeException("Set must be 0");

		int N = detDesc.getNumberOfFeatures();
		for( int i = 0; i < N; i++ ) {
			locations.add( detDesc.getLocation(i) );
			descriptions.add( detDesc.getDescription(i) );
		}
	}

	@Override
	public int getNumberOfSets() {
		return 1;
	}
}

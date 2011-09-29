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

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.brief.BriefFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class WrapDescribeBrief<T extends ImageBase> implements ExtractFeatureDescription<T> {

	int length;
	DescribePointBrief alg;
	BriefFeature feature;

	public WrapDescribeBrief( DescribePointBrief<T> alg ) {
		this.alg = alg;
		this.length = alg.getDefinition().getLength();
		feature = new BriefFeature(length);
	}

	@Override
	public void setImage(T image) {
		alg.setImage((ImageFloat32)image);
	}

	@Override
	public int getDescriptionLength() {
		return length;
	}

	@Override
	public TupleDesc_F64 process(int x, int y, double orientation, double scale, TupleDesc_F64 ret) {

		if( !alg.process(x,y,feature) )
			return null;

		if( ret == null )
			ret = new TupleDesc_F64(length);

		for( int i = 0; i < length; i++ ) {
			int index = i/32;
			int bit = i%32;

			if( ((feature.data[index] >> bit) & 0x01) == 1 ) {
				ret.value[i] = 1;
			} else {
				ret.value[i] = -1;
			}
		}

		return ret;
	}

	@Override
	public boolean requiresScale() {
		return false;
	}

	@Override
	public boolean requiresOrientation() {
		return false;
	}
}

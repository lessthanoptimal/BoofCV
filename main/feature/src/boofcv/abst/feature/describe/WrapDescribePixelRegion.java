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

import boofcv.alg.feature.describe.DescribePointPixelRegion;
import boofcv.alg.feature.describe.impl.ImplDescribePointPixelRegion_F32;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;

/**
 * Wrapper around {@link boofcv.alg.feature.describe.DescribePointPixelRegion} for
 * {@link DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class WrapDescribePixelRegion<T extends ImageBase, D extends TupleDesc>
		implements DescribeRegionPoint<T>
{
	DescribePointPixelRegion<T,D> alg;

	D desc;

	public WrapDescribePixelRegion(DescribePointPixelRegion<T, D> alg) {
		this.alg = alg;

		if( alg instanceof ImplDescribePointPixelRegion_F32 ) {
			desc = (D)new TupleDesc_F32(alg.getDescriptorLength());
		}
	}

	@Override
	public void setImage(T image) {
		alg.setImage(image);
	}

	@Override
	public int getDescriptionLength() {
		return alg.getDescriptorLength();
	}

	@Override
	public int getCanonicalRadius() {
		return alg.getDescriptorRadius();
	}

	@Override
	public TupleDesc_F64 process(double x, double y, double orientation,
								 double scale, TupleDesc_F64 ret)
	{
		if( ret == null ) {
			ret = new TupleDesc_F64(alg.getDescriptorLength());
		}

		alg.process((int)x,(int)y,desc);

		if( desc instanceof TupleDesc_F32 ) {
			TupleDesc_F32 d = (TupleDesc_F32)desc;
			for( int i = 0; i < d.value.length; i++ ) {
				ret.value[i] = d.value[i];
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

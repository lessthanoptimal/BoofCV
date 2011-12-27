/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageSingleBand;

/**
 * Wrapper around {@link boofcv.alg.feature.describe.DescribePointPixelRegionNCC} for
 * {@link boofcv.abst.feature.describe.DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class WrapDescribePixelRegionNCC<T extends ImageSingleBand>
		implements DescribeRegionPoint<T>
{
	DescribePointPixelRegionNCC<T> alg;

	NccFeature desc;

	public WrapDescribePixelRegionNCC(DescribePointPixelRegionNCC<T> alg) {
		this.alg = alg;
		desc = new NccFeature( alg.getDescriptorLength() );
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
		if( alg.process((int)x,(int)y,desc) ) {
			if( ret == null ) {
				ret = new TupleDesc_F64(alg.getDescriptorLength());
			}

			System.arraycopy(desc.value,0,ret.value,0,desc.value.length);
			return ret;
		} else {
			return null;
		}
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

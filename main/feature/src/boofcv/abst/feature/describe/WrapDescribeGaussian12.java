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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribePointGaussian12;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageSingleBand;


/**
 * Wrapper around {@link DescribePointGaussian12} for {@link DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class WrapDescribeGaussian12<T extends ImageSingleBand, D extends ImageSingleBand>
		extends WrapScaleToCharacteristic<T,D>
{
	DescribePointGaussian12<T,?> steer;

	public WrapDescribeGaussian12(DescribePointGaussian12<T,?> steer,
								 ImageGradient<T, D> gradient,
								 Class<T> inputType ,
								 Class<D> derivType ) {
		super(steer.getRadius(),gradient,inputType,derivType);
		this.steer = steer;
	}

	@Override
	public int getCanonicalRadius() {
		return steer.getRadius();
	}

	@Override
	public int getDescriptionLength() {
		return steer.getDescriptionLength();
	}

	@Override
	protected TupleDesc_F64 describe(int x, int y, double angle, TupleDesc_F64 ret) {
		steer.setImage(scaledImage);
		return steer.describe(x,y,angle,ret);
	}
}

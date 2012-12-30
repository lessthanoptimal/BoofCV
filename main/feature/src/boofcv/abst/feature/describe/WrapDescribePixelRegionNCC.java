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

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.ImageSingleBand;

/**
 * Wrapper around {@link boofcv.alg.feature.describe.DescribePointPixelRegionNCC} for
 * {@link boofcv.abst.feature.describe.DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class WrapDescribePixelRegionNCC<T extends ImageSingleBand>
		implements DescribeRegionPoint<T,NccFeature>
{
	DescribePointPixelRegionNCC<T> alg;

	public WrapDescribePixelRegionNCC(DescribePointPixelRegionNCC<T> alg) {
		this.alg = alg;
	}

	@Override
	public NccFeature createDescription() {
		return new NccFeature(alg.getDescriptorLength());
	}

	@Override
	public void setImage(T image) {
		alg.setImage(image);
	}

	@Override
	public int getDescriptorLength() {
		return alg.getDescriptorLength();
	}

	@Override
	public boolean isInBounds(double x, double y, double orientation, double scale) {
		return alg.isInBounds((int)x,(int)y);
	}

	@Override
	public NccFeature process(double x, double y, double orientation,
						   double scale, NccFeature ret)
	{
		if( ret == null )
			ret = createDescription();

		alg.process((int)x,(int)y,ret);

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

	@Override
	public Class<NccFeature> getDescriptorType() {
		return NccFeature.class;
	}
}

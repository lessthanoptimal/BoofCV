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

import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public class WrapDescribeSurf<T extends ImageBase, II extends ImageBase>
		implements ExtractFeatureDescription<T> {

	DescribePointSurf<II> surf;
	OrientationIntegral<II> orientation;
	II ii;

	public WrapDescribeSurf(DescribePointSurf<II> surf ,
							OrientationIntegral<II> orientation )
	{
		this.surf = surf;
		this.orientation = orientation;
	}

	@Override
	public void setImage(T image) {
		if( ii != null ) {
			ii.reshape(image.width,image.height);
		}
		ii = GIntegralImageOps.transform(image,ii);
		orientation.setImage(ii);
		surf.setImage(ii);
	}

	@Override
	public int getDescriptionLength() {
		return surf.getDescriptionLength();
	}

	@Override
	public TupleDesc_F64 process(int x, int y, double scale, TupleDesc_F64 ret) {

		double angle = 0;

		if( orientation != null )
			angle = orientation.compute(x,y);

		SurfFeature f = surf.describe(x,y,scale,angle,null);
		if( f == null)
			return null;
		return f.features;
	}
}

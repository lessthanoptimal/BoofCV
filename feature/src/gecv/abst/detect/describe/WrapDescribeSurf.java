/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.describe;

import gecv.alg.feature.describe.DescribePointSURF;
import gecv.alg.feature.orientation.OrientationIntegral;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.struct.feature.SurfFeature;
import gecv.struct.feature.TupleFeature_F64;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public class WrapDescribeSurf<T extends ImageBase> implements ExtractFeatureDescription<T> {

	DescribePointSURF<T> surf;
	OrientationIntegral<T> orientation;
	T ii;

	public WrapDescribeSurf(DescribePointSURF<T> surf ,
							OrientationIntegral<T> orientation )
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
	public TupleFeature_F64 process(int x, int y, double scale) {

		double angle = 0;

		if( orientation != null )
			angle = orientation.compute(x,y);

		SurfFeature f = surf.describe(x,y,scale,angle,null);
		if( f == null)
			return null;
		return f.features;
	}
}

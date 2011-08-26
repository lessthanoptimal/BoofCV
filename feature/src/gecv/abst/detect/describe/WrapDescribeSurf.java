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
import gecv.alg.feature.describe.SurfFeature;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.struct.feature.TupleFeature_F64;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public class WrapDescribeSurf<T extends ImageBase> implements ExtractFeatureDescription<T> {

	DescribePointSURF<T> surf;
	T ii;

	public WrapDescribeSurf(DescribePointSURF<T> surf) {
		this.surf = surf;
	}

	@Override
	public void setImage(T image) {
		if( ii != null ) {
			ii.reshape(image.width,image.height);
		}
		ii = GIntegralImageOps.transform(image,ii);
	}

	@Override
	public TupleFeature_F64 process(int x, int y, double scale) {
		surf.setImage(ii);
		SurfFeature f = surf.describe(x,y,scale);
		if( f == null)
			return null;
		return f.features;
	}
}

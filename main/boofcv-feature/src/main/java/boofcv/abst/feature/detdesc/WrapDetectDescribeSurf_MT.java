/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detdesc;

import boofcv.BoofDefaults;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageGray;

/**
 * Concurrent implementations of {@link WrapDetectDescribeSurf}.
 *
 * @author Peter Abeles
 */
public class WrapDetectDescribeSurf_MT<T extends ImageGray<T>, II extends ImageGray<II>>
		extends WrapDetectDescribeSurf<T,II> {

	public WrapDetectDescribeSurf_MT(FastHessianFeatureDetector<II> detector,
									 OrientationIntegral<II> orientation,
									 DescribePointSurf<II> describe) {
		super(detector, orientation, describe);
	}

	@Override
	protected void computeDescriptors() {
		BoofConcurrency.loopBlocks(0,foundPoints.size(),(i0, i1)->{
			OrientationIntegral<II> orientation = (OrientationIntegral)this.orientation.copy();
			DescribePointSurf<II> describe = this.describe.copy();

			orientation.setImage(ii);
			describe.setImage(ii);

			for( int i = i0; i < i1; i++ ) {
				ScalePoint p = foundPoints.get(i);
				double radius = p.scale*BoofDefaults.SURF_SCALE_TO_RADIUS;

				orientation.setObjectRadius(radius);
				double angle = orientation.compute(p.x,p.y);
				describe.describe(p.x,p.y, angle, p.scale, features.get(i));
				featureAngles.set(i,angle);
			}
		});
	}
}

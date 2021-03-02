/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detdesc;

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * Multi-threaded version of {@link DetectDescribeSurfPlanar}
 *
 * @author Peter Abeles
 */
public class DetectDescribeSurfPlanar_MT<II extends ImageGray<II>> extends DetectDescribeSurfPlanar<II> {
	public DetectDescribeSurfPlanar_MT( FastHessianFeatureDetector<II> detector,
										OrientationIntegral<II> orientation, DescribePointSurfPlanar<II> describe ) {
		super(detector, orientation, describe);
	}

	@Override
	protected void describe( II grayII, Planar<II> colorII ) {
		BoofConcurrency.loopBlocks(0, foundPoints.size(), ( i0, i1 ) -> {
			OrientationIntegral<II> orientation = (OrientationIntegral)this.orientation.copy();
			DescribePointSurfPlanar<II> describe = this.describe.copy();

			orientation.setImage(grayII);
			describe.setImage(grayII, colorII);

			for (int i = i0; i < i1; i++) {
				ScalePoint p = foundPoints.get(i);
				orientation.setObjectRadius(p.scale);
				double angle = orientation.compute(p.pixel.x, p.pixel.y);

				describe.describe(p.pixel.x, p.pixel.y, angle, p.scale, descriptions.get(i));

				featureAngles.set(i, angle);
			}
		});
	}
}

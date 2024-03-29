/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.feature.describe.FactoryDescribePointRadiusAngle;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

class TestDescribeSurfPlanar_RadiusAngle extends GenericDescribePointRadiusAngleChecks<Planar<GrayF32>, TupleDesc_F64> {
	TestDescribeSurfPlanar_RadiusAngle() {
		super(ImageType.pl(3,GrayF32.class));
	}
	@Override
	protected DescribePointRadiusAngle<Planar<GrayF32>, TupleDesc_F64> createAlg() {
		return FactoryDescribePointRadiusAngle.surfColorStable(null, imageType);
	}
}

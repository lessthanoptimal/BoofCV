/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.intensity;

import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestWrapperMedianCornerIntensity extends ChecksGeneralFeatureIntensity{
	public TestWrapperMedianCornerIntensity() {
		addTypes(ImageFloat32.class,ImageFloat32.class);
		addTypes(ImageUInt8.class, ImageSInt16.class);
	}

	@Override
	public GeneralFeatureIntensity<ImageFloat32, ImageFloat32> createAlg(Class imageType, Class derivType) {
		return FactoryIntensityPoint.median(2,imageType);
	}
}

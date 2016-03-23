/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * Provides some basic functionality for implementing {@link GeneralFeatureIntensity}.
 *
 * @author Peter Abeles
 */
public abstract class BaseGeneralFeatureIntensity <I extends ImageGray, D extends ImageGray>
		implements GeneralFeatureIntensity<I,D>
{
	GrayF32 intensity = new GrayF32(1,1);


	public void init( int width , int height) {
		if( intensity.width != width || intensity.height != height ) {
			intensity.reshape(width,height);
			// zero the image to make sure it's borders values are zero
			GImageMiscOps.fill(intensity, 0);
		}
	}

	@Override
	public GrayF32 getIntensity() {
		return intensity;
	}
}

/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.transform.census;

import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.abst.transform.census.FilterCensusTransformD55S32;
import boofcv.alg.transform.census.GCensusTransform;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class TestFilterCensusTransformD55S32 extends GenericFilterCensusTransformChecks<GrayU8,GrayS32> {

	public TestFilterCensusTransformD55S32() {
		super(GrayU8.class);
		this.radius = 2;
	}

	@Override
	public FilterCensusTransform<GrayU8, GrayS32> createAlg(ImageBorder<GrayU8> border) {
		return new FilterCensusTransformD55S32<>(border,GrayU8.class);
	}

	@Override
	public void callFunction(GrayU8 input, GrayS32 output) {
		GCensusTransform.dense5x5(input,output,border);
	}
}

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

package boofcv.alg.feature.describe;

import boofcv.struct.image.GrayF32;


/**
 * @author Peter Abeles
 */
public class TestDescribePointSurfMod  extends BaseTestDescribeSurf<GrayF32,GrayF32> {

	public TestDescribePointSurfMod() {
		super(GrayF32.class,GrayF32.class);
	}

	@Override
	public DescribePointSurf<GrayF32> createAlg() {
		return new DescribePointSurfMod<>(GrayF32.class);
	}
}

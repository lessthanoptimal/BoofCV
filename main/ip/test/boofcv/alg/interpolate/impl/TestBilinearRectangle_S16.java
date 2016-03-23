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

package boofcv.alg.interpolate.impl;

import boofcv.struct.image.GrayS16;


/**
 * @author Peter Abeles
 */
public class TestBilinearRectangle_S16 extends GeneralBilinearRectangleChecks<GrayS16>{


	public TestBilinearRectangle_S16() {
		super(GrayS16.class);
	}

	@Override
	protected GrayS16 createImage(int width, int height) {
		return new GrayS16(width,height);
	}
}

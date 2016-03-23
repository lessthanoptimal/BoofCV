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

package boofcv.alg.transform.ii.impl;

import boofcv.alg.filter.derivative.GeneralSparseSampleTests;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestSparseIntegralSample_F32 extends GeneralSparseSampleTests<GrayF32> {

	private static int baseR = 2;
	
	public TestSparseIntegralSample_F32() {
		super(GrayF32.class, new SparseIntegralSample_F32(),-baseR-1,-baseR-1,baseR,baseR);
		((SparseIntegralSample_F32)alg).setWidth(baseR*2+1);
	}

	@Test
	public void testAll() {
		super.performAllTests();
	}
}

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

package boofcv.alg.geo.h;

import boofcv.struct.geo.PairLineNorm;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestHomographyInducedStereo2Line extends CommonHomographyInducedPlane {

	@Test
	public void perfectData() {

		PairLineNorm l1 = convert(p1,p2);
		PairLineNorm l2 = convert(p1,p3);

		HomographyInducedStereo2Line alg = new HomographyInducedStereo2Line();
		alg.setFundamental(F,e2);
		assertTrue(alg.process(l1, l2));

		checkHomography(alg.getHomography());
	}
}

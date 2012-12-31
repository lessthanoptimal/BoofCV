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

package boofcv.abst.feature.interest;

import boofcv.abst.feature.detect.interest.WrapSiftDetector;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestWrapSiftDetector {

	@Test
	public void standard() {
		SiftDetector alg = FactoryInterestPointAlgs.siftDetector(null);
		SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f, 5, 4, false);

		WrapSiftDetector wrapper = new WrapSiftDetector(alg,ss);

		new GeneralInterestPointDetectorChecks<ImageFloat32>(wrapper,false,true,ImageFloat32.class){}.performAllTests();
	}

	@Test
	public void doubleInput() {
		SiftDetector alg = FactoryInterestPointAlgs.siftDetector(null);
		SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f, 5, 4, true);
		WrapSiftDetector wrapper = new WrapSiftDetector(alg,ss);

		new GeneralInterestPointDetectorChecks<ImageFloat32>(wrapper,false,true,ImageFloat32.class){}.performAllTests();
	}
}

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

package boofcv.abst.feature.interest;

import boofcv.abst.feature.detect.interest.WrapSiftDetector;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestWrapSiftDetector {

	Class types[] = new Class[]{GrayU8.class,GrayF32.class};

	@Test
	public void testAllImageTypes() {
		for( Class type : types ) {
			SiftDetector detector = FactoryInterestPointAlgs.sift(null,null);
			WrapSiftDetector alg = new WrapSiftDetector(detector,type);

			new GeneralInterestPointDetectorChecks(alg,false,true,type){}.performAllTests();
		}
	}
}
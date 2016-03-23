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

package boofcv.abst.feature.detdesc;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("ALL")
public class TestDetectDescribe_CompleteSift
{
	Class types[] = new Class[]{GrayF32.class,GrayU8.class};

	@Test
	public void allTypes() {
		for( final Class type : types ) {
			final Class derivType = GImageDerivativeOps.getDerivativeType(type);
			new GenericTestsDetectDescribePoint(true,true,ImageType.single(type),BrightFeature.class) {

				@Override
				public DetectDescribePoint createDetDesc() {
					return FactoryDetectDescribe.sift(null);
				}
			}.allTests();
		}
	}
}
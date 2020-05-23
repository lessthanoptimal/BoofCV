/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("ALL")
public class TestDetectDescribe_CompleteSift
{
	@Nested
	public class U8 extends GenericTestsDetectDescribePoint {
		protected U8() {
			super(true,true,ImageType.SB_U8,BrightFeature.class);
		}

		@Override
		public DetectDescribePoint createDetDesc() {
			return FactoryDetectDescribe.sift(null,GrayU8.class);
		}
	}

	@Nested
	public class F32 extends GenericTestsDetectDescribePoint {
		protected F32() {
			super(true,true,ImageType.SB_F32,BrightFeature.class);
		}

		@Override
		public DetectDescribePoint createDetDesc() {
			return FactoryDetectDescribe.sift(null,GrayF32.class);
		}
	}
}
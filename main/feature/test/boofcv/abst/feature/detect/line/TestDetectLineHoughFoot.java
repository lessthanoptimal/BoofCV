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

package boofcv.abst.feature.detect.line;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestDetectLineHoughFoot extends GeneralDetectLineTests {


	public TestDetectLineHoughFoot() {
		super(GrayU8.class,GrayF32.class);
	}

	@Override
	public <T extends ImageGray>
	DetectLine<T> createAlg(Class<T> imageType) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		return FactoryDetectLineAlgs.houghFoot(new ConfigHoughFoot(2, 3, 2, 10, 10), imageType, derivType);
	}
}

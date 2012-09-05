/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.WrapFPtoInterestPoint;
import boofcv.alg.feature.detect.interest.FeaturePyramid;
import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestWrapFPtoInterestPoint extends GeneralInterestPointDetectorChecks {

	Class imageType = ImageUInt8.class;
	Class derivType = ImageSInt16.class;

	double scales[] = new double[]{1.0,2.0,3.0,4.0};

	FeaturePyramid fp = FactoryInterestPointAlgs.hessianPyramid(3, 1, 200, imageType, derivType);

	ScaleSpacePyramid ss = new ScaleSpacePyramid(imageType,scales);

	public TestWrapFPtoInterestPoint() {
		configure(new WrapFPtoInterestPoint(fp, ss), false, true, imageType);
	}
}

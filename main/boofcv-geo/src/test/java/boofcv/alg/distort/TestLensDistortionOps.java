/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.alg.distort.impl.ImplImageDistortCache_I8;
import boofcv.alg.interpolate.impl.ImplBilinearPixel_U8;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLensDistortionOps {

	/**
	 * This is a bit hard to test accurately. That would require computing distorted image and seeing everything lines
	 * up properly. For now we just check tos ee if things blow up.
	 */
	@Test
	public void changeCameraModel() {
		CameraPinholeBrown original = new CameraPinholeBrown(200,200,0,200,200,400,400);
		CameraPinhole desired = new CameraPinholeBrown(300,300,0,200,200,400,400);
		CameraPinhole modified = new CameraPinhole();
		BorderType[] borders = new BorderType[]{
				BorderType.EXTENDED,BorderType.SKIP,BorderType.ZERO,BorderType.REFLECT,BorderType.WRAP};

		for( AdjustmentType adj : AdjustmentType.values() ) {
			for( BorderType border : borders ) {
				ImageDistort<GrayU8, GrayU8> alg = LensDistortionOps.changeCameraModel(
						adj, border,original,desired,modified, ImageType.single(GrayU8.class));

				// do a few more tests to see of dubious value. if the underlying implementation changes
				// this test will need to be updated
				assertTrue(alg instanceof ImplImageDistortCache_I8);
				ImplImageDistortCache_I8 _alg = (ImplImageDistortCache_I8)alg;
				assertTrue(_alg.getInterp() instanceof ImplBilinearPixel_U8);
			}
		}
	}
}

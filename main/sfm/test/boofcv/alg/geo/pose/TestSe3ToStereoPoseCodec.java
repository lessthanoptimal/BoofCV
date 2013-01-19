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

package boofcv.alg.geo.pose;

import boofcv.struct.sfm.StereoPose;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSe3ToStereoPoseCodec {

	StereoPose model = new StereoPose();

	@Test
	public void basic() {
		Helper helper = new Helper();
		Se3ToStereoPoseCodec alg = new Se3ToStereoPoseCodec(helper);

		alg.decode(null,model);
		assertTrue(helper.calledDecode);
		alg.encode(model,null);
		assertTrue(helper.calledEncode);
	}

	protected class Helper implements ModelCodec<Se3_F64> {

		boolean calledDecode = false;
		boolean calledEncode = false;

		@Override
		public void decode(double[] input, Se3_F64 outputModel) {
			calledDecode = true;
			assertTrue(model.worldToCam0 == outputModel);
		}

		@Override
		public void encode(Se3_F64 inputModel, double[] output) {
			calledEncode = true;
			assertTrue(model.worldToCam0 == inputModel);
		}

		@Override
		public int getParamLength() {
			return 6;
		}
	}
}

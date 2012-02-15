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

package boofcv.alg.geo.d2.stabilization;

import boofcv.struct.image.ImageUInt8;
import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

import static boofcv.alg.geo.d2.stabilization.TestImageMotionPointKey.DummyModelMatcher;
import static boofcv.alg.geo.d2.stabilization.TestImageMotionPointKey.DummyTracker;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMotionStabilizePointKey {

	@Test
	public void checkLargeDistortion() {
		Affine2D_F32 model = new Affine2D_F32();
		Affine2D_F32 computed = new Affine2D_F32(1,0,0,1,2,3);
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Affine2D_F32> matcher = new DummyModelMatcher<Affine2D_F32>(computed,20);

		ImageUInt8 input = new ImageUInt8(20,30);

		MotionStabilizePointKey<ImageUInt8,Affine2D_F32> alg =
				new MotionStabilizePointKey<ImageUInt8,Affine2D_F32>(tracker,matcher,null,model,5,3,10);
		
		// sanity check here
		assertTrue(alg.process(input));
		assertFalse(alg.isKeyFrame());
		assertFalse(alg.isReset());

		// make sure there is a huge motion that should trigger a reset
		matcher.setMotion(new Affine2D_F32(100,0,0,200,2,3));
		assertTrue(alg.process(input));
		assertTrue(alg.isReset());

	}
}

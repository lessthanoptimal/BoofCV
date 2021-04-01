/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
abstract class GenericDescribePointRadiusAngleChecks<T extends ImageBase<T>, TD extends TupleDesc<TD>>
		extends BoofStandardJUnit {
	protected ImageType<T> imageType;

	protected abstract DescribePointRadiusAngle<T, TD> createAlg();

	protected GenericDescribePointRadiusAngleChecks( ImageType<T> imageType ) {
		this.imageType = imageType;
	}

	/**
	 * A crude check to see if it respects the radius size parameter. just checks to see if the descriptor changes
	 * when it hits a certain size
	 */
	@Test
	void radiusSize() {
		T image = imageType.createImage(100, 120);

		int cx = 45;
		int cy = 48;
		int radius = 20;
		int extra = 3;
		int aradius = radius + extra;

		GImageMiscOps.fill(image, 255);
		GImageMiscOps.fill(image.subimage(cx - aradius, cy - aradius, cx + aradius + 1, cy + aradius + 1), 50);
//		image.set(cx,cy,100); // give it some texture so it doesn't amplify noise

		DescribePointRadiusAngle<T, TD> describe = createAlg();
		describe.setImage(image);

		TD t1 = describe.createDescription();
		describe.process(cx, cy, 0, radius, t1);
		TD t2 = describe.createDescription();

		// Moving by 1 pixels should make no difference
		describe.process(cx + 1, cy, 0, radius, t2);
		checkEquals(t1, t2);

		// Move it so that the feature's edge goes past the edge
		describe.process(cx + extra + 2, cy, 0, radius, t2);
		checkNotEquals(t1, t2);
	}

	void checkEquals( TupleDesc<?> a, TupleDesc<?> b ) {
		assertEquals(a.size(), b.size());

		int totalMissMatches = 0;
		for (int i = 0; i < a.size(); i++) {
			if (Math.abs(a.getDouble(i) - b.getDouble(i)) > 0.1) {
				totalMissMatches++;
			}
		}
		assertTrue(totalMissMatches <= a.size()/32);
	}

	void checkNotEquals( TupleDesc<?> a, TupleDesc<?> b ) {
		assertEquals(a.size(), b.size());
		int totalMissMatches = 0;
		for (int i = 0; i < a.size(); i++) {
			if (Math.abs(a.getDouble(i) - b.getDouble(i)) > UtilEjml.EPS) {
				totalMissMatches++;
			}
		}
		// there might be some stray matches
		assertTrue(totalMissMatches > a.size()/32);
	}
}

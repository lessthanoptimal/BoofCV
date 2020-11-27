/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.BoofTesting;
import boofcv.alg.filter.binary.ContourOps;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestBinaryContourFinderLinearExternal extends GenericBinaryContourFinder
{
	Random rand = BoofTesting.createRandom(0);

	public TestBinaryContourFinderLinearExternal() {
		super.supportsInternalContour = false;
	}

	@Override
	protected BinaryContourFinder create() {
		return new BinaryContourFinderLinearExternal();
	}

	@Test
	void compareToChang2004() {
		var binary = new GrayU8(200,190);
		var labeled = new GrayS32(200,190);
		GImageMiscOps.fillUniform(binary,rand,0,1);

		for( var rule : ConnectRule.values() ) {
			var chang = new LinearContourLabelChang2004(rule);
			var alg = new BinaryContourFinderLinearExternal();

			// configure contour tracing so that it should produce identical results
			alg.setConnectRule(rule);
			alg.setCreatePaddedCopy(true);
			alg.setCoordinateAdjustment(1,1);
			chang.setSaveInternalContours(false);

			chang.process(binary.clone(),labeled);
			alg.process(binary);

			DogArray<ContourPacked> expected = chang.getContours();
			List<ContourPacked> found = alg.getContours();

			assertEquals(expected.size, found.size());

			DogArray<Point2D_I32> contourExpected = new DogArray<>(Point2D_I32::new);
			DogArray<Point2D_I32> contourFound = new DogArray<>(Point2D_I32::new);
			for (int i = 0; i < expected.size; i++ ) {
				chang.getPackedPoints().getSet(expected.get(i).externalIndex,contourExpected);
				boolean matched = false;
				for (int j = 0; j < found.size(); j++) {
					alg.loadContour(found.get(i).id,contourFound);
					if(ContourOps.isEquivalent(contourExpected.toList(),contourFound.toList())) {
						matched = true;
						break;
					}
				}
				assertTrue(matched);
			}

		}
	}
}

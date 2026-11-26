/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census;

import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.override.BOverrideManager;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder1D_S32;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestBOverrideCensusTransform extends BoofStandardJUnit {
	GrayU8 image = new GrayU8(10, 8);
	FastAccess<Point2D_I32> samples = CensusTransform.createBlockSamples(1);
	ImageBorder<GrayU8> reflect = FactoryImageBorder.single(BorderType.REFLECT, GrayU8.class);
	ImageBorder<GrayU8> custom = new ImageBorder1D_S32<>(BorderIndex1D_Extend::new);

	List<BorderType> found = new ArrayList<>();

	@AfterEach void clear() {BOverrideManager.clear();}

	@Test void borderType() {
		BOverrideCensusTransform.census = new BOverrideCensusTransform.Census<GrayU8>() {
			@Override public void dense3x3( GrayU8 input, GrayU8 output, BorderType border ) {found.add(border);}

			@Override public void dense5x5( GrayU8 input, GrayS32 output, BorderType border ) {found.add(border);}

			@Override public void sample_S64( GrayU8 input, FastAccess<Point2D_I32> sample, GrayS64 output,
											  BorderType border ) {found.add(border);}

			@Override public void sample_IU16( GrayU8 input, FastAccess<Point2D_I32> sample, InterleavedU16 output,
											   BorderType border ) {found.add(border);}
		};

		for (ImageBorder<GrayU8> border : Arrays.asList(reflect, null)) {
			assertTrue(BOverrideCensusTransform.invokeNativeDense3x3(image, new GrayU8(1, 1), border));
			assertTrue(BOverrideCensusTransform.invokeNativeDense5x5(image, new GrayS32(1, 1), border));
			assertTrue(BOverrideCensusTransform.invokeNativeSample_S64(image, samples, new GrayS64(1, 1), border));
			assertTrue(BOverrideCensusTransform.invokeNativeSample_IU16(image, samples, new InterleavedU16(1, 1, 1), border));
		}
		assertFalse(BOverrideCensusTransform.invokeNativeDense3x3(image, new GrayU8(1, 1), custom));
		assertFalse(BOverrideCensusTransform.invokeNativeDense5x5(image, new GrayS32(1, 1), custom));
		assertFalse(BOverrideCensusTransform.invokeNativeSample_S64(image, samples, new GrayS64(1, 1), custom));
		assertFalse(BOverrideCensusTransform.invokeNativeSample_IU16(image, samples, new InterleavedU16(1, 1, 1), custom));

		BorderType R = BorderType.REFLECT;
		BorderType S = BorderType.SKIP;
		assertEquals(List.of(R, R, R, R, S, S, S, S), found);
	}
}

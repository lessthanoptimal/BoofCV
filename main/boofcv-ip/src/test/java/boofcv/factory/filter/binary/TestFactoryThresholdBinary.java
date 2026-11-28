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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.binary.GlobalFixedBinaryFilter;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.override.BOverrideManager;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFactoryThresholdBinary extends BoofStandardJUnit {
	@AfterEach void clearOverrides() {
		BOverrideManager.clear();
	}

	/// If an override returns null or throws a RuntimeException, the factory creates BoofCV's filter
	@Test void overrideDeclines() throws Exception {
		checkOverrideDeclines(false);
		checkOverrideDeclines(true);
	}

	void checkOverrideDeclines( boolean exception ) throws Exception {
		List<String> called = new ArrayList<>();
		for (Field f : BOverrideFactoryThresholdBinary.class.getFields()) {
			if (!Modifier.isStatic(f.getModifiers()) || !f.getType().isInterface())
				continue;
			f.set(null, Proxy.newProxyInstance(f.getType().getClassLoader(), new Class<?>[]{f.getType()},
					( proxy, method, args ) -> {
						called.add(f.getName());
						if (exception)
							throw new IllegalArgumentException("Unsupported");
						return null;
					}));
		}

		ConfigLength width = ConfigLength.fixed(5);
		Class<GrayU8> type = GrayU8.class;
		assertNotNull(FactoryThresholdBinary.localGaussian(width, 1.0, true, type));
		assertNotNull(FactoryThresholdBinary.localNiblack(width, true, 0.3f, type));
		assertNotNull(FactoryThresholdBinary.localSauvola(width, true, 0.3f, type));
		assertNotNull(FactoryThresholdBinary.localWolf(width, true, 0.3f, type));
		assertNotNull(FactoryThresholdBinary.localNick(width, true, -0.2f, type));
		assertNotNull(FactoryThresholdBinary.localMean(width, 1.0, true, type));
		assertNotNull(FactoryThresholdBinary.localOtsu(width, 1.0, true, false, 0.1, type));
		assertNotNull(FactoryThresholdBinary.blockMinMax(width, 1.0, true, true, 2.0, type));
		assertNotNull(FactoryThresholdBinary.blockMean(width, 1.0, true, true, type));
		assertNotNull(FactoryThresholdBinary.blockOtsu(width, 1.0, true, true, false, 0.1, type));
		assertNotNull(FactoryThresholdBinary.globalEntropy(0, 255, 1.0, true, type));
		assertNotNull(FactoryThresholdBinary.globalFixed(10, true, type));
		assertNotNull(FactoryThresholdBinary.globalOtsu(0, 255, 1.0, true, type));

		assertEquals(List.of("localGaussian", "localNiblackFamily", "localNiblackFamily", "localNiblackFamily",
				"localNick", "localMean", "localOtsu", "blockMinMax", "blockMean", "blockOtsu", "globalEntropy",
				"globalFixed", "globalOtsu"), called);
	}

	/// If an override returns a filter, the factory returns it
	@Test void overrideReturnsFilter() {
		InputToBinary<GrayU8> expected = new GlobalFixedBinaryFilter<>(10, true,
				ImageType.single(GrayU8.class));
		BOverrideFactoryThresholdBinary.globalFixed = new BOverrideFactoryThresholdBinary.GlobalFixed() {
			@SuppressWarnings("unchecked")
			@Override public <T extends ImageGray<T>>
			InputToBinary<T> handle( double threshold, boolean down, Class<T> inputType ) {
				return (InputToBinary<T>)expected;
			}
		};
		assertSame(expected, FactoryThresholdBinary.globalFixed(10, true, GrayU8.class));
	}
}

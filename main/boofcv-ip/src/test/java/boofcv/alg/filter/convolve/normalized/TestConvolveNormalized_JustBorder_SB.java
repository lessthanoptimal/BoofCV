/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.normalized;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConvolveNormalized_JustBorder_SB extends BoofStandardJUnit {
	@Test void compareToNaive() {
		var test = new CompareToNaive();
		int numFunctions = 20;

		for (int i = 0; i < 2; i++) {
			test.setImageDimension(15 + i, 20 + i);
			// convolve with different kernel sizes relative to the skip amount
			test.setKernelRadius(1, 1);
			test.performTests(numFunctions);
			test.setKernelRadius(2, 2);
			test.performTests(numFunctions);
			test.setKernelRadius(3, 3);
			test.performTests(numFunctions);

			// non symmetric
			test.setKernelRadius(3, 1);
			test.performTests(numFunctions);
			test.setKernelRadius(3, 4);
			test.performTests(numFunctions);

			// NOTE it intentionally can't handle this special case
			// now try a pathological case where the kernel is larger than the image
//		test.setKernelRadius(10);
//		test.performTests(9);
		}
	}

	public static class CompareToNaive extends CompareToStandardConvolutionNormalized {

		public CompareToNaive() {
			super(ConvolveNormalized_JustBorder_SB.class);
		}

		@Override protected boolean isEquivalent( Method candidate, Method evaluation ) {
			if (!evaluation.getName().equals("vertical"))
				return super.isEquivalent(candidate, evaluation);

			if (!candidate.getName().equals(evaluation.getName()))
				return false;

			Class<?>[] e = evaluation.getParameterTypes();
			Class<?>[] c = candidate.getParameterTypes();

			boolean hasWorkspace = e[e.length - 1].isAssignableFrom(GrowArray.class);
			if (!hasWorkspace)
				return super.isEquivalent(candidate, evaluation);

			for (int i = 0; i < c.length; i++) {
				if (e[i] != c[i])
					return false;
			}
			return true;
		}

		/**
		 * Just compares the image border against each other.
		 */
		@Override
		protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {

			GImageGray t, v;

			int borderX0 = 0, borderX1 = 0;
			int borderY0 = 0, borderY1 = 0;

			if (methodTest.getName().contentEquals("convolve")) {
				t = FactoryGImageGray.wrap((ImageGray)targetParam[2]);
				v = FactoryGImageGray.wrap((ImageGray)validationParam[2]);
				borderX0 = borderY0 = offset;
				borderX1 = borderY1 = kernelRadius*2 - offset;
			} else if (methodTest.getName().contentEquals("horizontal")) {
				t = FactoryGImageGray.wrap((ImageGray)targetParam[2]);
				v = FactoryGImageGray.wrap((ImageGray)validationParam[2]);
				borderX0 = offset;
				borderX1 = kernelRadius*2 - offset;
			} else if (methodTest.getName().contentEquals("vertical")) {
				if (validationParam.length == 3) { // validation has no workspace, which makes this easier
					t = FactoryGImageGray.wrap((ImageGray)targetParam[2]);
					v = FactoryGImageGray.wrap((ImageGray)validationParam[2]);
					borderY0 = offset;
					borderY1 = kernelRadius*2 - offset;
				} else {
					t = FactoryGImageGray.wrap((ImageGray)targetParam[3]);
					v = FactoryGImageGray.wrap((ImageGray)validationParam[3]);
					borderX0 = borderY0 = offset;
					borderX1 = borderY1 = kernelRadius*2 - offset;
				}
			} else {
				throw new RuntimeException("Unknown");
			}

//			System.out.println("-----------------------");
//			((ImageGray)t.getImage()).print();
//			System.out.println();
//			((ImageGray)v.getImage()).print();

			final int width = t.getWidth();
			final int height = t.getHeight();

			// Adjust the equality tolerance depending on the data type
			ImageDataType dt = t.getImage().getDataType();
			double tolRatio = dt.isInteger() ? 0 : dt.getNumBits() == 32 ? 1e-4 : 1e-8;

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (x < borderX0 || y < borderY0 || x >= width - borderX1 || y >= height - borderY1) {
						Number numT = t.get(x, y);
						Number numV = v.get(x, y);

						double tol = Math.max(numV.doubleValue(), numT.doubleValue())*tolRatio;

						assertEquals(numV.doubleValue(), numT.doubleValue(), tol);
					}
				}
			}
		}
	}
}

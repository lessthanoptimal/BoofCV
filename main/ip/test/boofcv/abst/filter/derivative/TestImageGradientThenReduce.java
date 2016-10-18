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

package boofcv.abst.filter.derivative;

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageGradientThenReduce {
	@Test
	public void stuff() {

		HelperGradient gradient = new HelperGradient();
		HelperReduce reduce = new HelperReduce();

		Planar<GrayF32> input = new Planar<>(GrayF32.class,10,12,3);
		GrayF32 outDerivX = new GrayF32(10,12);
		GrayF32 outDerivY = new GrayF32(10,12);

		ImageGradientThenReduce<Planar<GrayF32>,Planar<GrayF32>,GrayF32> alg =
				new ImageGradientThenReduce<>(gradient, reduce);

		alg.process(input,outDerivX,outDerivY);

		assertEquals(1,outDerivX.get(2,3),1e-4f);
		assertEquals(2,outDerivY.get(2,3),1e-4f);

		alg.setBorderType(BorderType.EXTENDED);
		assertTrue(gradient.setBorderCalled);

	}

	public static class HelperGradient implements ImageGradient<Planar<GrayF32>,Planar<GrayF32>> {

		public boolean setBorderCalled = false;

		@Override
		public void setBorderType(BorderType type) {
			setBorderCalled = true;
		}

		@Override
		public BorderType getBorderType() {
			return BorderType.EXTENDED;
		}

		@Override
		public int getBorder() {
			return 0;
		}

		@Override
		public ImageType<Planar<GrayF32>> getDerivativeType() {
			return ImageType.pl(3,GrayF32.class);
		}

		@Override
		public void process(Planar<GrayF32> inputImage, Planar<GrayF32> derivX, Planar<GrayF32> derivY) {
			derivX.getBand(0).set(5,6,1);
			derivY.getBand(0).set(5,6,2);
		}
	}

	public static class HelperReduce implements GradientMultiToSingleBand<Planar<GrayF32>,GrayF32> {

		@Override
		public void process(Planar<GrayF32> inDerivX, Planar<GrayF32> inDerivY, GrayF32 outDerivX, GrayF32 outDerivY) {
			assertEquals(1,inDerivX.getBand(0).get(5,6),1e-5f);
			assertEquals(2,inDerivY.getBand(0).get(5,6),1e-5f);

			outDerivX.set(2,3,1);
			outDerivY.set(2,3,2);
		}

		@Override
		public ImageType<Planar<GrayF32>> getInputType() {
			return ImageType.pl(3,GrayF32.class);
		}

		@Override
		public Class<GrayF32> getOutputType() {
			return GrayF32.class;
		}
	}
}
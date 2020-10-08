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

package boofcv.alg.misc;

import boofcv.BoofTesting;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestImageNormalization extends BoofStandardJUnit {
	int width = 40;
	int height = 30;

	private Class[] types = new Class[]{GrayU8.class, GrayU16.class, GrayF32.class};

	@Test
	void maxAbsOfOne() {
		for( Class type : types ) {
			ImageGray src = GeneralizedImageOps.createSingleBand(type,width,height);
			double maxValue = Math.min(src.getImageType().getDataType().getMaxValue(),2000);
			double minValue = src.getDataType().isSigned() ? -maxValue : 0;

			GImageMiscOps.fillUniform(src,rand,minValue,maxValue);
			GrayF32 dst = (GrayF32)src.createSameShape(GrayF32.class);

			NormalizeParameters param = new NormalizeParameters();
			ImageNormalization.maxAbsOfOne(src,dst,param);

			check_maxAbsOfOne(src, dst, param);
		}
	}

	private void check_maxAbsOfOne(ImageGray src, GrayF32 dst, NormalizeParameters param) {
		double maxAbs = 0;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = dst.get(x,y);
				maxAbs = Math.max(maxAbs,Math.abs(v));
			}
		}

		assertEquals(1.0,maxAbs,1e-2);

		// Apply the parameters and see if it gets the same results
		GrayF32 dst2 = dst.createSameShape();
		ImageNormalization.apply(src,param,dst2);
		BoofTesting.assertEquals(dst,dst2,1e-2);
	}

	@Test
	void zeroMeanMaxOne() {
		for( Class type : types ) {
			ImageGray src = GeneralizedImageOps.createSingleBand(type,width,height);
			double maxValue = Math.min(src.getImageType().getDataType().getMaxValue(),2000);
			double minValue = src.getDataType().isSigned() ? -maxValue : 0;

			GImageMiscOps.fillUniform(src,rand,minValue,maxValue);
			GrayF32 dst = (GrayF32)src.createSameShape(GrayF32.class);

			NormalizeParameters param = new NormalizeParameters();
			ImageNormalization.zeroMeanMaxOne(src,dst,param);

			check_zeroMeanMaxOne(src, dst, param,1.0);
		}
	}

	/**
	 * Pathological case where everything is the same value
	 */
	@Test
	void zeroMeanMaxOne_identical() {
		for( Class type : types ) {
			zeroMeanMaxOne_identical(type,0.0);
			zeroMeanMaxOne_identical(type,5.0);
		}
	}
	void zeroMeanMaxOne_identical( Class type, double value ) {
		ImageGray src = GeneralizedImageOps.createSingleBand(type,width,height);
		GImageMiscOps.fill(src,value);
		GrayF32 dst = (GrayF32)src.createSameShape(GrayF32.class);

		NormalizeParameters param = new NormalizeParameters();
		ImageNormalization.zeroMeanMaxOne(src,dst,param);

		check_zeroMeanMaxOne(src, dst, param,0.0);
	}

	private void check_zeroMeanMaxOne(ImageGray src, GrayF32 dst, NormalizeParameters param, double expectedMaxAbs) {
		double mean = 0;
		double maxAbs = 0;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = dst.get(x,y);
				mean += v;
				maxAbs = Math.max(maxAbs,Math.abs(v));
			}
		}

		assertEquals(0.0,mean/src.totalPixels(), UtilEjml.TEST_F32);
		assertEquals(expectedMaxAbs,maxAbs,1e-2);

		// Apply the parmeters and see if it gets the same results
		GrayF32 dst2 = dst.createSameShape();
		ImageNormalization.apply(src,param,dst2);
		BoofTesting.assertEquals(dst,dst2,1e-2);
	}

	@Test
	void zeroMeanStdOne() {
		for( Class type : types ) {
			ImageGray src = GeneralizedImageOps.createSingleBand(type,width,height);
			double maxValue = Math.min(src.getImageType().getDataType().getMaxValue(),2000);
			double minValue = src.getDataType().isSigned() ? -maxValue : 0;

			GImageMiscOps.fillUniform(src,rand,minValue,maxValue);
			GrayF32 dst = (GrayF32)src.createSameShape(GrayF32.class);

			NormalizeParameters param = new NormalizeParameters();
			ImageNormalization.zeroMeanStdOne(src,dst,param);

			check_zeroMeanStdOne(src, dst, param, 1.0);
		}
	}

	@Test
	void zeroMeanStdOne_identical() {
		for( Class type : types ) {
			zeroMeanStdOne_identical(type,0.0);
			zeroMeanStdOne_identical(type,5.0);
		}
	}
	void zeroMeanStdOne_identical( Class type, double value ) {
		ImageGray src = GeneralizedImageOps.createSingleBand(type,width,height);
		GImageMiscOps.fill(src,value);
		GrayF32 dst = (GrayF32)src.createSameShape(GrayF32.class);

		NormalizeParameters param = new NormalizeParameters();
		ImageNormalization.zeroMeanStdOne(src,dst,param);

		check_zeroMeanStdOne(src, dst, param, 0.0);
	}

	private void check_zeroMeanStdOne(ImageGray src, GrayF32 dst, NormalizeParameters param , double expectedStdev) {
		float mean = ImageStatistics.mean(dst);
		double stdev = 0;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = dst.get(x,y)-mean;
				stdev += v*v;
			}
		}

		stdev = Math.sqrt(stdev/(src.totalPixels()-1));

		assertEquals(0.0,mean, UtilEjml.TEST_F32);
		assertEquals(expectedStdev,stdev,1e-2);

		// Apply the parmeters and see if it gets the same results
		GrayF32 dst2 = dst.createSameShape();
		ImageNormalization.apply(src,param,dst2);
		BoofTesting.assertEquals(dst,dst2,0.1);
		// Maybe this large tolerance is a sign the operations need to be changed
	}
}

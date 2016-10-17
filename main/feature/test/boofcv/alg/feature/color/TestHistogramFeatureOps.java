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

package boofcv.alg.feature.color;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHistogramFeatureOps {

	int width = 30;
	int height = 40;
	Random rand = new Random(345345);

	@Test
	public void histogram_U8() {

		GrayU8 image = new GrayU8(width,height);

		image.set(2, 3, 40);
		image.set(2, 4, 40);
		image.set(2, 5, 40);
		image.set(2, 6, 40);

		image.set(5, 6, 255);
		image.set(5, 7, 255);

		TupleDesc_F64 histogram = new TupleDesc_F64(256);
		randomFill(histogram);

		double expected[] = new double[256];
		expected[0] = width*height-6;
		expected[40]=4.0; expected[255]=2.0;

		HistogramFeatureOps.histogram(image, 255, histogram);
		checkEquals(histogram, expected);


		// now have a different max and less bins than values
		histogram = new TupleDesc_F64(30);
		randomFill(histogram);
		image.set(5, 6, 150);
		image.set(5, 7, 150);
		image.set(5, 8, 41);

		expected = new double[30];
		expected[0] = width*height-7;
		expected[5]=4.0; expected[6]=1.0;expected[22]=2.0;
		HistogramFeatureOps.histogram(image, 200, histogram);

		checkEquals(histogram, expected);
	}

	@Test
	public void histogram_U16() {
		GrayU16 image = new GrayU16(width,height);

		image.set(2, 3, 40000);
		image.set(2, 4, 40000);
		image.set(2, 5, 40000);
		image.set(2, 6, 40000);

		image.set(5, 6, 65535);
		image.set(5, 7, 65535);

		TupleDesc_F64 histogram = new TupleDesc_F64(256);
		randomFill(histogram);

		double expected[] = new double[256];
		expected[0] = width*height-6;
		expected[156]=4.0; expected[255]=2.0;

		HistogramFeatureOps.histogram(image, 65535, histogram);
		checkEquals(histogram, expected);
	}

	@Test
	public void histogram_F32() {
		GrayF32 image = new GrayF32(width,height);

		image.set(2, 3, 40);
		image.set(2, 4, 40);
		image.set(2, 5, 40);
		image.set(2, 6, 40);

		image.set(5, 6, 255);
		image.set(5, 7, 255f*255f/256f);// should just barely be in bin 255
		image.set(5, 8, 254.9f);// this should go into bin 255 also

		TupleDesc_F64 histogram = new TupleDesc_F64(256);
		randomFill(histogram);

		double expected[] = new double[256];
		expected[0] = width*height-7;
		expected[40]=4.0; expected[255]=3.0;

		HistogramFeatureOps.histogram(image, 0, 255, histogram);
		checkEquals(histogram, expected);

		// now have a different max and less bins than values
		histogram = new TupleDesc_F64(30);
		randomFill(histogram);
		image.set(5, 6, 150);
		image.set(5, 7, 150);
		image.set(5, 8, 150);
		image.set(5, 9, 49);

		expected = new double[30];
		expected[10] = width*height-8;
		expected[14]=5.0; expected[25]=3.0;
		HistogramFeatureOps.histogram(image, -100 , 200, histogram);

		checkEquals(histogram, expected);
	}

	@Test
	public void histogram_PL_U8() {
		Planar<GrayU8> image = new Planar<>(GrayU8.class,width,height,2);

		GeneralizedImageOps.setM(image, 2, 3, 20, 30);
		GeneralizedImageOps.setM(image, 2, 4, 20, 30);
		GeneralizedImageOps.setM(image, 2, 5, 20, 30);
		GeneralizedImageOps.setM(image, 2, 6, 20, 30);
		GeneralizedImageOps.setM(image, 2, 7, 20, 30);

		GeneralizedImageOps.setM(image, 3, 7, 45, 2);

		Histogram_F64 histogram = new Histogram_F64(256,256);
		histogram.setRange(0, 0, 255);
		histogram.setRange(1, 0, 255);

		HistogramFeatureOps.histogram_U8(image, histogram);

		int N = width*height;
		assertEquals(N-6, histogram.get(0,0),1e-8);
		assertEquals(5, histogram.get(20, 30), 1e-8);
		assertEquals(1, histogram.get(45, 2),1e-8);
	}

	/**
	 * Compare to single band image.  Results should be identical
	 */
	@Test
	public void histogram_PL_U8_compareToSingle() {
		GrayU8 image = new GrayU8(width,height);
		ImageMiscOps.fillUniform(image,rand,0,255);

		Planar<GrayU8> ms = new Planar<>(GrayU8.class,width,height,1);
		ms.setBand(0,image);

		TupleDesc_F64 expected = new TupleDesc_F64(256);
		Histogram_F64 found = new Histogram_F64(256);
		found.setRange(0,0,255);

		HistogramFeatureOps.histogram(image, 255, expected);
		HistogramFeatureOps.histogram_U8(ms,found);

		for (int i = 0; i < found.size(); i++) {
			assertEquals(expected.getDouble(i),found.getDouble(i),1e-8);
		}
	}

	@Test
	public void histogram_PL_F32() {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,width,height,2);

		GeneralizedImageOps.setM(image, 2, 3, 20, 30);
		GeneralizedImageOps.setM(image, 2, 4, 20, 30);
		GeneralizedImageOps.setM(image, 2, 5, 20, 30);
		GeneralizedImageOps.setM(image, 2, 6, 20, 30);
		GeneralizedImageOps.setM(image, 2, 7, 20, 30);

		GeneralizedImageOps.setM(image, 3, 7, 45, 2);

		Histogram_F64 histogram = new Histogram_F64(256,256);
		histogram.setRange(0, 0, 256);
		histogram.setRange(1, 0, 256);

		HistogramFeatureOps.histogram_F32(image, histogram);

		int N = width*height;
		assertEquals(N-6, histogram.get(0,0),1e-8);
		assertEquals(5, histogram.get(20, 30), 1e-8);
		assertEquals(1, histogram.get(45, 2),1e-8);
	}

	/**
	 * Compare to single band image.  Results should be identical
	 */
	@Test
	public void histogram_PL_F32_compareToSingle() {
		GrayF32 image = new GrayF32(width,height);
		ImageMiscOps.fillUniform(image,rand,-100,100);

		Planar<GrayF32> ms = new Planar<>(GrayF32.class,width,height,1);
		ms.setBand(0,image);

		TupleDesc_F64 expected = new TupleDesc_F64(200);
		Histogram_F64 found = new Histogram_F64(200);
		found.setRange(0,-100,100);

		HistogramFeatureOps.histogram(image, -100,100, expected);
		HistogramFeatureOps.histogram_F32(ms, found);

		for (int i = 0; i < found.size(); i++) {
			assertEquals(expected.getDouble(i),found.getDouble(i),1e-8);
		}
	}

	private void checkEquals(TupleDesc_F64 histogram, double[] expected) {
		for (int i = 0; i < histogram.value.length; i++) {
			assertEquals(i+"",expected[i],histogram.value[i],1e-8);
		}
	}

	private void randomFill( TupleDesc_F64 histogram) {
		for (int i = 0; i < histogram.value.length; i++) {
			histogram.value[i] = rand.nextDouble()*100-50;
		}
	}
}
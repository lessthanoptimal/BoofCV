/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGHistogramFeatureOps {

	Random rand = new Random(2345);

	int width = 30;
	int height = 40;

	Class[] supported = new Class[]{ImageUInt8.class, ImageFloat32.class};

	@Test
	public void histogram_sb() {

		for( Class type : supported ) {
			ImageSingleBand image = GeneralizedImageOps.createSingleBand(type,width,height);
			GImageMiscOps.fillUniform(image,rand,0,200);

			TupleDesc_F64 found = new TupleDesc_F64(200);
			TupleDesc_F64 expected = new TupleDesc_F64(200);

			GHistogramFeatureOps.histogram(image, 0, 200, found);

			if( type == ImageFloat32.class ) {
				HistogramFeatureOps.histogram((ImageFloat32)image,0,200,expected);
			} else {
				HistogramFeatureOps.histogram((ImageUInt8)image,200,expected);
			}

			assertEquals(0, DescriptorDistance.euclidean(expected,found),1e-8);
		}
	}

	@Test
	public void histogram_ms() {
		for( Class type : supported ) {
			MultiSpectral image = new MultiSpectral(type,width,height,2);
			GImageMiscOps.fillUniform(image,rand,0,200);

			Histogram_F64 found = new Histogram_F64(50,40);
			found.setRange(0,0,200);found.setRange(1, 0, 200);
			Histogram_F64 expected = new Histogram_F64(50,40);
			expected.setRange(0,0,200);expected.setRange(1,0,200);

			GHistogramFeatureOps.histogram(image, found);

			if( type == ImageFloat32.class ) {
				HistogramFeatureOps.histogram_F32(image, expected);
			} else {
				HistogramFeatureOps.histogram_U8(image, expected);
			}

			assertEquals(0, DescriptorDistance.euclidean(expected,found),1e-8);
		}
	}


	@Test
	public void histogram_array() {
		MultiSpectral<ImageUInt8> image = new MultiSpectral(ImageUInt8.class,width,height,2);
		GImageMiscOps.fillUniform(image,rand,0,200);

		double[] pixels = new double[ image.width*image.height*2 ];
		for (int i = 0; i < pixels.length; i += 2) {
			pixels[i] = image.getBand(0).getData()[i/2]&0xFF;
			pixels[i+1] = image.getBand(1).getData()[i/2]&0xFF;
		}

		Histogram_F64 found = new Histogram_F64(50,40);
		found.setRange(0,0,201);found.setRange(1, 0, 201); // +2 to max
		Histogram_F64 expected = new Histogram_F64(50,40);
		expected.setRange(0,0,200);expected.setRange(1,0,200);

		GHistogramFeatureOps.histogram(image, expected);
		GHistogramFeatureOps.histogram(pixels,pixels.length,found);

		assertEquals(0, DescriptorDistance.euclidean(expected, found), 1e-8);
	}
}
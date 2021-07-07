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

package boofcv.abst.feature.dense;

import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestGenericDenseDescribeImage extends BoofStandardJUnit {

	/**
	 * Give it a known situation and see if it produces the expected results
	 */
	@Test void process() {
		DummyFeature sparse = new DummyFeature();

		GenericDenseDescribeImageDense alg = new GenericDenseDescribeImageDense(sparse,1.5,3,4);

		GrayU8 image = new GrayU8(100,110);

		alg.process(image);
		List<TupleDesc_F64> descs = alg.getDescriptions();
		List<Point2D_I32> points = alg.getLocations();

		assertEquals(descs.size(),points.size());

		int featureRadius = (int)Math.round(1.5*7.0/2.0);
		int w = (100-2*featureRadius)/3;
		int h = (110-2*featureRadius)/4;

		// -1 since it intentionally skips feature 20
		assertEquals(w*h-1,points.size());

		int count = 0;
		for (int y = 0; y < h; y++) {
			int pixelY = featureRadius + y*y;
			for (int x = 0; x < w; x++) {
				int pixelX = featureRadius + x*3;

				Point2D_I32 p = null;
				if( count < 19 ) {
					p = points.get(count);
				} else if( count > 20 ) {
					p = points.get(count+1);
				} else {
					continue;
				}
				assertEquals(pixelX,p.x);
				assertEquals(pixelY,p.y);
				count++;
			}
		}
	}

	@Test void checkDescriptorScale() {
		DummyFeature sparse = new DummyFeature();

		GrayU8 image = new GrayU8(100,110);

		GenericDenseDescribeImageDense alg = new GenericDenseDescribeImageDense(sparse,1,8,9);

		alg.configure(1, 8, 9);
		alg.process(image);
		int found10 = alg.getDescriptions().size();

		alg.configure(1.5, 8, 9);
		alg.process(image);
		int found15 = alg.getDescriptions().size();

		alg.configure(0.75, 8, 9);
		alg.process(image);
		int found07a = alg.getDescriptions().size();

		alg.configure(0.75, 8*0.75, 9*0.75);
		alg.process(image);
		int found07b = alg.getDescriptions().size();

		// same sampling period, should have same number of samples
		assertEquals(found10, found07a);
		assertEquals(found15, found10);
		// sampling period is shorter so there should be more features
		assertTrue(found07b > found10);
	}

	public static class DummyFeature implements DescribePointRadiusAngle {

		ImageType type = ImageType.single(GrayU8.class);
		double inputRadius;
		ImageBase image;
		DogArray<Point2D_I32> points = new DogArray<>(Point2D_I32::new);
		int count = 0;

		@Override
		public void setImage(ImageBase image) {
			this.image = image;
			count = 0;
		}

		@Override
		public boolean process(double x, double y, double orientation, double radius, TupleDesc description) {
			assertNotNull(description);
			if( ++count != 20 ) {
				points.grow().setTo((int) x, (int) y);
				inputRadius = radius;
				return true;
			} else {
				return false;
			}
		}

		@Override public boolean isScalable() {return false;}

		@Override public boolean isOriented() {return false;}

		@Override
		public ImageType getImageType() {
			return type;
		}

		@Override
		public double getCanonicalWidth() {
			return 7;
		}

		@Override
		public TupleDesc createDescription() {
			return new TupleDesc_F64(10);
		}

		@Override
		public Class getDescriptionType() {
			return TupleDesc_F64.class;
		}
	}
}

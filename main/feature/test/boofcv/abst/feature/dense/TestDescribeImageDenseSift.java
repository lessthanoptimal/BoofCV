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

package boofcv.abst.feature.dense;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.dense.ConfigDenseSift;
import boofcv.factory.feature.dense.DenseSampling;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribeImageDenseSift {

	int width = 120;
	int height = 90;

	Random rand = new Random(234);

	Class imageTypes[] = new Class[]{GrayU8.class, GrayF32.class};

	int widthScaleOne;

	public TestDescribeImageDenseSift() {
		DescribeImageDenseSift alg = createAlg(GrayU8.class,5,5);
		widthScaleOne = alg.getAlg().getCanonicalRadius()*2;
	}

	/**
	 * Checks to see if the returned location is actually where it sampled
	 */
	@Test
	public void checkSampleLocations() {
		for( Class type : imageTypes ) {
			ImageGray image = GeneralizedImageOps.createSingleBand(type,width,height);
			GImageMiscOps.fillUniform(image,rand,0,200);

			DescribeImageDense alg = createAlg(type,8,9);

			alg.process(image);

			List<Point2D_I32> locations = alg.getLocations();

			for( int i = 0; i < locations.size(); i++ ) {
				Point2D_I32 p = locations.get(i);

				TupleDesc_F64 expected = describe(p.x,p.y,image,alg);
				TupleDesc_F64 found = (TupleDesc_F64)alg.getDescriptions().get(i);

				for (int j = 0; j < expected.size(); j++) {
					assertEquals(expected.value[j],found.value[j],1e-8);
				}
			}
		}
	}

	/**
	 * Features should not be sampled so that they go over the image border
	 */
	@Test
	public void checkBorder() {
		for( Class type : imageTypes ) {
			ImageGray image = GeneralizedImageOps.createSingleBand(type,width,height);
			GImageMiscOps.fillUniform(image,rand,0,200);

			DescribeImageDense alg = createAlg(type,8,9);

			alg.process(image);

			List<Point2D_I32> locations = alg.getLocations();

			int w = getWidthScaleOfOne();
			int r = w/2;

			int numCols = (image.width-w)/8;
			int numRows = (image.height-w)/9;
			assertEquals(numCols*numRows,locations.size());

			for( Point2D_I32 p : locations ) {
				assertTrue(p.x+" "+p.y, p.x >= r && p.x <= width-r );
				assertTrue( p.y >= r && p.y <= height-r );
			}
		}
	}

	private <T extends DescribeImageDense> T createAlg( Class imageType , double periodX , double periodY  ) {

		ConfigDenseSift config = new ConfigDenseSift(new DenseSampling(periodX,periodY));

		return (T)FactoryDescribeImageDense.sift(config,imageType);
	}

	private TupleDesc_F64 describe(int x , int y , ImageGray image , DescribeImageDense alg ) {
		DescribeImageDenseSift sift = (DescribeImageDenseSift)alg;

		TupleDesc_F64 output = sift.createDescription();
		sift.alg.computeDescriptor(x,y,output);

		return output;
	}

	private int getWidthScaleOfOne() {
		return widthScaleOne;
	}
}

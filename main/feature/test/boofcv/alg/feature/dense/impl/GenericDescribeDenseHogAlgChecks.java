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

package boofcv.alg.feature.dense.impl;

import boofcv.alg.feature.dense.DescribeDenseHogAlg;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericDescribeDenseHogAlgChecks<T extends ImageBase> {

	public Random rand = new Random(234);
	ImageType<T> imageType;

	int width = 50, height = 60;

	public GenericDescribeDenseHogAlgChecks(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	public abstract DescribeDenseHogAlg<T,?> createAlg( int orientationBins , int widthCell , int widthBlock , int stepBlock );

	/**
	 * Very basic test which really just tests to see if its doing something
	 */
	@Test
	public void process() {
		int widthCell = 8;

		for (int blockWidth = 1; blockWidth <= 3; blockWidth++) {
			for (int step = 1; step <= 3; step++) {

				DescribeDenseHogAlg<T, ?> alg = createAlg(10, widthCell, blockWidth, step);

				T input = imageType.createImage(width, height);
				GImageMiscOps.fillUniform(input, rand, 0, 120);

				alg.setInput(input);
				alg.process();

				FastQueue<Point2D_I32> points = alg.getLocations();
				FastQueue<TupleDesc_F64> descriptions = alg.getDescriptions();

				int pixelsBlock = blockWidth*widthCell;

				int index = 0;
				for (int y = 0; y+pixelsBlock <= height; y += widthCell*step) {
					for (int x = 0; x+pixelsBlock <= width; x += widthCell*step, index++) {
						assertEquals(points.get(index).x, x);
						assertEquals(points.get(index).y, y);
					}
				}

				assertEquals(index, points.size());
				assertEquals(index, descriptions.size());
			}
		}
	}

	@Test
	public void computeDerivative() {
		DescribeDenseHogAlg<T,?> alg = createAlg(10,8,3,1);

		ImageBase derivX = alg._getDerivX();
		ImageBase derivY = alg._getDerivY();

		derivX.reshape(width,height);
		derivY.reshape(width,height);

		GImageMiscOps.fillUniform(derivX,rand,0,150);
		GImageMiscOps.fillUniform(derivY,rand,0,150);

		int N = derivX.getImageType().getNumBands();

		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++, index++ ) {
				double maxNorm = 0;
				double maxX = 0;
				double maxY = 0;

				alg.computeDerivative(index);

				for (int i = 0; i < N; i++) {
					double dx = GeneralizedImageOps.get(derivX,x,y,i);
					double dy = GeneralizedImageOps.get(derivY,x,y,i);

					double n = dx*dx + dy*dy;
					if( n > maxNorm ) {
						maxNorm = n;
						maxX = dx;
						maxY = dy;
					}
				}

				assertEquals(alg._getPixelDX(),maxX,1e-4);
				assertEquals(alg._getPixelDY(),maxY,1e-4);
			}
		}
	}

}

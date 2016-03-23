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

package boofcv.alg.feature.describe;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseScaleGradient;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkSurfDescribeOps<T extends ImageGray>
{
	static int imgWidth = 640;
	static int imgHeight = 480;
	Class<T> imageType;
	
	T input;

	// parameters for region gradient
	double tl_x = 100.0;
	double tl_y = 120.3;
	double period = 1.2;
	int regionSize = 20;
	double kernelWidth = 5;
	double derivX[] = new double[ regionSize*regionSize ];
	double derivY[] = new double[ regionSize*regionSize ];

	// kernel used to manually sample
	SparseScaleGradient<T,?> g;

	public BenchmarkSurfDescribeOps() {
		this((Class<T>)GrayF32.class);
	}

	public BenchmarkSurfDescribeOps(Class<T> imageType) {
		this.imageType = imageType;
		Random rand = new Random(234);
		input = GeneralizedImageOps.createSingleBand(imageType,imgWidth,imgHeight);
		GImageMiscOps.fillUniform(input, rand, 0, 1);
		g = SurfDescribeOps.createGradient(false,imageType);
		g.setWidth(kernelWidth);
		g.setImage(input);
	}

	public int timeGradient_NotHaar(int reps) {
		for( int i = 0; i < reps; i++ )
			SurfDescribeOps.gradient(input, tl_x, tl_y, period, regionSize,
					kernelWidth, false, derivX, derivY);
		return 0;
	}

	public int timeGradient_Haar(int reps) {
		for( int i = 0; i < reps; i++ )
			SurfDescribeOps.gradient(input, tl_x , tl_y , period, regionSize,
					kernelWidth,true,derivX,derivY);
		return 0;
	}

	/**
	 * Sample the gradient using SparseImageGradient instead of the completely
	 * unrolled code
	 */
	public int timeGradient_Sample(int reps) {

		for( int i = 0; i < reps; i++ ) {
			double tl_x = this.tl_x + 0.5;
			double tl_y = this.tl_y + 0.5;

			int j = 0;
			for( int y = 0; y < regionSize; y++ ) {
				for( int x = 0; x < regionSize; x++ , j++) {
					int xx = (int)(tl_x + x * period);
					int yy = (int)(tl_y + y * period);

					GradientValue deriv = g.compute(xx,yy);
					derivX[j] = deriv.getX();
					derivY[j] = deriv.getY();
				}
			}

		}
		return 0;
	}

	/**
	 * Sample the gradient, but just for boundary conditions
	 */
	public int timeGradient_SampleCheck(int reps) {

		for( int i = 0; i < reps; i++ ) {
			double tl_x = this.tl_x + 0.5;
			double tl_y = this.tl_y + 0.5;

			int j = 0;
			for( int y = 0; y < regionSize; y++ ) {
				for( int x = 0; x < regionSize; x++ , j++) {
					int xx = (int)(tl_x + x * period);
					int yy = (int)(tl_y + y * period);

					if( g.isInBounds(xx,yy)) {
						GradientValue deriv = g.compute(xx,yy);
						derivX[j] = deriv.getX();
						derivY[j] = deriv.getY();
					}
				}
			}

		}
		return 0;
	}
	
	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight  +" ==========");

//		Runner.main(BenchmarkSurfDescribeOps.class, args);
	}
}

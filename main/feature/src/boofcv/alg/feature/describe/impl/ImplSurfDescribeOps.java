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

package boofcv.alg.feature.describe.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseGradientSafe;
import boofcv.struct.sparse.SparseScaleGradient;


/**
 *
 * Design Note:
 * When estimating the
 *
 * @author Peter Abeles
 */
public class ImplSurfDescribeOps {

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link boofcv.alg.transform.ii.DerivativeIntegralImage}.
	 * Assumes that the entire region, including the surrounding pixels, are inside the image.
	 */
	public static void gradientInner(GrayF32 ii, double tl_x, double tl_y, double samplePeriod ,
									 int regionSize, double kernelSize,
									 float[] derivX, float derivY[])
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		tl_x += 0.5;
		tl_y += 0.5;

		// round the kernel size
		int w = (int)(kernelSize+0.5);
		int r = w/2;
		if( r <= 0 ) r = 1;
		w = r*2+1;

		int i = 0;
		for( int y = 0; y < regionSize; y++ ) {
			int pixelsY = (int)(tl_y + y * samplePeriod);
			int indexRow1 = ii.startIndex + (pixelsY-r-1)*ii.stride - r - 1;
			int indexRow2 = indexRow1 + r*ii.stride;
			int indexRow3 = indexRow2 + ii.stride;
			int indexRow4 = indexRow3 + r*ii.stride;

			for( int x = 0; x < regionSize; x++ , i++) {
				int pixelsX = (int)(tl_x + x * samplePeriod);

				final int indexSrc1 = indexRow1 + pixelsX;
				final int indexSrc2 = indexRow2 + pixelsX;
				final int indexSrc3 = indexRow3 + pixelsX;
				final int indexSrc4 = indexRow4 + pixelsX;

				final float p0 = ii.data[indexSrc1];
				final float p1 = ii.data[indexSrc1+r];
				final float p2 = ii.data[indexSrc1+r+1];
				final float p3 = ii.data[indexSrc1+w];
				final float p11 = ii.data[indexSrc2];
				final float p4 = ii.data[indexSrc2+w];
				final float p10 = ii.data[indexSrc3];
				final float p5 = ii.data[indexSrc3+w];
				final float p9 = ii.data[indexSrc4];
				final float p8 = ii.data[indexSrc4+r];
				final float p7 = ii.data[indexSrc4+r+1];
				final float p6 = ii.data[indexSrc4+w];

				final float left = p8-p9-p1+p0;
				final float right = p6-p7-p3+p2;
				final float top = p4-p11-p3+p0;
				final float bottom = p6-p9-p5+p10;

				derivX[i] = right-left;
				derivY[i] = bottom-top;
//				System.out.printf("%2d %2d %2d %2d dx = %6.2f  dy = %6.2f\n",x,y,pixelsX,pixelsY,derivX[i],derivY[i]);
			}
		}
	}

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link boofcv.alg.transform.ii.DerivativeIntegralImage}.
	 * Assumes that the entire region, including the surrounding pixels, are inside the image.
	 */
	public static void gradientInner(GrayS32 ii, double tl_x, double tl_y, double samplePeriod ,
									 int regionSize, double kernelSize,
									 int[] derivX, int derivY[])
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		tl_x += 0.5;
		tl_y += 0.5;

		// round the kernel size
		int w = (int)(kernelSize+0.5);
		int r = w/2;
		if( r <= 0 ) r = 1;
		w = r*2+1;

		int i = 0;
		for( int y = 0; y < regionSize; y++ ) {
			int pixelsY = (int)(tl_y + y * samplePeriod);
			int indexRow1 = ii.startIndex + (pixelsY-r-1)*ii.stride - r - 1;
			int indexRow2 = indexRow1 + r*ii.stride;
			int indexRow3 = indexRow2 + ii.stride;
			int indexRow4 = indexRow3 + r*ii.stride;

			for( int x = 0; x < regionSize; x++ , i++) {
				int pixelsX = (int)(tl_x + x * samplePeriod);

				final int indexSrc1 = indexRow1 + pixelsX;
				final int indexSrc2 = indexRow2 + pixelsX;
				final int indexSrc3 = indexRow3 + pixelsX;
				final int indexSrc4 = indexRow4 + pixelsX;

				final int p0 = ii.data[indexSrc1];
				final int p1 = ii.data[indexSrc1+r];
				final int p2 = ii.data[indexSrc1+r+1];
				final int p3 = ii.data[indexSrc1+w];
				final int p11 = ii.data[indexSrc2];
				final int p4 = ii.data[indexSrc2+w];
				final int p10 = ii.data[indexSrc3];
				final int p5 = ii.data[indexSrc3+w];
				final int p9 = ii.data[indexSrc4];
				final int p8 = ii.data[indexSrc4+r];
				final int p7 = ii.data[indexSrc4+r+1];
				final int p6 = ii.data[indexSrc4+w];

				final int left = p8-p9-p1+p0;
				final int right = p6-p7-p3+p2;
				final int top = p4-p11-p3+p0;
				final int bottom = p6-p9-p5+p10;

				derivX[i] = right-left;
				derivY[i] = bottom-top;
			}
		}
	}

	/**
	 * Simple algorithm for computing the gradient of a region.  Can handle image borders
	 */
	public static <T extends ImageGray>
	void naiveGradient(T ii, double tl_x, double tl_y, double samplePeriod ,
					   int regionSize, double kernelSize,
					   boolean useHaar, double[] derivX, double derivY[])
	{
		SparseScaleGradient<T,?> gg =  SurfDescribeOps.createGradient(useHaar,(Class<T>)ii.getClass());
		gg.setWidth(kernelSize);
		gg.setImage(ii);
		SparseGradientSafe g = new SparseGradientSafe(gg);

		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		tl_x += 0.5;
		tl_y += 0.5;

		int i = 0;
		for( int y = 0; y < regionSize; y++ ) {
			for( int x = 0; x < regionSize; x++ , i++) {
				int xx = (int)(tl_x + x * samplePeriod);
				int yy = (int)(tl_y + y * samplePeriod);

				GradientValue deriv = g.compute(xx,yy);
				derivX[i] = deriv.getX();
				derivY[i] = deriv.getY();
//				System.out.printf("%2d %2d %2d %2d dx = %6.2f  dy = %6.2f\n",x,y,xx,yy,derivX[i],derivY[i]);
			}
		}
	}

}

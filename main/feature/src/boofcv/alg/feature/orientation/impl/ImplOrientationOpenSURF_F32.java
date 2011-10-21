/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageFloat32;


/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.orientation.OrientationAverageIntegral} for a specific type.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationOpenSURF_F32
		implements OrientationIntegral<ImageFloat32>
{
	ImageFloat32 ii;

	final double gauss25[][] = new double[][]{
	{0.02350693969273,0.01849121369071,0.01239503121241,0.00708015417522,0.00344628101733,0.00142945847484,0.00050524879060},
	{0.02169964028389,0.01706954162243,0.01144205592615,0.00653580605408,0.00318131834134,0.00131955648461,0.00046640341759},
	{0.01706954162243,0.01342737701584,0.00900063997939,0.00514124713667,0.00250251364222,0.00103799989504,0.00036688592278},
	{0.01144205592615,0.00900063997939,0.00603330940534,0.00344628101733,0.00167748505986,0.00069579213743,0.00024593098864},
	{0.00653580605408,0.00514124713667,0.00344628101733,0.00196854695367,0.00095819467066,0.00039744277546,0.00014047800980},
	{0.00318131834134,0.00250251364222,0.00167748505986,0.00095819467066,0.00046640341759,0.00019345616757,0.00006837798818},
	{0.00131955648461,0.00103799989504,0.00069579213743,0.00039744277546,0.00019345616757,0.00008024231247,0.00002836202103}};

	// where the output from the derivative is stored
	double[] derivX;
	double[] derivY;

	// the angle each pixel is pointing
	protected double angles[];

	double scale;

	public ImplOrientationOpenSURF_F32() {
		derivX = new double[109];
		derivY = new double[109];
		angles = new double[109];
	}

	@Override
	public void setScale(double scale) {
		this.scale = scale;
	}

	@Override
	public double compute(int c_x, int c_y ) {
		computeGradient(c_x,c_y);

		return weighted();
	}

	private void computeGradient( int c_x, int c_y ) {
		int s = (int)Math.round(scale);
		SparseImageGradient<ImageFloat32,?> g =  SurfDescribeOps.createGradient(false,true,4,s,(Class<ImageFloat32>)ii.getClass());
		g.setImage(ii);

		final int id[] = {6,5,4,3,2,1,0,1,2,3,4,5,6};

		int idx = 0;
		// calculate haar responses for points within radius of 6*scale
		for(int i = -6; i <= 6; ++i)
		{
			for(int j = -6; j <= 6; ++j)
			{
				if(i*i + j*j < 36)
				{
					int x = c_x+i*s;
					int y = c_y+j*s;
					float gauss = (float)gauss25[id[i+6]][id[j+6]];
					GradientValue gradient = g.compute(x,y);
					derivX[idx] = gauss * gradient.getX();
					derivY[idx] = gauss * gradient.getY();
					angles[idx] = Math.atan2(derivY[idx], derivX[idx]);
					if( angles[idx] < 0)
						angles[idx] = 2*Math.PI+angles[idx];
//					System.out.printf("(%2d,%2d) (%d,%d) gauss = %5.2f  dx = %5.2f dy = %5.2f  angle = %4.2f\n",i,j,x,y,gauss,derivX[idx],derivY[idx],angles[idx]);

					++idx;
				}
			}
		}
	}


	private double weighted() {
		// calculate the dominant direction
		double max=0, orientation = 0;

		// loop slides pi/3 window around feature point
		for(double ang1 = 0; ang1 < 2*Math.PI;  ang1+=0.15f) {
			double ang2 = ( ang1+Math.PI/3.0f > 2*Math.PI ? ang1-5.0f*Math.PI/3.0f : ang1+Math.PI/3.0f);
			double sumX = 0;
			double sumY = 0;
			for(int k = 0; k < angles.length; ++k)
			{
				// get angle from the x-axis of the sample point
				double ang = angles[k];

				// determine whether the point is within the window
				if (ang1 < ang2 && ang1 < ang && ang < ang2)
				{
					sumX+=derivX[k];
					sumY+=derivY[k];
				}
				else if (ang2 < ang1 &&
						((ang > 0 && ang < ang2) || (ang > ang1 && ang < 2*Math.PI) ))
				{
					sumX+=derivX[k];
					sumY+=derivY[k];
				}
			}

			// if the vector produced from this window is longer than all
			// previous vectors then this forms the new dominant direction
			if (sumX*sumX + sumY*sumY > max)
			{
				// store largest orientation
				max = sumX*sumX + sumY*sumY;
				orientation = Math.atan2(sumY,sumX);
//				System.out.printf("  better angle = %5.2f  orientation = %4.2f sumX = %f\n",ang1,orientation,sumX);
			}
		}

		return orientation;
	}


	@Override
	public void setImage(ImageFloat32 integralImage) {
		this.ii = integralImage;
	}

	@Override
	public Class<ImageFloat32> getImageType() {
		return ImageFloat32.class;
	}
}

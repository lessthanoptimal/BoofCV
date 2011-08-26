/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.describe;

import gecv.alg.feature.describe.impl.ImplSurfDescribeOps;
import gecv.alg.feature.describe.impl.NaiveSurfDescribeOps;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;


/**
 * Operations related to computing SURF descriptors.
 *
 * @author Peter Abeles
 */
public class SurfDescribeOps {

	/**
	 * <p>
	 * Computes the of a square region.  The region considered has a radius
	 * of ceil(radius*s) pixels.  The derivative is computed every 's' pixels.
	 * </p>
	 *
	 * <p>
	 * Deviation from paper:<br>
	 * <ul>
	 * <li>An symmetric box derivative is used instead of the Haar wavelet.</li>
	 * </ul>
	 * </p>
	 *
	 * @param ii Integral image.
	 * @param c_x Center pixel.
	 * @param c_y Center pixel.
	 * @param radius Radius of region being considered in samples.
	 * @param s Scale of feature.
	 * @param derivX Derivative x wavelet output.
	 * @param derivY Derivative Y wavelet output.
	 */
	public static <T extends ImageBase>
	void gradient( T ii , int c_x , int c_y ,
				   int radius , double s ,
				   double []derivX , double derivY[] )
	{
//		NaiveSurfDescribeOps.gradient(ii,c_x,c_y,radius,s,false,derivX,derivY);
		int r = (int)Math.ceil(radius);
		int step = (int)Math.ceil(1);
		ImplSurfDescribeOps.gradientInner((ImageFloat32)ii,r,step,c_x-r,c_y-r,c_x+r,c_y+r,0,0,derivX,derivY);
	}


	/**
	 * <p>
	 * Computes features in the SURF descriptor.
	 * </p>
	 *
	 * <p>
	 * Deviation from paper:<br>
	 * <ul>
	 * <li>Weighting function is applied to each sub region as a whole and not to each wavelet inside the sub
	 * region.  This allows the weight to be precomputed once.  Unlikely to degrade quality significantly.</li>
	 * <li>An symmetric box derivative is used instead of the Haar wavelet.  Haar is not symmetric and the performance
	 * noticeable improved when the derivative was used instead.</li>
	 * </ul>
	 * </p>
	 *
	 * @param ii Integral image.
	 * @param c_x Center of the feature x-coordinate.
	 * @param c_y Center of the feature y-coordinate.
	 * @param theta Orientation of the features.
	 * @param weight Gaussian normalization.
	 * @param regionSize Number of wavelets wide.
	 * @param numSubRegions How many sub-regions is the large region divided along its width.
	 * @param scale The scale of the wavelets.
	 * @param features Where the features are written to.  Must be 4*numSubRegions^2 large.
	 */
	public static <T extends ImageBase>
	void features( T ii , int c_x , int c_y ,
				   double theta , Kernel2D_F64 weight ,
				   int regionSize , int numSubRegions , double scale ,
				   double []features )
	{
		NaiveSurfDescribeOps.features(ii,c_x,c_y,theta,weight,regionSize,numSubRegions,scale,false,features);
	}

	// todo move to a generalized class?
	public static void normalizeFeatures( double []features ) {
		double norm = 0;
		for( int i = 0; i < features.length; i++ ) {
			double a = features[i];
			norm += a*a;
		}
		// if the norm is zero, don't normalize
		if( norm == 0 )
			return;
		
		norm = Math.sqrt(norm);
		for( int i = 0; i < features.length; i++ ) {
			features[i] /= norm;
		}
	}
}

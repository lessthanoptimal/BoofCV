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

package gecv.alg.detect.interest;

import gecv.alg.transform.ii.DerivativeIntegralImage;
import gecv.alg.transform.ii.IntegralImageOps;
import gecv.alg.transform.ii.IntegralKernel;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
// todo optimize code more
public class SurfHessianDetector {

	public static void intensity( ImageFloat32 integral, int skip , int size ,
								  ImageFloat32 intensity)
	{
		// todo check size with skip
//		InputSanityCheck.checkSameShape(integral,intensity);

		final int w = intensity.width;
		final int h = intensity.height;

		// get convolution kernels for the second order derivatives
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(size);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(size);
		IntegralKernel kerXY = DerivativeIntegralImage.kernelDerivXY(size);

		double norm = 1.0f/(size*size);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				int xx = x*skip;
				int yy = y*skip;

				float Dxx = IntegralImageOps.convolveSparse(integral,kerXX,xx,yy);
				float Dyy = IntegralImageOps.convolveSparse(integral,kerYY,xx,yy);
				float Dxy = IntegralImageOps.convolveSparse(integral,kerXY,xx,yy);

				Dxx *= norm;
				Dxy *= norm;
				Dyy *= norm;

				float det = Dxx*Dyy-0.81f*Dxy*Dxy;

				intensity.set(x,y,det);
			}
		}
	}
}

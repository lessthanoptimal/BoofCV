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

package boofcv.alg.distort.motion;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.GrayF32;

/**
 * Operations related to simulating motion blur
 *
 * @author Peter Abeles
 */
public class MotionBlurOps {
	/**
	 * Creates a PSF for linear motion blur with constant velocity. This is done by using the equations
	 * found in [1] for phi=0, then rotating the image and converting into a kernel.
	 *
	 * [1] Reginald L. Lagendijk, Jan Biemond "Basic Methods for Image Restoration and Identification" 2005
	 *
	 * @param lengthOfMotion Length of motion for the blur. velocity*time.
	 * @param phi Angle of blur in the image. Radians.
	 * @return 2D Kernel that represents the PSF
	 */
	public static Kernel2D_F32 linearMotionPsf( double lengthOfMotion, double phi ) {
		// Pre-compute boundaries used by the equation below
		int roundDown = (int)Math.floor((lengthOfMotion - 1.0)/2.0);
		int roundUp = (int)Math.ceil((lengthOfMotion - 1.0)/2.0);

		// size of the kernel
		int kernelRadius = roundUp;
		int kernelLength = kernelRadius*2 + 1;

		var psfZero = new GrayF32(kernelLength, kernelLength);

		// shorthand
		double L = lengthOfMotion;

		// Create the PSF
		for (int x = -kernelRadius; x <= kernelRadius; x++) {
			float f;
			if (Math.abs(x) <= roundDown)
				f = (float)(1.0/L);
			else
				f = (float)((1.0/(2.0*L))*((L - 1) - 2*Math.floor((L - 1)/2)));
			psfZero.set(x + kernelRadius, kernelRadius, f);
		}

		// Rotate
		GrayF32 psf = psfZero.createSameShape();
		new FDistort(psfZero, psf).rotate(phi).apply();

		// Convert into a kernel
		var kernel = new Kernel2D_F32(kernelLength, kernelRadius);
		System.arraycopy(psf.data, 0, kernel.data, 0, kernelLength*kernelLength);

		if (phi != 0.0) {
			// make sure it still adds to one.
			// if phi==0 the sum should already be 1 or it's a bug. don't want to hide the bug
			float sum = KernelMath.sum(kernel);
			KernelMath.divide(kernel, sum);
		}

		return kernel;
	}
}

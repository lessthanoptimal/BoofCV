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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.intensity.impl.ImplHessianBlobIntensity;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;

/**
 * <p>
 * Detects "blob" intensity using the image's second derivative.  The Hessian (second derivative)
 * matrix is defined as [ I<sub>xx</sub> , I<sub>xy</sub> ; I<sub>xy</sub> , I<sub>yy</sub>],
 * where the subscript indicates a partial derivative of the input image.  The trace and determinant of this matrix
 * is commonly used to detect interest point intensities.  These tend to be at a peak for blobs
 * and circular type objects.  The trace is commonly referred to as the Laplacian.
 * </p>
 *
 * <p>
 * <ul>
 * <li>Determinant: D<sub>xx</sub>*D<sub>yy</sub> + D<sub>xy</sub><sup>2</sup></li>
 * <li>Trace: |D<sub>xx</sub> + D<sub>yy</sub>|</li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class HessianBlobIntensity {

	/**
	 * Different types of Hessian blob detectors
	 */
	public static enum Type
	{
		/**
		 * Compute the determinant.  Maximum are features.
		 */
		DETERMINANT,
		/**
		 * Compute the trace (aka Laplacian).  Maximum and Minimums are features.
		 */
		TRACE
	}

	/**
	 * Feature intensity using the Hessian matrix's determinant.
	 *
	 * @param featureIntensity Output feature intensity. Modified.
	 * @param hessianXX Second derivative along x-axis. Not modified.
	 * @param hessianYY Second derivative along y-axis. Not modified.
	 * @param hessianXY Second derivative along x-axis and y-axis. Not modified.
	 */
	public static void determinant(GrayF32 featureIntensity , GrayF32 hessianXX, GrayF32 hessianYY , GrayF32 hessianXY )
	{
		InputSanityCheck.checkSameShape(featureIntensity,hessianXX,hessianYY,hessianXY);

		ImplHessianBlobIntensity.determinant(featureIntensity,hessianXX,hessianYY,hessianXY);
	}

	/**
	 * Feature intensity using the trace of the Hessian matrix.  This is also known as the Laplacian.
	 *
	 * @param featureIntensity Output feature intensity. Modified.
	 * @param hessianXX Second derivative along x-axis. Not modified.
	 * @param hessianYY Second derivative along y-axis. Not modified.
	 */
	public static void trace(GrayF32 featureIntensity , GrayF32 hessianXX, GrayF32 hessianYY )
	{
		InputSanityCheck.checkSameShape(featureIntensity,hessianXX,hessianYY);

		ImplHessianBlobIntensity.trace(featureIntensity,hessianXX,hessianYY);
	}

	/**
	 * Feature intensity using the Hessian matrix's determinant.
	 *
	 * @param featureIntensity Output feature intensity. Modified.
	 * @param hessianXX Second derivative along x-axis. Not modified.
	 * @param hessianYY Second derivative along y-axis. Not modified.
	 * @param hessianXY Second derivative along x-axis and y-axis. Not modified.
	 */
	public static void determinant(GrayF32 featureIntensity , GrayS16 hessianXX, GrayS16 hessianYY , GrayS16 hessianXY )
	{
		InputSanityCheck.checkSameShape(featureIntensity,hessianXX,hessianYY,hessianXY);

		ImplHessianBlobIntensity.determinant(featureIntensity,hessianXX,hessianYY,hessianXY);
	}

	/**
	 * Feature intensity using the trace of the Hessian matrix.  This is also known as the Laplacian.
	 *
	 * @param featureIntensity Output feature intensity. Modified.
	 * @param hessianXX Second derivative along x-axis. Not modified.
	 * @param hessianYY Second derivative along y-axis. Not modified.
	 */
	public static void trace(GrayF32 featureIntensity , GrayS16 hessianXX, GrayS16 hessianYY )
	{
		InputSanityCheck.checkSameShape(featureIntensity,hessianXX,hessianYY);

		ImplHessianBlobIntensity.trace(featureIntensity,hessianXX,hessianYY);
	}
}

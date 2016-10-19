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

package boofcv.alg.sfm.d2;

import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.struct.distort.PixelTransform2_F32;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.affine.UtilAffine;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography;

/**
 * @author Peter Abeles
 */
public class UtilImageMotion {

	/**
	 * Given a motion model create a PixelTransform used to distort the image
	 *
	 * @param transform Motion transform
	 * @return PixelTransform_F32 used to distort the image
	 */
	public static PixelTransform2_F32 createPixelTransform(InvertibleTransform transform) {
		PixelTransform2_F32 pixelTran;
		if( transform instanceof Homography2D_F64) {
			Homography2D_F32 t = UtilHomography.convert((Homography2D_F64) transform, (Homography2D_F32)null);
			pixelTran = new PixelTransformHomography_F32(t);
		} else if( transform instanceof Homography2D_F32) {
				pixelTran = new PixelTransformHomography_F32((Homography2D_F32)transform);
		} else if( transform instanceof Affine2D_F64) {
			Affine2D_F32 t = UtilAffine.convert((Affine2D_F64) transform, null);
			pixelTran = new PixelTransformAffine_F32(t);
		} else if( transform instanceof Affine2D_F32) {
			pixelTran = new PixelTransformAffine_F32((Affine2D_F32)transform);
		} else {
			throw new RuntimeException("Unknown model type");
		}
		return pixelTran;
	}

}

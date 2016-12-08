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

package boofcv.struct;

import boofcv.core.image.border.*;


/**
 * @author Peter Abeles
 */
public class BoofDefaults {

	/**
	 * Default tolerance for floats
	 */
	public static final float TEST_FLOAT_TOL = 1e-4f;
	/**
	 * Default tolerance for doubles
	 */
	public static final double TEST_DOUBLE_TOL = 1e-8;


	public static final double SURF_SCALE_TO_RADIUS = 2.0;
	public static final double SIFT_SCALE_TO_RADIUS = 1.0;
	public static final double BRIEF_SCALE_TO_RADIUS = 2.0;

	/**
	 * String specifying BoofCV's version.
	 */
	public static String version = "0.26";

	// Use extended borders when computing image derivatives 
	public static BorderType DERIV_BORDER_TYPE = BorderType.EXTENDED;

	/**
	 * Creates a new instance of the default border for derivatives of integer images
	 */
	public static ImageBorder_S32 borderDerivative_I32() {
		return new ImageBorder1D_S32((Class)BorderIndex1D_Extend.class);
	}

	/**
	 * Creates a new instance of the default border for derivatives of GrayF32
	 */
	public static ImageBorder_F32 borderDerivative_F32() {
		return new ImageBorder1D_F32((Class)BorderIndex1D_Extend.class);
	}
}

/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
	 * String specifying BoofCV's version.
	 */
	public static String version = "0.11";

	// Use extended borders when computing image derivatives 
	public static BorderType DERIV_BORDER_TYPE = BorderType.EXTENDED;
	public static ImageBorder_I32 DERIV_BORDER_I32 = new ImageBorder1D_I32((Class)BorderIndex1D_Extend.class);
	public static ImageBorder_F32 DERIV_BORDER_F32 = new ImageBorder1D_F32((Class)BorderIndex1D_Extend.class);

	// multiplication factor to go from scale to pixel radius
	public static final double SCALE_SPACE_CANONICAL_RADIUS = 2.5;
}

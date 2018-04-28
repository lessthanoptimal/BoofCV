/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.abst.filter.binary.BinaryLabelContourFinderChang2004;

/**
 * Creates instances of {@link BinaryLabelContourFinder}
 *
 * @author Peter Abeles
 */
public class FactoryBinaryContourFinder {
	/**
	 *
	 * @see boofcv.alg.filter.binary.LinearContourLabelChang2004
	 *
	 * @return new instance
	 */
	public static BinaryLabelContourFinder linearChang2004() {
		if( BOverrideFactoryBinaryContourFinder.chang2004 != null ) {
			return BOverrideFactoryBinaryContourFinder.chang2004.createChang2004();
		} else {
			return new BinaryLabelContourFinderChang2004();
		}
	}

	/**
	 * Binary contour finder for external contours only
	 */
	public static BinaryContourFinder linearExternal() {
		return new BinaryContourFinderLinearExternal();
	}
}

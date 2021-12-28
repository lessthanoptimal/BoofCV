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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.feature.ScalePoint;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageGray;

import java.util.List;

/**
 * Interest point detector for {@link boofcv.struct.gss.GaussianScaleSpace Scale Space} images.
 *
 * @author Peter Abeles
 */
public interface InterestPointScaleSpace<T extends ImageGray<T>, D extends ImageGray<D>> {

	/**
	 * Detect features in the scale space image
	 *
	 * @param ss Scale space of an image
	 */
	void detect( GaussianScaleSpace<T, D> ss );

	/**
	 * Returns all the found interest points
	 *
	 * @return List of found interest points.
	 */
	List<ScalePoint> getInterestPoints();
}

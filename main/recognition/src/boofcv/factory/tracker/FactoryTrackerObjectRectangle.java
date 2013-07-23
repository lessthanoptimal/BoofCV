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

package boofcv.factory.tracker;

import boofcv.abst.tracker.Tld_to_TrackerObjectRectangle;
import boofcv.abst.tracker.TrackerObjectRectangle;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageSingleBand;

/**
 * @author Peter Abeles
 */
public class FactoryTrackerObjectRectangle {

	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectRectangle<T> createTLD( TldConfig<T,D> config ) {
		TldTracker<T,D> tracker = new TldTracker<T,D>(config);

		return new Tld_to_TrackerObjectRectangle<T,D>(tracker);
	}
}

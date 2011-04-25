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

package gecv.alg.tracker.pklt;

import gecv.alg.tracker.klt.KltTracker;
import gecv.struct.image.ImageBase;

/**
 * <p>
 * A pyramid KLT tracker that allows features to be tracker over a larger region than the basic ({@link KltTracker})
 * implementation.  A feature is tracked at multiple resolutions, large motions can be detected at low resolution and
 * are refined at higher resolutions.
 * </p>
 * <p/>
 * <p>
 * Features are tracked at the lowest layer in the pyramid which can contain the feature.  If a feature is contained
 * or not is defined by the basic tracker provided to the pyramid tracker.  In other words, if this tracker can handle
 * partial features then so can the pyramid tracker.
 * </p>
 *
 * @author Peter Abeles
 */
public class PyramidKltTracker<InputImage extends ImageBase, DerivativeImage extends ImageBase> {

	KltTracker<InputImage, DerivativeImage> tracker;
}

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

package gecv.alg.detect.corner;

import gecv.struct.image.ImageBase;

/**
 * <p>
 * Implementation of the Kitchen and Rosenfeld corner detector as described in [1].  Unlike the KLT or Harris corner
 * detectors this corner detector is designed to detect corners on the actual corner.  This operator is mathematically
 * identical to calculating the horizontal curvature of the intensity image.  A reasonable indication of the corner's
 * strength is obtained by multiplying the curvature by the local intensity gradient.
 * </p>
 * <p/>
 * <p>
 * [1] Page 393 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd ed. 2005
 * </p>
 *
 * @author Peter Abeles
 */
public interface KitRosCornerDetector<T extends ImageBase> extends GradientCornerDetector<T> {
}

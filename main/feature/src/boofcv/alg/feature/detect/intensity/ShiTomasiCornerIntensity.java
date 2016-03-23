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

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * This corner detector is designed to select the best features for tracking inside of a Kanade-Lucas-Tomasi (KLT)
 * feature tracker [1].  It selects features which have a low self similarity in all directions.  The output
 * is an intensity image which indicates how corner like each pixel is.  Point features are extracted from the
 * feature intensity image using {@link boofcv.abst.feature.detect.extract.NonMaxSuppression}.
 * </p>
 * <p>
 * An important consideration when using this detector in some applications is that the
 * selected corners will not lie on the actual corner.  They lie inside of the corner itself.  This can bias
 * measurements when used to extract the location of a physical object in an image.
 * </p>
 * <p/>
 * <p>
 * Detected features are square in shape.  The feature's radius is equal to the square's width divided by two and rounded
 * down.
 * </p>
 * <p>
 * [1] Jianbo Shi and Carlo Tomasi. Good Features to Track. IEEE Conference on Computer Vision and Pattern Recognition,
 * pages 593-600, 1994
 * </p>
 *
 * @author Peter Abeles
 */
public interface ShiTomasiCornerIntensity<T extends ImageGray> extends GradientCornerIntensity<T> {

}

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

package gecv.alg.filter.convolve;


/**
 * <p>
 * Specialized convolution where the center of the convolution skips over a constant number
 * of pixels in the x and/or y axis.  The output it written into an image in a dense fashion,
 * resulting in it being at a lower resolution.  A typical application for this is down sampling
 * inside an image pyramid.
 * </p>
 * 
 * @author Peter Abeles
 */
public class ConvolveDownNoBorder {
}

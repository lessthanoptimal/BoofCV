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

package gecv.alg.track.klt;

/**
 * @author Peter Abeles
 */
public class KltFeature {

	/**
	 * The feature's size.  Each feature is square with a width equal to its
	 * radius*2+1.
	 */
	public int radius;

	/**
	 * Pixel intensity around the feature
	 */
	public float pixel[];
	/**
	 * Image derivative around the feature in the x-direction
	 */
	public float derivX[];
	/**
	 * Image derivative around the feature in the y-direction
	 */
	public float derivY[];

	/**
	 * spatial gradient matrix used in updating the feature's position
	 */
	public float Gxx, Gxy, Gyy;
}

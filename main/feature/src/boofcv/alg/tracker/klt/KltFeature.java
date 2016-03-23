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

package boofcv.alg.tracker.klt;

import boofcv.struct.image.GrayF32;

/**
 * Contains feature information for {@link KltTracker}.
 *
 * @author Peter Abeles
 */
public class KltFeature {

	/**
	 * Feature's location inside the image in pixels
	 */
	public float x, y;

	/**
	 * The feature's size.  Each feature is square with a width equal to its
	 * radius*2+1.
	 */
	public int radius;

	/**
	 * Pixel intensity around the feature
	 */
	public GrayF32 desc;
	/**
	 * Image derivative around the feature in the x-direction
	 */
	public GrayF32 derivX;
	/**
	 * Image derivative around the feature in the y-direction
	 */
	public GrayF32 derivY;

	/**
	 * spatial gradient matrix used in updating the feature's position
	 */
	public float Gxx, Gxy, Gyy;

	public KltFeature(int radius) {
		this.radius = radius;
		int sideLength = radius * 2 + 1;

		desc = new GrayF32(sideLength,sideLength);
		derivX = new GrayF32(sideLength,sideLength);
		derivY = new GrayF32(sideLength,sideLength);
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}
}

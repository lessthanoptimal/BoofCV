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

package boofcv.struct.distort;


/**
 * Computes the location of a pixel after an arbitrary distortion has been applied to the image.
 *
 * @author Peter Abeles
 */
public abstract class PixelTransform2_F32 implements PixelTransform {

	// distorted pixel coordinates
	public float distX;
	public float distY;

	public float getDistX() {
		return distX;
	}

	public float getDistY() {
		return distY;
	}
}

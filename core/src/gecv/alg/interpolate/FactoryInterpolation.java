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

package gecv.alg.interpolate;

import gecv.alg.interpolate.impl.BilinearPixel_F32;
import gecv.alg.interpolate.impl.BilinearRectangle_F32;
import gecv.alg.interpolate.impl.NearestNeighborPixel_F32;
import gecv.struct.image.ImageFloat32;

/**
 * Simplified interface for creating interpolation classes.
 *
 * @author Peter Abeles
 */
public class FactoryInterpolation {

	public static InterpolatePixel<ImageFloat32> bilinearPixel_F32() {
		return new BilinearPixel_F32();
	}

	public static InterpolateRectangle<ImageFloat32> bilinearRectangle_F32() {
		return new BilinearRectangle_F32();
	}

	public static InterpolatePixel<ImageFloat32> nearestNeighborPixel_F32() {
		return new NearestNeighborPixel_F32();
	}
}

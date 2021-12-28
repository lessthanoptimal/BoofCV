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

package boofcv.alg.feature.detect.selector;

import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.jetbrains.annotations.Nullable;

/**
 * Classes for sampling the intensity image
 *
 * @author Peter Abeles
 */
@SuppressWarnings("ALL")
public class SampleIntensityImage {
	/**
	 * Implementation for {@link Point2D_I16}
	 */
	public static class I16 implements SampleIntensity<Point2D_I16> {
		@SuppressWarnings({"NullAway"}) // Passing in null for this type is a bug and is easily detected
		@Override
		public float sample( @Nullable GrayF32 intensity, int index, Point2D_I16 p ) {
			return intensity.unsafe_get(p.x, p.y);
		}

		@Override public int getX( Point2D_I16 p ) {return p.x;}

		@Override public int getY( Point2D_I16 p ) {return p.y;}
	}

	/**
	 * Implementation for {@link Point2D_F32}
	 */
	public static class F32 implements SampleIntensity<Point2D_F32> {
		@SuppressWarnings({"NullAway"}) // Passing in null for this type is a bug and is easily detected
		@Override
		public float sample( @Nullable GrayF32 intensity, int index, Point2D_F32 p ) {
			return intensity.unsafe_get((int)p.x, (int)p.y);
		}

		@Override public int getX( Point2D_F32 p ) {return (int)p.x;}

		@Override public int getY( Point2D_F32 p ) {return (int)p.y;}
	}

	/**
	 * Implementation for {@link Point2D_F64}
	 */
	public static class F64 implements SampleIntensity<Point2D_F64> {
		@SuppressWarnings({"NullAway"}) // Passing in null for this type is a bug and is easily detected
		@Override
		public float sample( @Nullable GrayF32 intensity, int index, Point2D_F64 p ) {
			return intensity.unsafe_get((int)p.x, (int)p.y);
		}

		@Override public int getX( Point2D_F64 p ) {return (int)p.x;}

		@Override public int getY( Point2D_F64 p ) {return (int)p.y;}
	}
}

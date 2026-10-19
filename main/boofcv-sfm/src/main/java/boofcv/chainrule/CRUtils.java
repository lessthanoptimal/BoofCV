/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.chainrule.CRArray;
import org.jetbrains.annotations.Nullable;

/// Conversion helpers that copy data types to and from the raw `double[]` / [CRArray] storage
public class CRUtils {

	/// Copies the [Point2D_F32] coordinates (x, y) into `out` at its offset.
	///
	/// @param out Destination; if null, a new [CRArray] backed by a `double[2]` at offset 0 is allocated
	/// @return `out`, or if `out` is null then a the newly allocated [CRArray]
	public static CRArray<double[]> toCRArray( Point2D_F32 p, @Nullable CRArray<double[]> out ) {
		if (out == null)
			out = new CRArray<>(new double[2], 0);

		out.a[out.offset] = p.x;
		out.a[out.offset + 1] = p.y;

		return out;
	}

	/// Copies the [Point2D_F64] coordinates (x, y) into `out` at its offset.
	///
	/// @param out Destination; if null, a new [CRArray] backed by a `double[2]` at offset 0 is allocated
	/// @return `out`, or if `out` is null then a the newly allocated [CRArray]
	public static CRArray<double[]> toCRArray( Point2D_F64 p, @Nullable CRArray<double[]> out ) {
		if (out == null)
			out = new CRArray<>(new double[2], 0);

		out.a[out.offset] = p.x;
		out.a[out.offset + 1] = p.y;

		return out;
	}

	/// Copies the [Point3D_F64] coordinates (x, y, z) into `out` at its offset.
	///
	/// @param out Destination; if null, a new [CRArray] backed by a `double[3]` at offset 0 is allocated
	/// @return `out`, or if `out` is null then a the newly allocated [CRArray]
	public static CRArray<double[]> toCRArray( Point3D_F64 p, @Nullable CRArray<double[]> out ) {
		if (out == null)
			out = new CRArray<>(new double[3], 0);

		out.a[out.offset] = p.x;
		out.a[out.offset + 1] = p.y;
		out.a[out.offset + 2] = p.z;

		return out;
	}

	/// Copies the [Point4D_F64] coordinates (x, y, z, w) into `out` at its offset.
	///
	/// @param out Destination; if null, a new [CRArray] backed by a `double[4]` at offset 0 is allocated
	/// @return `out`, or if `out` is null then a the newly allocated [CRArray]
	public static CRArray<double[]> toCRArray( Point4D_F64 p, @Nullable CRArray<double[]> out ) {
		if (out == null)
			out = new CRArray<>(new double[4], 0);

		out.a[out.offset] = p.x;
		out.a[out.offset + 1] = p.y;
		out.a[out.offset + 2] = p.z;
		out.a[out.offset + 3] = p.w;

		return out;
	}

	/// Writes the 2D point (x, y) into `array` starting at `offset`.
	public static void toArray( double x, double y, double[] array, int offset ) {
		array[offset] = x;
		array[offset + 1] = y;
	}

	/// Writes the 3D point (x, y, z) into `array` starting at `offset`.
	public static void toArray( double x, double y, double z, double[] array, int offset ) {
		array[offset] = x;
		array[offset + 1] = y;
		array[offset + 2] = z;
	}

	/// Writes the 4D point (x, y, z, w) into `array` starting at `offset`.
	public static void toArray( double x, double y, double z, double w, double[] array, int offset ) {
		array[offset] = x;
		array[offset + 1] = y;
		array[offset + 2] = z;
		array[offset + 3] = w;
	}

	/// Reads a [Point4D_F64] (x, y, z, w) from `array` starting at `offset` into `p`.
	public static void toPoint4( double[] array, int offset, Point4D_F64 p ) {
		p.x = array[offset];
		p.y = array[offset + 1];
		p.z = array[offset + 2];
		p.w = array[offset + 3];
	}

	/// Reads a [Point3D_F64] (x, y, z) from `array` starting at `offset` into `p`.
	public static void toPoint3( double[] array, int offset, Point3D_F64 p ) {
		p.x = array[offset];
		p.y = array[offset + 1];
		p.z = array[offset + 2];
	}
}

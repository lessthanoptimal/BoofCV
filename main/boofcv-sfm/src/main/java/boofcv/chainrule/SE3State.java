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

import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;

/// State that encodes a translation and rotation, i.e. SE3 transformation from src to dst.
public class SE3State {
	/// Translational component of SE3
	public final Vector3D_F64 loc = new Vector3D_F64();

	/// Parameterized rotation of SE3.
	public final double[] ori;

	public SE3State( int orientationDof ) {
		ori = new double[orientationDof];
	}

	/// Fill all parameters with the specified value
	public void fill( double value ) {
		loc.setTo(value, value, value);
		Arrays.fill(ori, value);
	}

	public void zero() {
		fill(0.0);
	}

	public void fillNaN() {
		fill(Double.NaN);
	}

	/// Ensures that all parameters are valid
	public void normalize( RotationNumerics rotate ) {
		rotate.normalizeParams(ori, 0);
	}

	/// Read value from an encoded array
	public int decode( double[] array, int offset ) {
		loc.x = array[offset++];
		loc.y = array[offset++];
		loc.z = array[offset++];

		for (int i = 0; i < ori.length; i++) {
			ori[i] = array[offset++];
		}
		return offset;
	}

	/// Writes the value into the array and returns new offset
	public int encode( double[] array, int offset ) {
		array[offset++] = loc.x;
		array[offset++] = loc.y;
		array[offset++] = loc.z;

		for (int i = 0; i < ori.length; i++) {
			array[offset++] = ori[i];
		}
		return offset;
	}

	public int length() {
		return 3 + ori.length;
	}

	/// Encodes the SE3 into a parameter vector
	public static int encode( Se3_F64 src, double[] array, int offset, RotationNumerics rotate ) {
		array[offset++] = src.T.x;
		array[offset++] = src.T.y;
		array[offset++] = src.T.z;
		rotate.matrixToParam(src.R, array, offset);
		return offset + rotate.length();
	}

	/// Encodes the SE3 into a parameter vector
	public static int decode( double[] array, int offset, Se3_F64 dst, RotationNumerics rotate ) {
		dst.T.x = array[offset++];
		dst.T.y = array[offset++];
		dst.T.z = array[offset++];

		rotate.set(array, offset);
		rotate.toMatrix(dst.R);
		return offset + rotate.length();
	}

	/// Converts an [Se3_F64] into this state
	public void setTo( Se3_F64 src, RotationNumerics numerics ) {
		numerics.matrixToParam(src.R, ori, 0);
		loc.setTo(src.T);
	}

	/// Converts the state into [Se3_F64].
	public Se3_F64 toSe3( RotationNumerics numerics, @Nullable Se3_F64 out ) {
		if (out == null)
			out = new Se3_F64();

		numerics.set(ori, 0);
		numerics.toMatrix(out.R);
		out.T.setTo(loc);

		return out;
	}

	@Override public String toString() {
		var df = new DecimalFormat("0.###E0");
		var builder = new StringBuilder();
		builder.append("SE3State {T=[ ").append(df.format(loc.x)).
				append(" , ").append(df.format(loc.y)).append(" , ").append(df.format(loc.z)).append(" ] ");
		builder.append("R=[");
		for (int i = 0; i < ori.length; i++) {
			builder.append(" ").append(df.format(ori[i]));
		}
		builder.append(" ]}");
		return builder.toString();
	}
}

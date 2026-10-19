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

import org.ddogleg.chainrule.sym.Sym;
import org.ddogleg.chainrule.sym.SymOps;

/// [org.ddogleg.chainrule.sym.SymOps] for the geometric operators, which depend on GeoRegression and BoofCV
public class SymGeoOps {
	/// `[R|t]*X`: rigid transform of a point. Input 1 is the pose encoding
	public static Sym se3( RotationNumerics rotate, Sym pose, Sym point ) {
		return Sym.of(new CROperatorSE3(false, rotate)).in0(point).in1(pose);
	}

	/// Pinhole projection of a camera-frame point. Input 1 is the pinhole encoding
	public static Sym pinhole( boolean zeroSkew, double aspectRatio, Sym intrinsics, Sym point ) {
		return Sym.of(new CROperatorCameraPinhole(false, zeroSkew, aspectRatio)).in0(point).in1(intrinsics);
	}
}

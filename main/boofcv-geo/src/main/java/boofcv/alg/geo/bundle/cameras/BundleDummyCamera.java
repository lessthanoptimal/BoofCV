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

package boofcv.alg.geo.bundle.cameras;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

/**
 * Model that does nothing other than throw exceptions. Used to make sure everything is correctly initialized
 * and for null safety.
 *
 * @author Peter Abeles
 */
public class BundleDummyCamera implements BundleAdjustmentCamera {
	public static final BundleDummyCamera INSTANCE = new BundleDummyCamera();

	@Override public void setIntrinsic( double[] parameters, int offset ) {
		throw new RuntimeException("Camera model not initialized correctly");
	}

	@Override public void getIntrinsic( double[] parameters, int offset ) {
		throw new RuntimeException("Camera model not initialized correctly");
	}

	@Override public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
		throw new RuntimeException("Camera model not initialized correctly");
	}

	@Override
	public void jacobian( double camX, double camY, double camZ, double[] pointX, double[] pointY,
						  boolean computeIntrinsic, @Nullable double[] calibX, @Nullable double[] calibY ) {
		throw new RuntimeException("Camera model not initialized correctly");
	}

	@Override public int getIntrinsicCount() {
		throw new RuntimeException("Camera model not initialized correctly");
	}
}

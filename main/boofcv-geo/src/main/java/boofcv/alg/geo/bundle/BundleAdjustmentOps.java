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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.alg.geo.bundle.cameras.BundleKannalaBrandt;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * Operations related to Bundle Adjustment.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentOps {

	/**
	 * Converts the {@link BundleAdjustmentCamera} into {@link CameraPinholeBrown}. Sets the width and height
	 * parameters and if applicable, sets the image center to be the implicit (width/2, height/2)
	 *
	 * @param src (Input) Input camera model.
	 * @param width (Input) Input image width
	 * @param height (Input) Input image height
	 * @param dst (Output) Storage for output.
	 */
	public static CameraPinholeBrown convert( BundleAdjustmentCamera src, int width, int height,
											  @Nullable CameraPinholeBrown dst ) {
		if (dst == null)
			dst = new CameraPinholeBrown();

		if (src instanceof BundlePinhole) {
			dst.fsetRadial().fsetTangental(0, 0); // remove distortion terms
			convert((BundlePinhole)src, width, height, (CameraPinhole)dst);
		} else if (src instanceof BundlePinholeBrown) {
			convert((BundlePinholeBrown)src, width, height, dst);
			dst.fsetShape(width, height);
		} else if (src instanceof BundlePinholeSimplified) {
			convert((BundlePinholeSimplified)src, width, height, dst);
		} else {
			throw new RuntimeException("Unknown src type. " + src);
		}
		return dst;
	}

	/**
	 * Converts {@link BundlePinhole} into {@link CameraPinhole}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static CameraPinhole convert( BundlePinhole src, int width, int height,
										 @Nullable CameraPinhole dst ) {
		if (dst == null)
			dst = new CameraPinhole();

		dst.fx = src.fx;
		dst.fy = src.fy;
		dst.cx = src.cx;
		dst.cy = src.cy;
		if (src.zeroSkew)
			dst.skew = 0;
		else
			dst.skew = src.skew;

		dst.fsetShape(width, height);

		return dst;
	}

	/**
	 * Converts {@link BundlePinholeBrown} into {@link CameraPinholeBrown}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static CameraPinholeBrown convert( BundlePinholeBrown src, int width, int height,
											  @Nullable CameraPinholeBrown dst ) {
		if (dst == null)
			dst = new CameraPinholeBrown();

		dst.fx = src.fx;
		dst.fy = src.fy;
		dst.cx = src.cx;
		dst.cy = src.cy;
		if (src.zeroSkew)
			dst.skew = 0;
		else
			dst.skew = src.skew;
		dst.radial = src.radial.clone();
		if (src.tangential) {
			dst.t1 = src.t1;
			dst.t2 = src.t2;
		} else {
			dst.t1 = dst.t2 = 0;
		}
		dst.fsetShape(width, height);

		return dst;
	}

	/**
	 * Converts {@link BundlePinholeSimplified} into {@link CameraPinholeBrown}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static CameraPinholeBrown convert( BundlePinholeSimplified src, int width, int height,
											  @Nullable CameraPinholeBrown dst ) {
		if (dst == null)
			dst = new CameraPinholeBrown();

		dst.fsetRadial(src.k1, src.k2).fsetTangental(0.0, 0.0);
		dst.fsetK(src.f, src.f, 0.0, width/2, height/2, 0, 0);
		dst.fsetShape(width, height);

		return dst;
	}

	/**
	 * Converts {@link CameraKannalaBrandt} into {@link CameraPinholeBrown}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static CameraKannalaBrandt convert( BundleKannalaBrandt src, int width, int height,
											   @Nullable CameraKannalaBrandt dst ) {
		if (dst == null)
			dst = new CameraKannalaBrandt();

		dst.setTo(src.getModel());
		if (src.zeroSkew)
			dst.skew = 0;

		dst.fsetShape(width, height);

		return dst;
	}

	/**
	 * Converts {@link BundlePinholeSimplified} into {@link CameraPinhole}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static CameraPinhole convert( BundlePinholeSimplified src, int width, int height,
										 @Nullable CameraPinhole dst ) {
		if (dst == null)
			dst = new CameraPinhole();

		dst.fsetK(src.f, src.f, 0.0, width/2, height/2, 0, 0);
		dst.fsetShape(width, height);

		return dst;
	}

	/**
	 * Converts {@link CameraPinholeBrown} into {@link BundlePinhole}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static BundlePinhole convert( CameraPinholeBrown src, @Nullable BundlePinhole dst ) {
		if (dst == null)
			dst = new BundlePinhole();

		dst.zeroSkew = src.skew == 0;
		dst.fx = src.fx;
		dst.fy = src.fy;
		dst.cx = src.cx;
		dst.cy = src.cy;
		dst.skew = src.skew;

		return dst;
	}

	/**
	 * Converts {@link CameraPinholeBrown} into {@link BundlePinholeBrown}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static BundlePinholeBrown convert( CameraPinholeBrown src, @Nullable BundlePinholeBrown dst ) {
		if (dst == null)
			dst = new BundlePinholeBrown();

		if (src.radial == null)
			dst.radial = new double[0];
		else
			dst.radial = src.radial.clone();

		dst.zeroSkew = src.skew == 0;
		dst.fx = src.fx;
		dst.fy = src.fy;
		dst.cx = src.cx;
		dst.cy = src.cy;
		if (src.t1 != 0.0 || src.t2 != 0.0) {
			dst.t1 = src.t1;
			dst.t2 = src.t2;
			dst.tangential = true;
		} else {
			dst.t1 = 0.0;
			dst.t2 = 0.0;
			dst.tangential = false;
		}
		dst.skew = src.skew;

		return dst;
	}

	/**
	 * Converts {@link CameraPinholeBrown} into {@link BundlePinhole}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static BundlePinholeSimplified convert( CameraPinhole src, @Nullable BundlePinholeSimplified dst ) {
		if (dst == null)
			dst = new BundlePinholeSimplified();

		dst.f = (src.fx + src.fy)/2.0;
		dst.k1 = 0.0;
		dst.k2 = 0.0;

		return dst;
	}

	/**
	 * Converts {@link CameraPinhole} into {@link BundlePinhole}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static BundlePinhole convert( CameraPinhole src, @Nullable BundlePinhole dst ) {
		if (dst == null)
			dst = new BundlePinhole();

		dst.setK(src.fx, src.fy, src.skew, src.cx, src.cy);

		return dst;
	}

	/**
	 * Converts {@link DMatrixRMaj} into {@link BundlePinhole}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static BundlePinholeSimplified convert( DMatrixRMaj src, @Nullable BundlePinholeSimplified dst ) {
		if (dst == null)
			dst = new BundlePinholeSimplified();

		dst.f = (src.get(0, 0) + src.get(1, 1))/2.0;
		dst.k1 = 0.0;
		dst.k2 = 0.0;

		return dst;
	}

	/**
	 * Converts {@link BundlePinholeSimplified} into {@link DMatrixRMaj}.
	 *
	 * @param src (Input) Input camera model.
	 * @param dst (Output) Storage for output. If null a new instance is created.
	 * @return The converted camera model
	 */
	public static DMatrixRMaj convert( BundlePinholeSimplified src, @Nullable DMatrixRMaj dst ) {
		if (dst == null)
			dst = new DMatrixRMaj(3, 3);

		dst.reshape(3, 3);
		dst.zero();
		dst.unsafe_set(0, 0, src.f);
		dst.unsafe_set(1, 1, src.f);
		dst.unsafe_set(2, 2, 1);

		return dst;
	}
}

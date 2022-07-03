/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.rectify;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import lombok.Data;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * Describes the geometric meaning of values in a disparity image.
 *
 * @author Peter Abeles
 */
@Data
public class DisparityParameters {
	/** The minimum possible disparity value */
	public int disparityMin;
	/** The number of possible disparity values */
	public int disparityRange;
	/** The baseline between the two views */
	public double baseline;
	/** Rectified camera's intrinsic parameters */
	public final CameraPinhole pinhole = new CameraPinhole();
	/** Rotation from view to rectified synthetic view */
	public final DMatrixRMaj rotateToRectified = CommonOps_DDRM.identity(3);

	// TODO update everything to use rectifiedR

	public DisparityParameters() {}

	public DisparityParameters( int disparityMin, int disparityRange,
								double baseline, CameraPinhole pinhole ) {
		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;
		this.baseline = baseline;
		this.pinhole.setTo(pinhole);
	}

	/**
	 * Give a pixel coordinate and raw disparity value, compute its 3D location. If the point is at infinity
	 * or the disparity value is illegal, then false is returned.
	 *
	 * @param pixelX Pixel coordinate x-axis
	 * @param pixelY Pixel coordinate y-axis
	 * @param value Raw disparity value. DO NOT ADD MIN.
	 * @param location (Output) Computed 3D coordinate
	 * @return true if successful
	 */
	public boolean pixelTo3D( double pixelX, double pixelY, double value, Point3D_F64 location ) {
		if (value >= disparityRange)
			return false;

		value += disparityMin;

		// The point lies at infinity.
		if (value == 0)
			return false;

		// Note that this will be in the rectified left camera's reference frame.
		// An additional rotation is needed to put it into the original left camera frame.
		location.z = baseline*pinhole.fx/value;
		location.x = location.z*(pixelX - pinhole.cx)/pinhole.fx;
		location.y = location.z*(pixelY - pinhole.cy)/pinhole.fy;

		// Bring it back into left camera frame
		GeometryMath_F64.multTran(rotateToRectified, location, location);
		return true;
	}

	/** Resets fields to their initial values */
	public void reset() {
		disparityMin = 0;
		disparityRange = 0;
		baseline = 0;
		pinhole.fsetK(0, 0, 0, 0, 0, 0, 0);
		CommonOps_DDRM.setIdentity(rotateToRectified);
	}

	/**
	 * Makes 'this' a copy of 'src'.
	 *
	 * @param src Set of disparity parameters.
	 */
	public DisparityParameters setTo( DisparityParameters src ) {
		this.disparityMin = src.disparityMin;
		this.disparityRange = src.disparityRange;
		this.baseline = src.baseline;
		this.pinhole.setTo(src.pinhole);
		this.rotateToRectified.setTo(src.rotateToRectified);
		return this;
	}

	/** Checks if specified parameters are valid */
	public void checkValidity() {
		BoofMiscOps.checkTrue(disparityMin >= 0);
		BoofMiscOps.checkTrue(disparityRange > 0);
		BoofMiscOps.checkTrue(baseline > 0);
		BoofMiscOps.checkTrue(pinhole.fx > 0);
	}
}

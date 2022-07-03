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

package boofcv.struct.calib;

import lombok.Getter;
import org.ejml.FancyPrint;

import java.io.Serializable;

/**
 * Division model for lens distortion [1].  p = (1 / (1 + radial*||x||**2))*x, where 'x' is distorted observation
 * and 'p' is undistorted point.
 *
 * [1]  Andrew W. Fitzgibbon, "Simultaneous Linear Estimation of Multiple View Geometry and Lens Distortion" CVPR'01
 *
 * @author Peter Abeles
 */
public class CameraDivision extends CameraPinhole implements Serializable {
	/** Single parameter radial distortion term */
	@Getter public double radial;

	@Override public void reset() {
		super.reset();
		radial = 0;
	}

	@Override public CameraDivision fsetK( double fx, double fy,
									 double skew,
									 double cx, double cy,
									 int width, int height ) {
		return (CameraDivision)super.fsetK(fx, fy, skew, cx, cy, width, height);
	}

	public CameraDivision fsetRadial( double radial ) {
		this.radial = radial;
		return this;
	}

	@Override public <T extends CameraModel> T createLike() {
		return (T)new CameraDivision();
	}

	public CameraDivision setTo( CameraDivision param ) {
		this.radial = param.radial;
		super.setTo(param);
		return this;
	}

	@Override public CameraPinhole setTo( CameraPinhole param ) {
		if (param instanceof CameraDivision) {
			this.setTo((CameraDivision)param);
		} else {
			this.radial = 0;
			super.setTo(param);
		}
		return this;
	}

	public boolean isDistorted() {
		return radial != 0;
	}

	@Override public void print() {
		super.print();
		System.out.printf("radial=%6.2e%n\n", radial);
	}

	@Override public String toString() {
		FancyPrint fp = new FancyPrint();
		String txt = "CameraDivision{" +
				"fx=" + fx +
				", fy=" + fy +
				", skew=" + skew +
				", cx=" + cx +
				", cy=" + cy +
				", width=" + width +
				", height=" + height +
				", radial="+fp.s(radial);
		txt += '}';
		return txt;
	}
}

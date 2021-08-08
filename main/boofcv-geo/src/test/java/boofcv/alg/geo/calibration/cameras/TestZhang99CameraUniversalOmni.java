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

package boofcv.alg.geo.calibration.cameras;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.GenericCalibrationZhang99;
import boofcv.struct.calib.CameraUniversalOmni;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.ejml.UtilEjml.EPS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestZhang99CameraUniversalOmni
		extends GenericCalibrationZhang99<CameraUniversalOmni> {

	@Override
	public List<CameraConfig> createCamera( Random rand ) {
		List<OmniConfig> list = new ArrayList<>();

		list.add(createStandard(false, false, 0, 0, rand));
		list.add(createStandard(true, true, 2, 0, rand));
		list.add(createStandard(false, true, 2, 0, rand));
		list.add(createStandard(true, false, 2, 0, rand));

		list.add(createFisheye(true));
		list.add(createFisheye(false));

		return (List)list;
	}

	@Override
	public List<CameraConfig> createCameraForLinearTests( Random rand ) {
		List<OmniConfig> list = new ArrayList<>();

		// tangent can't be linearly estimated
		list.add(createStandard(false, false, 0, 0, rand));
		list.add(createStandard(true, false, 0, 0, rand));

		// radial distortion has to be very small for parameters to have a decent estimate
		// the estimate it does come up with is much better than starting from zero but has
		// large error
//		list.add( createStandard(true,false,2,rand));

		return (List)list;
	}

	public OmniConfig createStandard( boolean zeroSkew,
									  boolean tangent, int numRadial,
									  double mirror,
									  Random rand ) {

		OmniConfig p = new OmniConfig(zeroSkew, tangent, numRadial, true);

		p.model.cx = 255;
		p.model.cy = 260;
		p.model.fx = 1250;
		p.model.fy = 900;

		if (zeroSkew)
			p.model.skew = 0;
		else
			p.model.skew = 1.09083;

		for (int i = 0; i < p.model.radial.length; i++) {
			p.model.radial[i] = rand.nextGaussian()*0.05;
		}

		if (p.includeTangential) {
			p.model.t1 = rand.nextGaussian()*0.02;
			p.model.t2 = rand.nextGaussian()*0.02;
		}

		p.model.mirrorOffset = mirror;

		return p;
	}

	public OmniConfig createFisheye( boolean fixedMirror ) {
		OmniConfig p = new OmniConfig(true, true, 2, fixedMirror);

		p.model.fx = 562.90;
		p.model.fy = 563.58;
		p.model.cx = 239.72;
		p.model.cy = 239.72;
		p.model.skew = 0.0;
		p.model.radial[0] = 0.226573352659;
		p.model.radial[1] = 6.72940754992;
		p.model.t1 = 0.004624464338;
		p.model.t2 = 9.66390674543E-4;
		p.model.mirrorOffset = 2.94487011878;

		return p;
	}

	@Override
	public Zhang99Camera createGenerator( CameraConfig config, List<Point2D_F64> layout ) {
		OmniConfig c = (OmniConfig)config;
		Zhang99CameraUniversalOmni a = new Zhang99CameraUniversalOmni(
				layout, c.assumeZeroSkew, c.includeTangential, c.numRadial);
		a.fixedMirror = c.fixedMirror;
		return a;
	}

	@Override
	public DMatrixRMaj cameraToK( CameraConfig config ) {
		return PerspectiveOps.pinholeToMatrix(config.model, (DMatrixRMaj)null);
	}

	@Override
	protected void checkIntrinsicOnly( CameraUniversalOmni expected, CameraUniversalOmni found,
									   double tolK, double tolD, double tolT ) {
		assertEquals(expected.fx, found.fx, Math.abs(expected.fx)*tolK + EPS);
		assertEquals(expected.fy, found.fy, Math.abs(expected.fy)*tolK + EPS);
		assertEquals(expected.skew, found.skew, Math.abs(expected.skew)*tolK + EPS);
		assertEquals(expected.cx, found.cx, Math.abs(expected.cx)*tolK + EPS);
		assertEquals(expected.cy, found.cy, Math.abs(expected.cy)*tolK + EPS);

		for (int i = 0; i < expected.radial.length; i++) {
			assertEquals(expected.radial[i], found.radial[i], tolD);
		}
		assertEquals(expected.t1, found.t1, tolT);
		assertEquals(expected.t2, found.t2, tolT);

		assertEquals(expected.mirrorOffset, found.mirrorOffset, tolT);
	}

	private class OmniConfig extends CameraConfig {
		boolean assumeZeroSkew;
		boolean includeTangential;
		int numRadial;
		boolean fixedMirror;

		public OmniConfig( boolean zeroSkew,
						   boolean tangent, int numRadial, boolean fixedMirror ) {
			this.assumeZeroSkew = zeroSkew;
			this.includeTangential = tangent;
			this.numRadial = numRadial;
			this.fixedMirror = fixedMirror;
			this.model = new CameraUniversalOmni(numRadial);
		}
	}
}

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
import boofcv.struct.calib.CameraKannalaBrandt;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.ejml.UtilEjml.EPS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestZhang99CameraKannalaBrandt extends GenericCalibrationZhang99<CameraKannalaBrandt> {

	public TestZhang99CameraKannalaBrandt() {
		// needed to relax this a little for when there are asymmetric terms
		reprojectionTol = 1e-2;
	}

	@Override
	public Zhang99Camera createGenerator( CameraConfig config, List<Point2D_F64> layout ) {
		KannalaBrandtConfig c = (KannalaBrandtConfig)config;
		return new Zhang99CameraKannalaBrandt(c.assumeZeroSkew, c.numSymmetric, c.numAsymmetric);
	}

	@Override
	public DMatrixRMaj cameraToK( CameraConfig config ) {
		return PerspectiveOps.pinholeToMatrix(config.model, (DMatrixRMaj)null);
	}

	@Override
	public List<CameraConfig> createCamera( Random rand ) {
		List<KannalaBrandtConfig> list = new ArrayList<>();

		list.add(createStandard(false, 5, 0, rand));
		list.add(createStandard(true, 5, 0, rand));
		list.add(createStandard(false, 5, 2, rand));
		list.add(createStandard(true, 5, 2, rand));

		return (List)list;
	}

	@Override @Test
	public void linearEstimate() {
		// The analogous parameters for pinhole in KB will not match a pure pinhole model
	}

	@Override public List<CameraConfig> createCameraForLinearTests( Random rand ) {
		throw new RuntimeException("Not supported");
	}

	public KannalaBrandtConfig createStandard( boolean zeroSkew,
											   int numSymmetric, int numAsymmetric,
											   Random rand ) {

		var p = new KannalaBrandtConfig(zeroSkew, numSymmetric, numAsymmetric);

		p.model.cx = 600;
		p.model.cy = 650;
		p.model.fx = 500;
		p.model.fy = 550;

		if (zeroSkew)
			p.model.skew = 0;
		else
			p.model.skew = 0.05;

		p.model.symmetric[0] = 1.0;
		if (p.model.symmetric.length > 1)
			p.model.symmetric[1] = 0.1;

		for (int i = 0; i < numAsymmetric; i++) {
			p.model.radial[i] = rand.nextGaussian()*0.05;
			p.model.tangent[i] = rand.nextGaussian()*0.05;
		}

		for (int i = 0; i < p.model.radialTrig.length; i++) {
			p.model.radialTrig[i] = rand.nextGaussian()*0.01;
			p.model.tangentTrig[i] = rand.nextGaussian()*0.01;
		}

		return p;
	}

	@Override
	protected void checkIntrinsicOnly( CameraKannalaBrandt expected, CameraKannalaBrandt found,
									   double tolK, double tolD, double tolT ) {
		assertEquals(expected.fx, found.fx, Math.abs(expected.fx)*tolK + EPS);
		assertEquals(expected.fy, found.fy, Math.abs(expected.fy)*tolK + EPS);
		assertEquals(expected.skew, found.skew, Math.abs(expected.skew)*tolK + EPS);
		assertEquals(expected.cx, found.cx, Math.abs(expected.cx)*tolK + EPS);
		assertEquals(expected.cy, found.cy, Math.abs(expected.cy)*tolK + EPS);

		for (int i = 0; i < expected.radial.length; i++) {
			assertEquals(expected.radial[i], found.radial[i], tolD);
			assertEquals(expected.tangent[i], found.tangent[i], tolD);
		}
		for (int i = 0; i < expected.tangentTrig.length; i++) {
			assertEquals(expected.radialTrig[i], found.radialTrig[i], tolT);
			assertEquals(expected.tangentTrig[i], found.tangentTrig[i], tolT);
		}
	}

	private class KannalaBrandtConfig extends CameraConfig {
		boolean assumeZeroSkew;
		int numSymmetric;
		int numAsymmetric;

		public KannalaBrandtConfig( boolean assumeZeroSkew, int numSymmetric, int numAsymmetric ) {
			this.assumeZeroSkew = assumeZeroSkew;
			this.numSymmetric = numSymmetric;
			this.numAsymmetric = numAsymmetric;
			this.model = new CameraKannalaBrandt(numSymmetric, numAsymmetric);
		}
	}
}

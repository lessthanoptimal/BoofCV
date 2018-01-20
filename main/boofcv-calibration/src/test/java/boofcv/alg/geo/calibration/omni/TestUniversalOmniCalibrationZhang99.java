/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration.omni;

import boofcv.alg.geo.calibration.GenericCalibrationZhang99;
import boofcv.alg.geo.calibration.Zhang99IntrinsicParam;
import boofcv.struct.calib.CameraUniversalOmni;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUniversalOmniCalibrationZhang99
		extends GenericCalibrationZhang99<CameraUniversalOmni>
{

	@Override
	public List<Zhang99IntrinsicParam> createParameters(Random rand) {
		List<Zhang99IntrinsicParam> list = new ArrayList<>();

		list.add( createStandard(false,false,0,0,rand));
		list.add( createStandard(true,true,2,0,rand));
		list.add( createStandard(false,true,2,0,rand));
		list.add( createStandard(true,false,2,0,rand));

		list.add( createFisheye(true));
		list.add( createFisheye(false));

		return list;
	}

	@Override
	public List<Zhang99IntrinsicParam> createParametersForLinearTest(Random rand) {
		List<Zhang99IntrinsicParam> list = new ArrayList<>();

		// tangent can't be linearly estimated
		list.add( createStandard(false,false,0,0,rand));
		list.add( createStandard(true,false,0,0,rand));

		// radial distortion has to be very small for parameters to have a decent estimate
		// the estimate it does come up with is much better than starting from zero but has
		// large error
//		list.add( createStandard(true,false,2,rand));

		return list;
	}

	public static Zhang99IntrinsicParam createStandard( boolean zeroSkew ,
														boolean tangent , int numRadial ,
														double mirror ,
														Random rand) {

		CalibParamUniversalOmni p = new CalibParamUniversalOmni(zeroSkew,numRadial,tangent,true);

		p.intrinsic.cx = 255;
		p.intrinsic.cy = 260;
		p.intrinsic.fx = 1250;
		p.intrinsic.fy = 900;
		p.intrinsic.skew = 1.09083;

		if( zeroSkew )
			p.intrinsic.skew = 0;

		for( int i = 0; i < p.intrinsic.radial.length;i++ ) {
			p.intrinsic.radial[i] = rand.nextGaussian()*0.05;
		}

		if( p.includeTangential ) {
			p.intrinsic.t1 = rand.nextGaussian()*0.02;
			p.intrinsic.t2 = rand.nextGaussian()*0.02;
		}

		p.intrinsic.mirrorOffset = mirror;

		p.forceProjectionUpdate();

		return p;
	}

	public static Zhang99IntrinsicParam createFisheye( boolean fixedMirror ) {
		CalibParamUniversalOmni p = new CalibParamUniversalOmni(true,2,true,fixedMirror);

		p.intrinsic.fx = 562.90;
		p.intrinsic.fy = 563.58;
		p.intrinsic.cx = 239.72;
		p.intrinsic.cy = 239.72;
		p.intrinsic.skew = 0.0;
		p.intrinsic.radial[0] = 0.226573352659;
		p.intrinsic.radial[1] = 6.72940754992;
		p.intrinsic.t1 = 0.004624464338;
		p.intrinsic.t2 = 9.66390674543E-4;
		p.intrinsic.mirrorOffset = 2.94487011878;
		p.forceProjectionUpdate();

		return p;
	}

	@Override
	public void addNoise(CameraUniversalOmni param, double magnitude) {
		param.fx += rand.nextDouble()*magnitude*Math.abs(param.fx);
		param.fy += rand.nextDouble()*magnitude*Math.abs(param.fy);
		param.skew += rand.nextDouble()*magnitude*Math.abs(param.skew);
		param.cx += rand.nextDouble()*magnitude*Math.abs(param.cx);
		param.cy += rand.nextDouble()*magnitude*Math.abs(param.cy);

		param.t1 += rand.nextDouble()*magnitude*Math.abs(param.t1);
		param.t2 += rand.nextDouble()*magnitude*Math.abs(param.t2);

		param.mirrorOffset += rand.nextGaussian()*magnitude*0.01;

		for( int i = 0; i < param.radial.length; i++ ) {
			param.radial[i] = rand.nextGaussian()*param.radial[i]*magnitude*10;
		}
	}

	@Override
	protected void checkIntrinsicOnly(CameraUniversalOmni expected, CameraUniversalOmni found,
									  double tolK, double tolD, double tolT) {
		assertEquals(expected.fx,found.fx,Math.abs(expected.fx)*tolK);
		assertEquals(expected.fy,found.fy,Math.abs(expected.fy)*tolK);
		assertEquals(expected.skew,found.skew,Math.abs(expected.skew)*tolK);
		assertEquals(expected.cx,found.cx,Math.abs(expected.cx)*tolK);
		assertEquals(expected.cy, found.cy, Math.abs(expected.cy) * tolK);

		for( int i = 0; i < expected.radial.length; i++ ) {
			assertEquals(expected.radial[i],found.radial[i],tolD);
		}
		assertEquals(expected.t1,found.t1,tolT);
		assertEquals(expected.t2,found.t2,tolT);

		assertEquals(expected.mirrorOffset,found.mirrorOffset,tolT);
	}

	@Override
	public void checkEquals(CameraUniversalOmni expected,
							CameraUniversalOmni found,
							CameraUniversalOmni initial, double tol) {
		// see if it improved the estimate
		assertTrue(Math.abs(expected.fx-initial.fx)*tol >= Math.abs(expected.fx-found.fx));
		assertTrue(Math.abs(expected.fy-initial.fy)*tol >= Math.abs(expected.fy-found.fy));
		assertEquals(expected.skew, found.skew, tol*0.1);
		assertTrue(Math.abs(expected.cx-initial.cx)*tol >= Math.abs(expected.cx-found.cx));
		assertTrue(Math.abs(expected.cy-initial.cy)*tol >= Math.abs(expected.cy-found.cy));

		for( int i = 0; i < expected.radial.length; i++ ) {
			double e = expected.radial[i];
			double f = found.radial[i];
			double init = initial.radial[i];
			assertTrue(Math.abs(init - f) * 0.5 >= Math.abs(f - e));
		}

		assertTrue(Math.abs(expected.t1 - found.t1) <= Math.abs(initial.t1 - found.t1));
		assertTrue(Math.abs(expected.t2 - found.t2) <= Math.abs(initial.t2 - found.t2));

		assertTrue(Math.abs(expected.mirrorOffset - found.mirrorOffset)
				<= Math.abs(initial.mirrorOffset - found.mirrorOffset));
	}
}

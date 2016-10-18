/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.sfot;

import boofcv.abst.distort.FDistort;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.RectangleRotate_F64;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.metric.UtilAngle;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSparseFlowObjectTracker {

	Random rand = new Random(2342);

	@Test
	public void noMotion() {
		checkMotion(0,0,0);
	}

	@Test
	public void translation() {
		checkMotion(0, 0, 0);
		checkMotion(0,-7.6,0);
	}

	@Test
	public void rotation() {
		checkMotion(0, 0, 0.05);
		checkMotion(0,0,-0.05);
	}

	@Test
	public void both() {
		checkMotion(10, -7.6, 0.05);
	}

	protected void checkMotion( double tranX , double tranY , double rot ) {
		GrayU8 frame0 = new GrayU8(320,240);
		GrayU8 frame1 = new GrayU8(320,240);
		ImageMiscOps.fillUniform(frame0,rand,0,256);

		double c = Math.cos(rot);
		double s = Math.sin(rot);

		new FDistort(frame0,frame1).affine(c,-s,s,c,tranX,tranY).apply();

		SfotConfig config = new SfotConfig();

		ImageGradient<GrayU8,GrayS16> gradient = FactoryDerivative.sobel(GrayU8.class,GrayS16.class);

		SparseFlowObjectTracker<GrayU8,GrayS16> alg = new SparseFlowObjectTracker<>(
				config, GrayU8.class, GrayS16.class, gradient);

		RectangleRotate_F64 region0 = new RectangleRotate_F64(120,140,30,40,0.1);
		RectangleRotate_F64 region1 = new RectangleRotate_F64();

		alg.init(frame0,region0);
		assertTrue(alg.update(frame1,region1));

		double expectedX = c*region0.cx - s*region0.cy + tranX;
		double expectedY = s*region0.cx + c*region0.cy + tranY;
		double expectedYaw = UtilAngle.bound(region0.theta + rot);


		assertEquals(expectedX, region1.cx, 0.5);
		assertEquals(expectedY, region1.cy, 0.5);
		assertEquals(expectedYaw, region1.theta, 0.01);
	}

}

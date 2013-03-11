/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.color;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestColorHsv {
	double tol = 1e-5;


	@Test
	public void backAndForth_F64() {
		double hsv[] = new double[3];
		double rgb[] = new double[3];

		ColorHsv.rgbToHsv(0.5, 0.3, 0.2, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0.5, 0.3, 0.2);

		ColorHsv.rgbToHsv(0, 0, 0, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0, 0, 0);

		ColorHsv.rgbToHsv(1, 0, 0, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,1, 0, 0);

		ColorHsv.rgbToHsv(0, 1, 0, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0, 1, 0);

		ColorHsv.rgbToHsv(0, 0, 1, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0, 0, 1);

		ColorHsv.rgbToHsv(0.5, 0.5, 0.5, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0.5, 0.5, 0.5);

		ColorHsv.rgbToHsv(0.25, 0.5, 0.75, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0.25, 0.5, 0.75);

		ColorHsv.rgbToHsv(0.8, 0.1, 0.75, hsv);
		ColorHsv.hsvToRgb(hsv[0],hsv[1],hsv[2],rgb);
		check(rgb,0.8, 0.1, 0.75);
	}

	private static void check( double found[] , double a , double b , double c ) {
		double tol = 0.01;

		assertEquals(a,found[0],tol);
		assertEquals(b,found[1],tol);
		assertEquals(c,found[2],tol);
	}
}

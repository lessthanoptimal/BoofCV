/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point3Transform2_F32;
import georegression.struct.point.Point2D_F32;

/**
 * @author Peter Abeles
 */
public class SphereToNarrowPixel_F32 implements Point3Transform2_F32 {

	Point2Transform2_F32 projToPixel;

	public SphereToNarrowPixel_F32(Point2Transform2_F32 projToPixel) {
		this.projToPixel = projToPixel;
	}

	@Override
	public void compute(float x, float y, float z, Point2D_F32 out) {
		// ignore the whole z <= 0 issue with it being behind the camera. No way to tell it that it failed
		projToPixel.compute(x/z,y/z, out);
	}
}

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

package boofcv.abst.sfm;

import boofcv.alg.sfm.DepthSparse3D;
import boofcv.struct.image.ImageGray;

/**
 * Wrapper around {@link DepthSparse3D} for {@link ImagePixelTo3D}.
 *
 * @author Peter Abeles
 */
public class DepthSparse3D_to_PixelTo3D<T extends ImageGray>
	implements ImagePixelTo3D
{
	DepthSparse3D<T> alg;

	public DepthSparse3D_to_PixelTo3D(DepthSparse3D<T> alg) {
		this.alg = alg;
	}

	@Override
	public boolean process(double x, double y) {
		return alg.process((int)x,(int)y);
	}

	@Override
	public double getX() {
		return alg.getWorldPt().x;
	}

	@Override
	public double getY() {
		return alg.getWorldPt().y;
	}

	@Override
	public double getZ() {
		return alg.getWorldPt().z;
	}

	@Override
	public double getW() {
		return 1;
	}
}

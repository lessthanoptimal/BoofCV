/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.distort;

import boofcv.alg.distort.mls.ImageDeformPointMLS_F32;
import georegression.struct.point.Point2D_F32;

import java.util.List;

/**
 * Wrapper around {@link ImageDeformPointMLS_F32} for {@link PointDeformKeyPoints}
 *
 * @author Peter Abeles
 */
public class PointDeform_MLS implements PointDeformKeyPoints
{
	ImageDeformPointMLS_F32 alg;

	int rows,cols;

	public PointDeform_MLS(ImageDeformPointMLS_F32 alg , int rows, int cols) {
		this.alg = alg;
		this.rows = rows;
		this.cols = cols;
	}

	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		alg.compute(x,y, out);
	}

	@Override
	public PointDeform_MLS copyConcurrent() {
		return new PointDeform_MLS(alg.copyConcurrent(),rows,cols);
	}

	@Override
	public void setImageShape(int width, int height) {
		alg.configure(width,height,rows,cols);
	}

	@Override
	public void setSource(List<Point2D_F32> locations) {
		alg.reset();
		for (int i = 0; i < locations.size(); i++) {
			Point2D_F32 p = locations.get(i);
			alg.addControl(p.x, p.y);
		}
		alg.fixate();
	}

	@Override
	public void setDestination(List<Point2D_F32> locations) {
		for (int i = 0; i < locations.size(); i++) {
			Point2D_F32 p = locations.get(i);
			alg.setDistorted(i, p.x, p.y);
		}
		alg.fixate();
	}

	@Override
	public void setSource(int which, float x, float y) {
		alg.setUndistorted(which, x , y );
		alg.fixate();
	}

	@Override
	public void setDestination(int which, float x, float y) {
		alg.setDistorted(which, x , y );
		alg.fixate();
	}

	public ImageDeformPointMLS_F32 getAlgorithm() {
		return alg;
	}
}

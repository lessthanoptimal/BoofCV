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

package boofcv.abst.fiducial;

import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Wrapper around {@link BaseDetectFiducialSquare} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public class BaseSquare_FiducialDetector<T extends ImageGray,Detector extends BaseDetectFiducialSquare<T>>
	implements FiducialDetector<T>
{
	Detector alg;

	// type of image it can process
	ImageType<T> type;

	// storage for undistorted fiducial corner quadrilateral
	Quadrilateral_F64 undistQuad = new Quadrilateral_F64();

	// Used for finding the center of the square
	private LineGeneral2D_F64 line02 = new LineGeneral2D_F64();
	private LineGeneral2D_F64 line13 = new LineGeneral2D_F64();

	protected Point2Transform2_F64 distToUndist = new DoNothing2Transform2_F64();
	protected Point2Transform2_F64 undistToDist = new DoNothing2Transform2_F64();

	public BaseSquare_FiducialDetector(Detector alg) {
		this.alg = alg;
		this.type = ImageType.single(alg.getInputType());
	}

	@Override
	public void detect(T input) {
		alg.process(input);
	}
	/**
	 * Return the intersection of two lines defined by opposing corners.  This should also be the geometric center
	 * @param which Fiducial's index
	 * @param location (output) Storage for the transform. modified.
	 */
	@Override
	public void getImageLocation(int which, Point2D_F64 location) {
		FoundFiducial f = alg.getFound().get(which);

		distToUndist.compute(f.locationPixels.a.x,f.locationPixels.a.y, undistQuad.a);
		distToUndist.compute(f.locationPixels.b.x,f.locationPixels.b.y, undistQuad.b);
		distToUndist.compute(f.locationPixels.c.x,f.locationPixels.c.y, undistQuad.c);
		distToUndist.compute(f.locationPixels.d.x,f.locationPixels.d.y, undistQuad.d);


		// compute intersection in undistorted pixels so that the intersection is the true
		// geometric center of the square
		UtilLine2D_F64.convert(undistQuad.a, undistQuad.c,line02);
		UtilLine2D_F64.convert(undistQuad.b, undistQuad.d,line13);

		Intersection2D_F64.intersection(line02,line13,location);

		// apply lens distortion to the point so that it appears in the correct location
		undistToDist.compute(location.x,location.y, location);
	}

	@Override
	public int totalFound() {
		return alg.getFound().size;
	}

	@Override
	public long getId( int which ) {
		return alg.getFound().get(which).id;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	public Detector getAlgorithm() {
		return alg;
	}
}

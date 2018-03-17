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

package boofcv.abst.fiducial;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.alg.fiducial.square.QuadPoseEstimator;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link BaseDetectFiducialSquare} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public abstract class SquareBase_to_FiducialDetector<T extends ImageGray<T>,Detector extends BaseDetectFiducialSquare<T>>
	extends FiducialDetectorPnP<T>
{
	Detector alg;

	// type of image it can process
	ImageType<T> type;

	// Used for finding the center of the square
	private LineGeneral2D_F64 line02 = new LineGeneral2D_F64();
	private LineGeneral2D_F64 line13 = new LineGeneral2D_F64();

	// used to compute 3D pose of target
	QuadPoseEstimator poseEstimator = new QuadPoseEstimator(1e-6,200);

	Quadrilateral_F64 quad = new Quadrilateral_F64();
	List<PointIndex2D_F64> listQuad = new ArrayList<>();

	List<Point2D3D> points2D3D;

	public SquareBase_to_FiducialDetector(Detector alg) {
		this.alg = alg;
		this.type = ImageType.single(alg.getInputType());

		// add corner points in target frame.  Used to compute homography.  Target's center is at its origin
		// see comment in class JavaDoc above.  Note that the target's length is one below.  The scale factor
		// will be provided later one
		poseEstimator.setFiducial(-0.5, 0.5, 0.5, 0.5, 0.5, -0.5, -0.5, -0.5);
		points2D3D = poseEstimator.createCopyPoints2D3D();

		for (int i = 0; i < 4; i++) {
			listQuad.add( new PointIndex2D_F64());
			listQuad.get(i).index = i;
		}
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
	public void getCenter(int which, Point2D_F64 location) {
		Quadrilateral_F64 q = alg.getFound().get(which).distortedPixels;

		// compute intersection in undistorted pixels so that the intersection is the true
		// geometric center of the square. Since distorted pixels are being used this will only be approximate
		UtilLine2D_F64.convert(q.a, q.c,line02);
		UtilLine2D_F64.convert(q.b, q.d,line13);

		Intersection2D_F64.intersection(line02,line13,location);
	}

	@Override
	protected boolean estimatePose( int which, List<Point2D3D> points , Se3_F64 fiducialToCamera ) {
		quad.a.set( points.get(0).observation );
		quad.b.set( points.get(1).observation );
		quad.c.set( points.get(2).observation );
		quad.d.set( points.get(3).observation );

		if( !poseEstimator.process(quad,false) ) {
			return false;
		}

		fiducialToCamera.set( poseEstimator.getWorldToCamera() );
		double width = getWidth(which);
		fiducialToCamera.T.x *= width;
		fiducialToCamera.T.y *= width;
		fiducialToCamera.T.z *= width;
		return true;
	}

	@Override
	public void setLensDistortion(LensDistortionNarrowFOV distortion, int width, int height ) {
		super.setLensDistortion(distortion,width,height);
		if( distortion != null )
			poseEstimator.setLensDistoriton(distortion);
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
	public String getMessage(int which) {return null;}

	@Override
	public boolean hasUniqueID() {
		return true;
	}

	@Override
	public boolean hasMessage() {
		return false;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	public Detector getAlgorithm() {
		return alg;
	}

	@Override
	public List<PointIndex2D_F64> getDetectedControl(int which) {
		FoundFiducial found = getAlgorithm().getFound().get(which);
		listQuad.get(0).set( found.distortedPixels.a );
		listQuad.get(1).set( found.distortedPixels.b );
		listQuad.get(2).set( found.distortedPixels.c );
		listQuad.get(3).set( found.distortedPixels.d );

		return listQuad;
	}

	@Override
	protected List<Point2D3D> getControl3D(int which) {
		return points2D3D;
	}

	@Override
	public Polygon2D_F64 getBounds(int which, @Nullable Polygon2D_F64 storage) {
		if( storage == null )
			storage = new Polygon2D_F64();
		else
			storage.vertexes.reset();

		FoundFiducial found = getAlgorithm().getFound().get(which);
		storage.vertexes.grow().set(found.distortedPixels.a);
		storage.vertexes.grow().set(found.distortedPixels.b);
		storage.vertexes.grow().set(found.distortedPixels.c);
		storage.vertexes.grow().set(found.distortedPixels.d);

		return storage;
	}

}

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

package boofcv.alg.shapes.polygon;

import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.edge.EdgeIntensityPolygon;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.Polygon2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO Comment
 *
 * @author Peter Abeles
 */
public class DetectPolygonBinaryGrayRefine<T extends ImageGray<T>> {

	// Detects the polygons using a contour from a binary image
	private DetectPolygonFromContour<T> detector;

	// Refines the edges using the contour alone
	private RefinePolygonToContour refineContour;

	// Refines the edges using the gray scale image
	private RefinePolygonToGray<T> refineGray;

	// Used to remove false positives
	private EdgeIntensityPolygon<T> edgeIntensity;

	private Polygon2D_F64 work = new Polygon2D_F64();

	public DetectPolygonBinaryGrayRefine(DetectPolygonFromContour<T> detector,
										 RefinePolygonToContour refineContour,
										 RefinePolygonToGray<T> refineGray) {
		this.detector = detector;
		this.refineContour = refineContour;
		this.refineGray = refineGray;

		this.edgeIntensity = new EdgeIntensityPolygon<>(1, 1.5, 15,
				detector.getInputType());
	}

	public void setHelper( PolygonHelper helper ) {
		detector.setHelper(helper);
	}

	public void setVerbose( boolean verbose ) {
		detector.setVerbose(verbose);
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates.  The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width.  Used in sanity check only.
	 * @param height Input image height.  Used in sanity check only.
	 * @param distToUndist Transform from distorted to undistorted image.
	 * @param undistToDist Transform from undistorted to distorted image.
	 */
	public void setLensDistortion(int width , int height ,
								  PixelTransform2_F32 distToUndist , PixelTransform2_F32 undistToDist ) {
		detector.setLensDistortion(width, height, distToUndist, undistToDist);
		refineGray.setLensDistortion(width, height, distToUndist, undistToDist);
		edgeIntensity.setTransform(undistToDist);
	}

	/**
	 * Discard previously set lens distortion models
	 */
	public void clearLensDistortion() {
		detector.clearLensDistortion();
		refineGray.clearLensDistortion();
		edgeIntensity.setTransform(null);
	}

	public void process(T gray , GrayU8 binary ) {
		detector.process(gray,binary);
		refineGray.setImage(gray);
		edgeIntensity.setImage(gray);
	}

	public void refineContour(DetectPolygonFromContour.Info polygon ) {
		refineContour.process(polygon.contour,polygon.splits,polygon.polygon);

		// todo save edge intensity.  NEED SAME INENSITY

	}

	public boolean refineUsingEdge(DetectPolygonFromContour.Info polygon ) {
		work.set(polygon.polygon);
		return refineGray.refine(work,polygon.polygon);

		// todo save edge intensity.  NEED SAME INENSITY
	}

	public void refineAll() {
		List<DetectPolygonFromContour.Info> detections = detector.getFound().toList();
		double minEdgeIntensity = detector.getContourEdgeThreshold();

		for (int i = 0; i < detections.size(); i++) {
			DetectPolygonFromContour.Info d = detections.get(i);
			refineContour(d);
			if( d.computeEdgeIntensity() >= minEdgeIntensity )
				refineUsingEdge(d);
		}
	}

	public List<Polygon2D_F64> getPolygons( List<Polygon2D_F64> storage ) {
		if( storage == null )
			storage = new ArrayList<>();
		else
			storage.clear();

		double minEdgeIntensity = detector.getContourEdgeThreshold();

		List<DetectPolygonFromContour.Info> detections = detector.getFound().toList();
		for (int i = 0; i < detections.size(); i++) {
			DetectPolygonFromContour.Info d = detections.get(i);

			if( d.computeEdgeIntensity() >= minEdgeIntensity )
				storage.add( detections.get(i).polygon );
		}
		return storage;
	}

	public List<DetectPolygonFromContour.Info> getPolygonInfo() {
		return detector.getFound().toList();
	}

	public Class<T> getInputType() {
		return detector.getInputType();
	}

	public int getMinimumSides() {
		return detector.getMinimumSides();
	}

	public int getMaximumSides() {
		return detector.getMaximumSides();
	}

	public boolean isOutputClockwise() {
		return detector.isOutputClockwise();
	}

	public DetectPolygonFromContour<T> getDetector() {
		return detector;
	}

	public List<Contour> getAllContours() {
		return detector.getAllContours();
	}
}

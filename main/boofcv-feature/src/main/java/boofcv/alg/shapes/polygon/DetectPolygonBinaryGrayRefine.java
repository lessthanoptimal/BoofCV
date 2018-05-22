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

package boofcv.alg.shapes.polygon;

import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.shapes.edge.EdgeIntensityPolygon;
import boofcv.misc.MovingAverage;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects polygons using contour of blobs in a binary image. The contours can then have their edges refined as a
 * whole or on an individual basis. Lens distortion can be specified. Lens distortion is handled in a sparse way
 * along the contour of shapes.
 *
 * @author Peter Abeles
 */
public class DetectPolygonBinaryGrayRefine<T extends ImageGray<T>> {

	// Detects the polygons using a contour from a binary image
	DetectPolygonFromContour<T> detector;

	AdjustPolygonForThresholdBias adjustForBias;

	// Refines the edges using the contour alone
	private RefinePolygonToContour refineContour;

	// Refines the edges using the gray scale image
	private RefinePolygonToGray<T> refineGray;

	// Used to remove false positives
	private EdgeIntensityPolygon<T> edgeIntensity;

	private Polygon2D_F64 work = new Polygon2D_F64();

	// useful for customization
	AdjustBeforeRefineEdge functionAdjust;

	// threshold for pruning after refinement
	double minimumRefineEdgeIntensity;

	// timing for profiler
	MovingAverage milliAdjustBias = new MovingAverage(0.8);

	/**
	 * Configures the polygon detector
	 *
	 * @param detector Fits a polygon to a contour
	 * @param refineContour Refines the polygon produce a better fit against the contour
	 * @param refineGray Refine the edges to the input gray scale image
	 * @param minimumRefineEdgeIntensity Threshold for pruning shapes. Must have this edge intensity. Try 6
	 * @param adjustForThresholdBias Should it adjust contour polygons for the bias caused by thresholding?
	 */
	public DetectPolygonBinaryGrayRefine(DetectPolygonFromContour<T> detector,
										 RefinePolygonToContour refineContour,
										 RefinePolygonToGray<T> refineGray ,
										 double minimumRefineEdgeIntensity ,
										 boolean adjustForThresholdBias ) {
		this.detector = detector;
		this.refineContour = refineContour;
		this.refineGray = refineGray;
		this.minimumRefineEdgeIntensity = minimumRefineEdgeIntensity;
		if( adjustForThresholdBias ) {
			this.adjustForBias = new AdjustPolygonForThresholdBias();
		}

		this.edgeIntensity = new EdgeIntensityPolygon<>(1, 1.5, 15,
				detector.getInputType());
	}

	/**
	 * Specify a helper used to inject specialized code into the polygon detector
	 */
	public void setHelper( PolygonHelper helper ) {
		detector.setHelper(helper);
	}

	/**
	 * Turn on and off verbose output to standard out
	 */
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
		if( refineGray != null )
			refineGray.setLensDistortion(width, height, distToUndist, undistToDist);
		edgeIntensity.setTransform(undistToDist);
	}

	/**
	 * Discard previously set lens distortion models
	 */
	public void clearLensDistortion() {
		detector.clearLensDistortion();
		if( refineGray != null )
			refineGray.clearLensDistortion();
		edgeIntensity.setTransform(null);
	}

	public void resetRuntimeProfiling() {
		detector.resetRuntimeProfiling();
		milliAdjustBias.reset();
	}

	/**
	 * Detects polygons inside the grayscale image and its thresholded version
	 * @param gray Gray scale image
	 * @param binary Binary version of grayscale image
	 */
	public void process(T gray , GrayU8 binary ) {
		detector.process(gray,binary);
		if( refineGray != null )
			refineGray.setImage(gray);
		edgeIntensity.setImage(gray);

		long time0 = System.nanoTime();
		FastQueue<DetectPolygonFromContour.Info> detections = detector.getFound();

		if( adjustForBias != null ) {
			int minSides = getMinimumSides();
			for (int i = detections.size()-1; i >= 0; i-- ) {
				Polygon2D_F64 p = detections.get(i).polygon;
				adjustForBias.process(p, detector.isOutputClockwise());

				// When the polygon is adjusted for bias a point might need to be removed because it's
				// almost parallel. This could cause the shape to have too few corners and needs to be removed.
				if( p.size() < minSides)
					detections.remove(i);
			}
		}
		long time1 = System.nanoTime();

		double milli = (time1-time0)*1e-6;

		milliAdjustBias.update(milli);
//		System.out.printf(" contour %7.2f shapes %7.2f adjust_bias %7.2f\n",
//				detector.getMilliShapes(),detector.getMilliShapes(),milliAdjustBias);
	}

	/**
	 * Refines the fit to the specified polygon. Only info.polygon is modified
	 * @param info The polygon and related info
	 * @return true if successful or false if not
	 */
	public boolean refine( DetectPolygonFromContour.Info info ) {
		double before,after;
		if( edgeIntensity.computeEdge(info.polygon,!detector.isOutputClockwise()) ) {
			before = edgeIntensity.getAverageOutside() - edgeIntensity.getAverageInside();
		} else {
			return false;
		}

		boolean success = false;

		if( refineContour != null ) {
			List<Point2D_I32> contour = detector.getContour(info);
			refineContour.process(contour,info.splits,work);

			if( adjustForBias != null )
				adjustForBias.process(work, detector.isOutputClockwise());

			if( edgeIntensity.computeEdge(work,!detector.isOutputClockwise()) ) {
				after = edgeIntensity.getAverageOutside() - edgeIntensity.getAverageInside();
				if( after > before ) {
					info.edgeInside = edgeIntensity.getAverageInside();
					info.edgeOutside = edgeIntensity.getAverageOutside();
					info.polygon.set(work);
					success = true;
					before = after;
				}
			}
		}

		if( functionAdjust != null ) {
			functionAdjust.adjust(info, detector.isOutputClockwise());
		}

		if( refineGray != null ) {
			work.vertexes.resize(info.polygon.size());
			if( refineGray.refine(info.polygon,work) ) {
				if( edgeIntensity.computeEdge(work,!detector.isOutputClockwise()) ) {
					after = edgeIntensity.getAverageOutside() - edgeIntensity.getAverageInside();

					// basically, unless it diverged stick with this optimization
					// a near tie
					if( after*1.5 > before ) {
						info.edgeInside = edgeIntensity.getAverageInside();
						info.edgeOutside = edgeIntensity.getAverageOutside();
						info.polygon.set(work);
						success = true;
					}
				}
			}
		}

		return success;
	}

	/**
	 * Refines all the detected polygons and places them into the provided list. Polygons which fail the refinement
	 * step are not added.
	 */
	public void refineAll() {
		List<DetectPolygonFromContour.Info> detections = detector.getFound().toList();

		for (int i = 0; i < detections.size(); i++) {
			refine(detections.get(i));
		}
	}

	/**
	 * Returns a list of all polygons with an edge threshold above the minimum
	 *
	 *
	 * @param storageInfo Optional storage for info associated with polygons. Pruning is done so the info list
	 *                    and the returned polygon list are not in synch with each other
	 */
	public List<Polygon2D_F64> getPolygons( @Nullable List<Polygon2D_F64> storage ,
											@Nullable List<DetectPolygonFromContour.Info> storageInfo ) {
		if( storage == null )
			storage = new ArrayList<>();
		else
			storage.clear();

		if( storageInfo != null )
			storageInfo.clear();

		List<DetectPolygonFromContour.Info> detections = detector.getFound().toList();
		for (int i = 0; i < detections.size(); i++) {
			DetectPolygonFromContour.Info d = detections.get(i);

			if( d.computeEdgeIntensity() >= minimumRefineEdgeIntensity ) {
				storage.add(d.polygon);
				if( storageInfo != null ) {
					storageInfo.add(d);
				}
			}
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

	public List<ContourPacked> getAllContours() {
		return detector.getAllContours();
	}

	public void setFunctionAdjust(AdjustBeforeRefineEdge functionAdjust) {
		this.functionAdjust = functionAdjust;
	}

	public double getMilliAdjustBias() {
		return milliAdjustBias.getAverage();
	}

	public interface AdjustBeforeRefineEdge {
		void adjust( DetectPolygonFromContour.Info info , boolean clockwise );
	}
}

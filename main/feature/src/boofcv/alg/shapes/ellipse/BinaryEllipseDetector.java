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

package boofcv.alg.shapes.ellipse;

import boofcv.alg.filter.binary.Contour;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Detects ellipses inside gray scale images.  The first step is to detect them in a binary image and then
 * refine the fit using the gray scale image.  Optionally the refinement step can be turned off or postponed
 * until the user invokes it directly.  False positives are pruned using the edge intensity check.  This check removes
 * ellipses with edges that are low intensity sicne they are most likely generated from noise.
 *
 * @author Peter Abeles
 */
public class BinaryEllipseDetector<T extends ImageGray> {

	BinaryEllipseDetectorPixel ellipseDetector;
	SnapToEllipseEdge<T> ellipseRefiner;
	EdgeIntensityEllipse<T> intensityCheck;

	// storage for the output refined ellipses
	FastQueue<EllipseRotated_F64> refined = new FastQueue<>(EllipseRotated_F64.class, true);

	Class<T> inputType;

	boolean verbose = false;

	// toggled the refinement step.  If false an ellise can still be refined after the fact
	boolean autoRefine = true;

	/**
	 * Configures the detector
	 *
	 * @param ellipseDetector Detector which uses pixel precise edges
	 * @param ellipseRefiner Sub pixel edge refinement.  If null the refinement step is skipped
	 * @param intensityCheck Computes the intensity of the edge to remove false positives
	 * @param inputType Input image type
	 */
	public BinaryEllipseDetector(BinaryEllipseDetectorPixel ellipseDetector,
								 SnapToEllipseEdge<T> ellipseRefiner,
								 EdgeIntensityEllipse<T> intensityCheck ,
								 Class<T> inputType ) {
		this.ellipseDetector = ellipseDetector;
		this.ellipseRefiner = ellipseRefiner;
		this.intensityCheck = intensityCheck;
		this.inputType = inputType;
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted.
	 * The undistorted image is never explicitly created.</p>
	 *
	 * <p>
	 * WARNING: The undistorted image must have the same bounds as the distorted input image.  This is because
	 * several of the bounds checks use the image shape.  This are simplified greatly by this assumption.
	 * </p>
	 *
	 * @param distToUndist Transform from distorted to undistorted image.
	 * @param undistToDist Transform from undistorted to distorted image.
	 */
	public void setLensDistortion(PixelTransform2_F32 distToUndist , PixelTransform2_F32 undistToDist ) {
		this.ellipseDetector.setLensDistortion(distToUndist);
		if( this.ellipseRefiner != null )
			this.ellipseRefiner.setTransform(undistToDist);
		this.intensityCheck.setTransform(undistToDist);
	}

	/**
	 * Detects ellipses inside the binary image and refines the edges for all detections inside the gray image
	 *
	 * @param gray Grayscale image
	 * @param binary Binary image of grayscale. 1 = ellipse and 0 = ignored background
	 */
	public void process(T gray, GrayU8 binary) {
		refined.reset();

		ellipseDetector.process(binary);
		if( ellipseRefiner != null)
			ellipseRefiner.setImage(gray);
		intensityCheck.setImage(gray);

		List<BinaryEllipseDetectorPixel.Found> found = ellipseDetector.getFound();

		for( BinaryEllipseDetectorPixel.Found f : found ) {

			if( !intensityCheck.process(f.ellipse) ) {
				if( verbose )
					System.out.println("Rejecting ellipse which isn't intense enough");

				continue;
			}

			EllipseRotated_F64 r = refined.grow();

			if( ellipseRefiner != null ) {
				if (!ellipseRefiner.process(f.ellipse, r)) {
					refined.removeTail();
				}
			} else {
				r.set(f.ellipse);
			}
		}
	}

	/**
	 * If auto refine is turned off an ellipse can be refined after the fact using this function, provided
	 * that the refinement algorithm was passed in to the constructor
	 * @param ellipse The ellipse to be refined
	 * @return true if refine was successful or false if not
	 */
	public boolean refine( EllipseRotated_F64 ellipse ) {
		if( autoRefine )
			throw new IllegalArgumentException("Autorefine is true, no need to refine again");
		if( ellipseRefiner == null )
			throw new IllegalArgumentException("Refiner has not been passed in");
		if (!ellipseRefiner.process(ellipse,ellipse)) {
			return false;
		} else {
			return true;
		}
	}

	public BinaryEllipseDetectorPixel getEllipseDetector() {
		return ellipseDetector;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isAutoRefine() {
		return autoRefine;
	}

	public void setAutoRefine(boolean autoRefine) {
		this.autoRefine = autoRefine;
	}

	public Class<T> getInputType() {
		return inputType;
	}

	public List<Contour> getAllContours() {
		return ellipseDetector.getContourFinder().getContours().toList();
	}

	/**
	 * <p>Returns all the found ellipses in the input image.</p>
	 *
	 * WARNING: Returned data is recycled on the next call to process
	 *
	 * @return List of found ellipses.
	 */
	public FastQueue<EllipseRotated_F64> getFoundEllipses() {
		return refined;
	}
}

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

import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * TODO write
 *
 * @author Peter Abeles
 */
// TODO reject ellipses with weak edges
public class BinaryEllipseDetector<T extends ImageGray> {

	BinaryEllipseDetectorPixel ellipseDetector;
	SnapToEllipseEdge<T> ellipseRefiner;
	EdgeIntensityEllipse<T> intensityCheck;

	FastQueue<EllipseRotated_F64> refined = new FastQueue<EllipseRotated_F64>(EllipseRotated_F64.class,true);

	Class<T> inputType;

	boolean verbose;

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
	 */
	public void setLensDistortion( PixelTransform_F32 distToUndist ) {
		this.ellipseDetector.setLensDistortion(distToUndist);
		if( this.ellipseRefiner != null )
			this.ellipseRefiner.setTransform(distToUndist);
		this.intensityCheck.setTransform(distToUndist);
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

	public Class<T> getInputType() {
		return inputType;
	}

	/**
	 * Returns all the found ellipses in the input image.
	 *
	 * WARNING: Returned data is recycled on the next call to process
	 *
	 * @return List of found ellipses.
	 */
	public FastQueue<EllipseRotated_F64> getFoundEllipses() {
		return refined;
	}
}

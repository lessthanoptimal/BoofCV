/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;

/**
 * Computes a score for amount of coverage across the image border. The image border is particularly important for
 * accurate calibration as that's where lens distortion is most pronounced in standard lens models.
 */
public class ScoreCalibrationBorderFill {
	/**
	 * How close to the edge a point needs is to be considered along the image border. If relative, then it's
	 * relative to the average side length
	 */
	@Getter public final ConfigLength borderSpace = ConfigLength.relative(0.01, 10);

	/** How close a calibration point needs to be to a target point for it to be considered a hit */
	@Getter public final ConfigLength touchTol = ConfigLength.relative(0.02, 10);

	/** The fill score. 0 = not filled. 1.0 = perfect */
	@Getter double score;

	/** Number of points along the image edge which must be sampled */
	@Getter @Setter public int innerSamples = 5;

	/** Points which must be observed */
	final DogArray<Point2D_I32> targets = new DogArray<>(Point2D_I32::new, Point2D_I32::zero);

	/** The actual threshold relative to image sized used to decide if an observation touch a target */
	public double actualTouchTol;

	/** actual distance from border of control points */
	public double actualBorderTol;

	/**
	 * Resets and initializes for an image of the specified shape
	 */
	public void initialize( int width, int height ) {
		score = 0.0;

		// Compute touch tolerance given image shape
		actualTouchTol = borderSpace.compute((width + height)/2.0);

		// offset from image border that target points should be
		int o = borderSpace.computeI((int)((width + height)/2));
		actualBorderTol = o;

		// Add the corners
		targets.reset();
		targets.grow().setTo(o, o);
		targets.grow().setTo(width - o - 1, o);
		targets.grow().setTo(width - o - 1, height - o - 1);
		targets.grow().setTo(o, height - o - 1);

		// Add the inner points along the border.
		// Spacing between points will vary along each axis. Probably would be better to have the same amount
		// of spacing for both.
		for (int i = 0; i < innerSamples; i++) {
			// Compute location of sample point. Points are offset from 'o' + space
			int x = o + (width - o*2)*(i + 1)/(innerSamples + 1);
			int y = o + (height - o*2)*(i + 1)/(innerSamples + 1);

			targets.grow().setTo(x, o);
			targets.grow().setTo(x, height - o - 1);
			targets.grow().setTo(o, y);
			targets.grow().setTo(width - o - 1, y);
		}
	}

	/**
	 * See if any observed calibration points hit a target. if so remove the target.
	 */
	public void add( CalibrationObservation obs ) {
		BoofMiscOps.checkTrue(obs.width != 0 && obs.height != 0, "Must specify width and height");

		// slightly faster if you use distance squared
		double tol = actualTouchTol*actualTouchTol;

		for (int obsIdx = 0; obsIdx < obs.size(); obsIdx++) {
			Point2D_F64 o = obs.get(obsIdx).p;

			// Only consider observations near the image border
			if (!isNearBorder(o.x, o.y, obs.width, obs.height))
				continue;

			// Check to see if there's a match
			// Brute for it. There are not many points
			for (int targetIdx = 0; targetIdx < targets.size; targetIdx++) {
				Point2D_I32 t = targets.get(targetIdx);
				// See if it's a hit
				if (o.distance2(t.x, t.y) <= tol) {
					targets.removeSwap(targetIdx);
					break;
				}
			}
		}

		// Compute the score from number of target points remaining
		score = 1.0 - (targets.size/(double)(4 + 4*innerSamples));
	}

	/**
	 * True if the point is within tolerance of the border
	 */
	boolean isNearBorder( double x, double y, int width, int height ) {
		if (Math.abs(x - actualBorderTol) > actualBorderTol && Math.abs(width - x - actualBorderTol - 1) > actualBorderTol)
			return false;
		if (Math.abs(y - actualBorderTol) > actualBorderTol && Math.abs(height - y - actualBorderTol - 1) > actualBorderTol)
			return false;
		return true;
	}
}

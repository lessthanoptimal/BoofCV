/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.sfm.ScaleTranslate2D;
import org.ddogleg.fitting.modelset.ModelGenerator;

import java.util.List;

/**
 * Estimates a {@link boofcv.struct.sfm.ScaleTranslate2D} from two 2D point correspondences.  The transform will take a point from
 * p1 to p2.  The algorithm works by finding the centroid of p1 and p2.
 * Scale is found by finding the average change in vector length between p1 and p2 and the centroids. Translation
 * is found by the translation between the two centroids, adjusted for change in scale.
 *
 * If more than two points are provided the extra points are ignored.
 *
 * @author Peter Abeles
 */
public class GenerateScaleTranslate2D
		implements ModelGenerator<ScaleTranslate2D,AssociatedPair>
{
	// centroid in p1 and p2
	double centerX1,centerY1;
	double centerX2,centerY2;

	@Override
	public boolean generate(List<AssociatedPair> dataSet, ScaleTranslate2D output) {
		AssociatedPair a = dataSet.get(0);
		AssociatedPair b = dataSet.get(1);

		centerX1 = (a.p1.x + b.p1.x)/2.0;
		centerY1 = (a.p1.y + b.p1.y)/2.0;
		centerX2 = (a.p2.x + b.p2.x)/2.0;
		centerY2 = (a.p2.y + b.p2.y)/2.0;

		double dx = a.p1.x-centerX1;
		double dy = a.p1.y-centerY1;

		double r1 = Math.sqrt(dx*dx + dy*dy);

		if( r1 == 0 )
			return false;

		dx = a.p2.x-centerX2;
		dy = a.p2.y-centerY2;
		double r2 = Math.sqrt(dx*dx + dy*dy);

		double scale1 = r2/r1;

		dx = b.p1.x-centerX1;
		dy = b.p1.y-centerY1;
		r1 = Math.sqrt(dx*dx + dy*dy);
		if( r1 == 0 )
			return false;

		dx = b.p2.x-centerX2;
		dy = b.p2.y-centerY2;
		r2 = Math.sqrt(dx*dx + dy*dy);

		double scale2 = r2/r1;

		output.scale = (scale1+scale2)/2.0;

		output.transX =  centerX2 - centerX1*output.scale;
		output.transY =  centerY2 - centerY1*output.scale;

		return true;
	}


	@Override
	public int getMinimumPoints() {
		return 2;
	}
}

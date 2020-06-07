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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;

/**
 * Provides a wrapper around a fast corner detector for {@link PointDetector} no non-maximum suppression will be done
 *
 * @author Peter Abeles
 */
public class FastToInterestPoint<T extends ImageGray<T>>
	implements InterestPointDetector<T>
{
	@Getter FastCornerDetector<T> detector;
	// Storage for all the found corners
	private FastQueue<Point2D_F64> found = new FastQueue<>(Point2D_F64::new);
	// total number of low corners. Used to identify which set a feature belongs in
	private int totalLow;

	public FastToInterestPoint(FastCornerDetector<T> detector ) {
		this.detector = detector;
	}

	@Override
	public void detect(T input) {
		detector.process(input);
		QueueCorner low = detector.getCandidatesLow();
		QueueCorner high = detector.getCandidatesHigh();
		totalLow = low.size;
		found.resize(totalLow+high.size);
		for (int i = 0; i < totalLow; i++) {
			Point2D_I16 c = low.get(i);
			found.get(i).set(c.x,c.y);
		}
		for (int i = 0; i < high.size; i++) {
			Point2D_I16 c = high.get(i);
			found.get(i+totalLow).set(c.x,c.y);
		}
	}

	@Override public int getSet(int index) { return index < totalLow ? 0 : 1; }

	@Override public boolean hasScale() { return false; }

	@Override public boolean hasOrientation() { return false; }

	@Override public ImageType<T> getInputType() { return ImageType.single(detector.getImageType()); }

	@Override public int getNumberOfSets() { return 2; }

	@Override public int getNumberOfFeatures() { return found.size; }

	@Override public Point2D_F64 getLocation(int featureIndex) { return found.get(featureIndex); }

	@Override public double getRadius(int featureIndex) { return detector.getRadius(); }

	@Override public double getOrientation(int featureIndex) { return 0; }
}

/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.selector.FeatureSelectLimit;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastArray;

/**
 * Provides a wrapper around a fast corner detector for {@link InterestPointDetector} no non-maximum suppression will be done
 *
 * @author Peter Abeles
 */
public class FastToInterestPoint<T extends ImageGray<T>>
		implements InterestPointDetector<T> {
	@Getter FastCornerDetector<T> detector;
	// Storage for all the found corners
	private final DogArray<Point2D_F64> found = new DogArray<>(Point2D_F64::new);
	// total number of low corners. Used to identify which set a feature belongs in
	private int totalLow;

	// selects the features with the largest intensity
	protected @Getter FeatureSelectLimit<Point2D_I16> selectLimit;
	protected FastArray<Point2D_I16> selected = new FastArray<>(Point2D_I16.class);
	/** Maximum number of features it can select per set */
	public @Getter @Setter int featureLimitPerSet = Integer.MAX_VALUE;

	// temporary storage for corner candidates
	QueueCorner candidates = new QueueCorner();

	public FastToInterestPoint( FastCornerDetector<T> detector, FeatureSelectLimit<Point2D_I16> selectLimit ) {
		this.detector = detector;
		this.selectLimit = selectLimit;
	}

	@Override
	public void detect( T input ) {
		detector.process(input);

		// get all the low candidates
		candidates.reset();
		detector.getCandidatesLow().copyInto(candidates);
		// If there are too many select a subset based on this rule
		selectLimit.select(input.width, input.height, null, candidates, featureLimitPerSet, selected);
		// Note how many low corners there are so that the set is known in the future
		totalLow = selected.size;
		// copy the results
		found.resize(totalLow);
		for (int i = 0; i < selected.size; i++) {
			Point2D_I16 c = selected.get(i);
			found.get(i).setTo(c.x, c.y);
		}

		// Do the same for high corners
		candidates.reset();
		detector.getCandidatesHigh().copyInto(candidates);
		selectLimit.select(input.width, input.height, null, candidates, featureLimitPerSet, selected);
		// predeclare and reshape the array
		found.reserve(found.size + selected.size);
		found.size = found.size + selected.size;
		// copy the new elements into the found array
		for (int i = 0; i < selected.size; i++) {
			Point2D_I16 c = selected.get(i);
			found.get(i + totalLow).setTo(c.x, c.y);
		}
	}

	@Override public int getSet( int index ) {return index < totalLow ? 0 : 1;}

	@Override public boolean hasScale() {return false;}

	@Override public boolean hasOrientation() {return false;}

	@Override public ImageType<T> getInputType() {return ImageType.single(detector.getImageType());}

	@Override public int getNumberOfSets() {return 2;}

	@Override public int getNumberOfFeatures() {return found.size;}

	@Override public Point2D_F64 getLocation( int featureIndex ) {return found.get(featureIndex);}

	@Override public double getRadius( int featureIndex ) {return 1.0;}

	@Override public double getOrientation( int featureIndex ) {return 0;}
}

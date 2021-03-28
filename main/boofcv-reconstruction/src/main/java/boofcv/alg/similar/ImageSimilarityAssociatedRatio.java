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

package boofcv.alg.similar;

import boofcv.struct.ConfigLength;
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastAccess;

/**
 * Very simple similarity test that looks at the ratio of total features in each image to the number of matched
 * features. This assumes the chosen association approach will prune miss matched features and not just assign
 * associations no mater what.
 *
 * @author Peter Abeles
 */
public class ImageSimilarityAssociatedRatio implements SimilarImagesSceneRecognition.SimilarityTest {

	/** Fraction of features in a single image which must be associated for them to be considered similar */
	public final ConfigLength minimum = ConfigLength.relative(0.4, 0);

	public ImageSimilarityAssociatedRatio( ConfigLength minimumRatio ) {
		this.minimum.setTo(minimumRatio);
	}

	public ImageSimilarityAssociatedRatio() {}

	@Override
	public boolean isSimilar( FastAccess<Point2D_F64> srcPixels,
							  FastAccess<Point2D_F64> dstPixels,
							  FastAccess<AssociatedIndex> matches ) {
		if (minimum.length > 0 && matches.size < minimum.length)
			return false;
		if (minimum.fraction < 0.0)
			return true;
		if (matches.size < minimum.fraction*srcPixels.size)
			return false;
		if (matches.size < minimum.fraction*dstPixels.size)
			return false;
		return true;
	}
}

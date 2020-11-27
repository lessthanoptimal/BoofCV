/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe.llah;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a document or marker which is described using {@link LlahFeature LLAH features}.
 *
 * @author Peter Abeles
 */
public class LlahDocument {

	/**
	 * Which document this belongs to. Index in the document list.
	 */
	public int documentID;

	/**
	 * 2D locations of landmarks in the document's plane
	 */
	public DogArray<Point2D_F64> landmarks = new DogArray<>(Point2D_F64::new);
	/**
	 * Description of LLAH features in the document
	 */
	public List<LlahFeature> features = new ArrayList<>();

	public void addFeature( double x, double y ) {
		landmarks.grow().setTo(x, y);
	}

	public void reset() {
		documentID = -1;
		landmarks.reset();
		features.clear();
	}
}

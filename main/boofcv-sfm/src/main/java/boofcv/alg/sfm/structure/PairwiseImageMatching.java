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

package boofcv.alg.sfm.structure;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;

/**
 * Exhaustively associates all possible pairs of images.
 *
 * @author Peter Abeles
 */
public class PairwiseImageMatching<T extends ImageBase<T>>
{
	DetectDescribePoint<T,TupleDesc> detDesc;
	ScoreAssociation<TupleDesc> scorer;
	AssociateDescription<TupleDesc> associate;

	public void addImage( T image ) {

	}

	public PairwiseImageGraph constructGraph() {
		PairwiseImageGraph graph = new PairwiseImageGraph();
		return null;
	}
}

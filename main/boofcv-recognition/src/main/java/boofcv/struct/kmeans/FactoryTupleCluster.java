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

package boofcv.struct.kmeans;

import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.clustering.ConfigKMeans;
import org.ddogleg.clustering.FactoryClustering;
import org.ddogleg.clustering.kmeans.StandardKMeans;

/**
 * @author Peter Abeles
 */
public class FactoryTupleCluster {
	public static StandardKMeans<TupleDesc_F64> kmeans( int dof ) {
		var dconfig = new ConfigKMeans();
		dconfig.maxConverge = 1000;
		dconfig.maxIterations = 1000;
		dconfig.convergeTol = 1e-8;

		return FactoryClustering.kMeans(dconfig,
				new ComputeMeanTuple_F64(),
				new TuplePointDistanceEuclideanSq.F64(),
				()->new TupleDesc_F64(dof));
	}
}

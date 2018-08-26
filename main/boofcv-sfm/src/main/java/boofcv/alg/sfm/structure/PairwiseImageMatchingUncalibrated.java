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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

/**
 * @author Peter Abeles
 */
public class PairwiseImageMatchingUncalibrated<T extends ImageBase<T>> extends PairwiseImageMatching<T>
{
	private ConfigFundamental configFundamental = new ConfigFundamental();
	Ransac<DMatrixRMaj,AssociatedPair> ransacFundamental;

	public PairwiseImageMatchingUncalibrated(DetectDescribePoint<T, TupleDesc> detDesc) {
		super(detDesc);
	}

	protected PairwiseImageMatchingUncalibrated(){
	}

	{
		ransacFundamental = FactoryMultiViewRobust.fundamentalRansac(configFundamental, configRansac);
	}

	protected void connectViews(PairwiseImageGraph.CameraView viewA , PairwiseImageGraph.CameraView viewB ,
								FastQueue<AssociatedIndex> matches)
	{
		PairwiseImageGraph.CameraMotion edge = new PairwiseImageGraph.CameraMotion();

		if( !fitEpipolar(matches,
				viewA.observationPixels.toList(), viewB.observationPixels.toList(),
				ransacFundamental,edge) )
		{
			return;
		}

		int inliersEpipolar = ransacFundamental.getMatchSet().size();

		if( inliersEpipolar < MIN_FEATURE_ASSOCIATED )
			return;

		// If only a very small number of features are associated do not consider the view
		double fractionA = inliersEpipolar/(double)viewA.descriptions.size;
		double fractionB = inliersEpipolar/(double)viewB.descriptions.size;

		if( fractionA < MIN_ASSOCIATE_FRACTION | fractionB < MIN_ASSOCIATE_FRACTION )
			return;

		// If the geometry is good for triangulation this number will be lower
		edge.viewSrc = viewA;
		edge.viewDst = viewB;
		viewA.connections.add(edge);
		viewB.connections.add(edge);
		graph.edges.add(edge);

		if( verbose != null )
			verbose.println("  Connected "+viewA.index+" -> "+viewB.index);
	}
}

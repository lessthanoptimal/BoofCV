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

import boofcv.alg.geo.pose.PositionFromPairLinear2;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the translation component given the rotation and at least two point observations
 * 
 * @author Peter Abeles
 */
public class TranGivenRotGenerator implements ModelGenerator<Vector3D_F64,Point2D3D>
{
	PositionFromPairLinear2 alg = new PositionFromPairLinear2();
	
	// rotation matrix
	DenseMatrix64F R;

	// storage
	List<Point3D_F64> worldPts = new ArrayList<Point3D_F64>();
	List<Point2D_F64 > observed = new ArrayList<Point2D_F64>();

	public void setRotation(DenseMatrix64F r) {
		R = r;
	}

	@Override
	public Vector3D_F64 createModelInstance() {
		return new Vector3D_F64();
	}

	@Override
	public boolean generate(List<Point2D3D> dataSet, Vector3D_F64 model ) {
		worldPts.clear();
		observed.clear();
		
		for( int i = 0; i < dataSet.size(); i++ ) {
			Point2D3D p = dataSet.get(i);
			worldPts.add( p.location );
			observed.add( p.observation);
		}
		
		if( alg.process(R,worldPts,observed) ) {
			model.set(alg.getT());
		}

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 2;
	}

}

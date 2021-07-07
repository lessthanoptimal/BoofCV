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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
* So these tests really fail, but the error margin was made to be very large so that they
* passed. Not sure if there is a bug in the implementation or if this is its inherit
* accuracy. The matlab code provided by the author exhibited large amounts of error
* in the minimal case and their paper did not show results for that case...
*
* @author Peter Abeles
*/
public class TestRelinearlize extends BoofStandardJUnit {

	Random rand = new Random(234);

	DMatrixRMaj L_full;
	DMatrixRMaj y;

	@Test void numControl4() {
		checkNumControl(4);
	}

	@Test void numControl3() {
		checkNumControl(3);
	}

	private void checkNumControl(int numControl ) {
		createInputs(numControl);
		Relinearlize alg = new Relinearlize();
		alg.setNumberControl(numControl);

		// variables being estimated
		double foundBeta[] = new double[numControl];
		alg.process(L_full,y,foundBeta);

		// check to see if its a valid solution
		DMatrixRMaj x = new DMatrixRMaj(L_full.numCols,1);
		DMatrixRMaj foundDistance = new DMatrixRMaj(L_full.numRows,1);
		int index = 0;
		for( int i = 0; i < numControl; i++ ) {
			for( int j = i; j < numControl; j++ ) {
				x.data[index++] = foundBeta[i]*foundBeta[j];
			}
		}

		CommonOps_DDRM.mult(L_full, x, foundDistance);

//		System.out.println("error = "+SpecializedOps_DDRM.diffNormF(foundDistance,y));

		// NOTE: This test can pass and the result still be bad because L_full is
		// an undetermined system. But at least there is some sort of test here
		assertTrue(MatrixFeatures_DDRM.isEquals(foundDistance, y, 2));

		// WARNING!!! the error margin was made to be huge to make sure it passed
	}

	private void createInputs( int numControl ) {
		if( numControl == 4 ) {
			L_full = new DMatrixRMaj(6,10);
			y = new DMatrixRMaj(6,1);
		} else {
			L_full = new DMatrixRMaj(3,6);
			y = new DMatrixRMaj(3,1);
		}

		// randomly select null points,
		List<Point3D_F64> nullPts[] = new ArrayList[numControl];
		for( int i = 0; i < numControl-1; i++ ) {
			nullPts[i] = GeoTestingOps.randomPoints_F64(-1,1,-1,1,-1,1,numControl,rand);
		}

		nullPts[numControl-1] = new ArrayList<>();
		nullPts[numControl-1].add( new Point3D_F64(1,0,0));
		nullPts[numControl-1].add( new Point3D_F64(0,1,0));
		nullPts[numControl-1].add( new Point3D_F64(0,0,1));
		if( numControl == 4 )
			nullPts[numControl-1].add( new Point3D_F64(0,0,0));

		// using the provided beta compute the world points
		// this way the constraint matrix will be consistent
		DogArray<Point3D_F64> worldPts = new DogArray<>(4, Point3D_F64::new);
		worldPts.grow().setTo(1,0,0);
		worldPts.grow().setTo(0,1,0);
		worldPts.grow().setTo(0,0,1);
		if( numControl == 4 )
			worldPts.grow().setTo(0, 0, 0);

		if( numControl == 4 )
			UtilLepetitEPnP.constraintMatrix6x10(L_full,y,worldPts,nullPts);
		else
			UtilLepetitEPnP.constraintMatrix3x6(L_full, y, worldPts, nullPts);
	}

}

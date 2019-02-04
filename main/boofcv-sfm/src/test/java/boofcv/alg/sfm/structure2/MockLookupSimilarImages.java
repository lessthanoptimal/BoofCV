/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates dummy data for testing algorithms. All views see the same scene and are similar to each other
 *
 * @author Peter Abeles
 */
class MockLookupSimilarImages implements LookupSimilarImages{
	CameraPinhole intrinsic = new CameraPinhole(400,410,0,420,420,800,800);
	int numFeaturse = 100;

	Random rand;
	List<String> viewIds = new ArrayList<>();
	List<List<Point2D_F64>> viewFeats = new ArrayList<>();

	public MockLookupSimilarImages( int numViews , long seed) {
		this.rand = new Random(seed);

		for (int i = 0; i < numViews; i++) {
			viewIds.add("View "+i);
		}

		// 3D location of points in view 0 reference frame
		List<Point3D_F64> feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1), -0.5, 0.5, numFeaturse, rand);

		// render pixel coordinates of all points
		for (int i = 0; i < numViews; i++) {
			Se3_F64 view0_to_viewi = SpecialEuclideanOps_F64.eulerXyz(
					0.01 + 0.1*i,0,rand.nextGaussian()*0.03,
					rand.nextGaussian()*0.03,rand.nextGaussian()*0.03,rand.nextGaussian()*0.1,null);

			// first view is the origin
			if( i == 0 )
				view0_to_viewi.reset();

			List<Point2D_F64> feats2D = new ArrayList<>();
			viewFeats.add(feats2D);

			for (int featId = 0; featId < feats3D.size(); featId++) {
				Point3D_F64 X = feats3D.get(featId);
				feats2D.add(PerspectiveOps.renderPixel(view0_to_viewi,intrinsic,X));
			}
		}
	}

	@Override
	public List<String> getImageIDs() {
		return viewIds;
	}

	@Override
	public void findSimilar(String target, List<String> similar) {
		similar.clear();
		for (int i = 0; i < viewIds.size(); i++) {
			if( !viewIds.get(i).equals(target) ) {
				similar.add(viewIds.get(i));
			}
		}
	}

	@Override
	public void lookupFeatures(String target, FastQueue<Point2D_F64> features) {
		int index = viewIds.indexOf(target);
		List<Point2D_F64> l = viewFeats.get(index);
		features.reset();
		for (int i = 0; i < l.size(); i++) {
			features.grow().set(l.get(i));
		}
	}

	@Override
	public void lookupMatches(String src, String dst, FastQueue<AssociatedIndex> pairs)
	{
		pairs.reset();
		for (int i = 0; i < numFeaturse; i++) {
			pairs.grow().setAssociation(i,i,0);
		}
	}

	@Override
	public void lookupShape(String target, ImageDimension shape) {
		shape.set(intrinsic.width,intrinsic.height);
	}
}
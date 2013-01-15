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

package boofcv.alg.sfm.d3;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.detdesc.PointDescSet;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.feature.UtilFeature;
import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

/**
* @author Peter Abeles
*/
// TODO add a second pass option
// TODO add bundle adjustment
// TODO Show right camera tracks in debugger
public class VisOdomQuadPnP<T extends ImageSingleBand,TD extends TupleDesc> {

	TriangulateTwoViewsCalibrated triangulate;

	// computes camera motion
	private ModelMatcher<Se3_F64, Stereo2D3D> matcher;
	private ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;

	private FastQueue<Stereo2D3D> modelFitData = new FastQueue<Stereo2D3D>(10,Stereo2D3D.class,true);

	// Detects feature inside the image
	DetectDescribeMulti<T,TD> detector;
	// Associates feature between the same camera
	AssociateDescription2D<TD> assocSame;
	// Associates features from left to right camera
	AssociateDescription2D<TD> assocL2R;

	FastQueue<QuadView> quadViews = new FastQueue<QuadView>(10,QuadView.class,true);

	ImageInfo<TD> featsLeft0,featsLeft1;
	ImageInfo<TD> featsRight0,featsRight1;
	SetMatches setMatches[];

	// stereo baseline going from left to right
	Se3_F64 leftToRight = new Se3_F64();

	// convert for original image pixels into normalized image coordinates
	private PointTransform_F64 leftImageToNorm;
	private PointTransform_F64 rightImageToNorm;

	// transform from the current view to the old view (left camera)
	private Se3_F64 newToOld = new Se3_F64();
	// transform from the current camera view to the world frame
	private Se3_F64 leftCamToWorld = new Se3_F64();

	// number of frames that have been processed
	// is this the first frame
	private boolean first = true;

	public VisOdomQuadPnP(DetectDescribeMulti<T,TD> detector,
						  AssociateDescription2D<TD> assocSame , AssociateDescription2D<TD> assocL2R ,
						  TriangulateTwoViewsCalibrated triangulate,
						  ModelMatcher<Se3_F64, Stereo2D3D> matcher,
						  ModelFitter<Se3_F64, Stereo2D3D> modelRefiner )
	{
		this.detector = detector;
		this.assocSame = assocSame;
		this.assocL2R = assocL2R;
		this.triangulate = triangulate;
		this.matcher = matcher;
		this.modelRefiner = modelRefiner;

		setMatches = new SetMatches[ detector.getNumberOfSets() ];
		for( int i = 0; i < setMatches.length; i++ ) {
			setMatches[i] = new SetMatches();
		}

		featsLeft0 = new ImageInfo<TD>(detector);
		featsLeft1 = new ImageInfo<TD>(detector);
		featsRight0 = new ImageInfo<TD>(detector);
		featsRight1 = new ImageInfo<TD>(detector);
	}

	public void setCalibration(StereoParameters param) {

		param.rightToLeft.invert(leftToRight);
		leftImageToNorm = LensDistortionOps.transformRadialToNorm_F64(param.left);
		rightImageToNorm = LensDistortionOps.transformRadialToNorm_F64(param.right);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		featsLeft0.reset();
		featsLeft1.reset();
		featsRight0.reset();
		featsRight1.reset();
		for( SetMatches m : setMatches )
			m.reset();
		newToOld.reset();
		leftCamToWorld.reset();
		first = true;
	}

	public boolean process( T left , T right ) {

		if( first ) {
			associateL2R(left, right);
			first = false;
		} else {
			associateL2R(left, right);
			associateF2F();
			cyclicConsistency();

			if( !estimateMotion() )
				return false;
		}

		return true;
	}

	private void associateL2R( T left , T right ) {
		// make the previous new observations into the new old ones
		ImageInfo<TD> tmp = featsLeft1;
		featsLeft1 = featsLeft0; featsLeft0 = tmp;
		tmp = featsRight1;
		featsRight1 = featsRight0; featsRight0 = tmp;

		// detect and associate features in the two images
		featsLeft1.reset();
		featsRight1.reset();

		describeImage(left,featsLeft1);
		describeImage(right,featsRight1);

		for( int i = 0; i < detector.getNumberOfSets(); i++ ) {
			SetMatches matches = setMatches[i];
			matches.swap();
			matches.match2to3.reset();

			FastQueue<Point2D_F64> leftLoc = featsLeft1.location[i];
			FastQueue<Point2D_F64> rightLoc = featsRight1.location[i];

			assocL2R.setSource(leftLoc,featsLeft1.description[i]);
			assocL2R.setDestination(rightLoc, featsRight1.description[i]);
			assocL2R.associate();

			FastQueue<AssociatedIndex> found = assocL2R.getMatches();

			setMatches(matches.match2to3, found, leftLoc.size);
		}
	}

	private void associateF2F()
	{
		quadViews.reset();

		for( int i = 0; i < detector.getNumberOfSets(); i++ ) {
			SetMatches matches = setMatches[i];

			// old left to new left
			assocSame.setSource(featsLeft0.location[i],featsLeft0.description[i]);
			assocSame.setDestination(featsLeft1.location[i], featsLeft1.description[i]);
			assocSame.associate();

			setMatches(matches.match0to2, assocSame.getMatches(), featsLeft0.location[i].size);

			// old right to new right
			assocSame.setSource(featsRight0.location[i],featsRight0.description[i]);
			assocSame.setDestination(featsRight1.location[i], featsRight1.description[i]);
			assocSame.associate();

			setMatches(matches.match1to3, assocSame.getMatches(), featsRight0.location[i].size);
		}
	}

	/**
	 * Create a list of features which have a consistent cycle of matches
	 * 0 -> 1 -> 3 and 0 -> 2 -> 3
	 */
	private void cyclicConsistency() {
		for( int i = 0; i < detector.getNumberOfSets(); i++ ) {
			FastQueue<Point2D_F64> obs0 = featsLeft0.location[i];
			FastQueue<Point2D_F64> obs1 = featsRight0.location[i];
			FastQueue<Point2D_F64> obs2 = featsLeft1.location[i];
			FastQueue<Point2D_F64> obs3 = featsRight1.location[i];

			SetMatches matches = setMatches[i];

			if( matches.match0to1.size != matches.match0to2.size )
				throw new RuntimeException("Failed sanity check");

			for( int j = 0; j < matches.match0to1.size; j++ ) {
				int indexIn1 = matches.match0to1.data[j];
				int indexIn2 = matches.match0to2.data[j];

				if( indexIn1 < 0 || indexIn2 < 0 )
					continue;

				int indexIn3a = matches.match1to3.data[indexIn1];
				int indexIn3b = matches.match2to3.data[indexIn2];

				if( indexIn3a < 0 || indexIn3b < 0 )
					continue;

				// consistent association to new right camera image
				if( indexIn3a == indexIn3b ) {
					QuadView v = quadViews.grow();
					v.v0 = obs0.get(j);
					v.v1 = obs1.get(indexIn1);
					v.v2 = obs2.get(indexIn2);
					v.v3 = obs3.get(indexIn3a);
				}
			}
		}
	}

	private void setMatches(GrowQueue_I32 matches,
							FastQueue<AssociatedIndex> found,
							int sizeSrc ) {
		matches.resize(sizeSrc);
		for( int j = 0; j < sizeSrc; j++ ) {
			matches.data[j] = -1;
		}
		for( int j = 0; j < found.size; j++ ) {
			AssociatedIndex a = found.get(j);
			matches.data[a.src] = a.dst;
		}
	}


	private void describeImage(T left , ImageInfo<TD> info ) {
		detector.process(left);
		for( int i = 0; i < detector.getNumberOfSets(); i++ ) {
			PointDescSet<TD> set = detector.getFeatureSet(i);
			FastQueue<Point2D_F64> l = info.location[i];
			FastQueue<TD> d = info.description[i];

			for( int j = 0; j < set.getNumberOfFeatures(); j++ ) {
				l.grow().set( set.getLocation(j) );
				d.grow().setTo( set.getDescription(j) );
			}
		}
	}

	private boolean estimateMotion() {
		modelFitData.reset();

		Point2D_F64 normLeft = new Point2D_F64();
		Point2D_F64 normRight = new Point2D_F64();

		// use 0 -> 1 stereo associations to estimate each feature's 3D position
		for( int i = 0; i < quadViews.size; i++ ) {
			QuadView obs = quadViews.get(i);

			// convert old stereo view to normalized coordinates
			leftImageToNorm.compute(obs.v0.x,obs.v0.y,normLeft);
			rightImageToNorm.compute(obs.v1.x,obs.v1.y,normRight);

			// compute 3D location using triangulation
			triangulate.triangulate(normLeft,normRight,leftToRight,obs.X);

			// add to data set for fitting if not at infinity
			if( !Double.isInfinite(obs.X.normSq()) ) {
				Stereo2D3D data = modelFitData.grow();
				leftImageToNorm.compute(obs.v2.x,obs.v2.y,data.leftObs);
				rightImageToNorm.compute(obs.v3.x,obs.v3.y,data.rightObs);
				data.location.set(obs.X);
			}
		}

		// robustly match the data
		if( !matcher.process(modelFitData.toList()) )
			return false;

		Se3_F64 oldToNew = matcher.getModel();

		// optionally refine the results
		if( modelRefiner != null ) {
			Se3_F64 found = new Se3_F64();
			modelRefiner.fitModel(matcher.getMatchSet(), oldToNew, found);
			found.invert(newToOld);
		} else {
			oldToNew.invert(newToOld);
		}

		// compound the just found motion with the previously found motion
		Se3_F64 temp = new Se3_F64();
		newToOld.concat(leftCamToWorld, temp);
		leftCamToWorld.set(temp);

		return true;
	}

	public ModelMatcher<Se3_F64, Stereo2D3D> getMatcher() {
		return matcher;
	}

	public FastQueue<QuadView> getQuadViews() {
		return quadViews;
	}

	public Se3_F64 getLeftToWorld() {
		return leftCamToWorld;
	}

	public static class ImageInfo<TD extends TupleDesc>
	{
		FastQueue<Point2D_F64> location[];
		FastQueue<TD> description[];

		public ImageInfo( DetectDescribeMulti<?,TD> detector ) {
			location = new FastQueue[ detector.getNumberOfSets() ];
			description = new FastQueue[ detector.getNumberOfSets() ];

			for( int i = 0; i < location.length; i++ ) {
				location[i] = new FastQueue<Point2D_F64>(100,Point2D_F64.class,true);
				description[i] = UtilFeature.createQueue(detector,100);
			}
		}

		public void reset() {
			for( int i = 0; i < location.length; i++ ) {
				location[i].reset();
				description[i].reset();
			}
		}
	}

	public static class SetMatches {
		GrowQueue_I32 match0to1 = new GrowQueue_I32(10);
		GrowQueue_I32 match0to2 = new GrowQueue_I32(10);
		GrowQueue_I32 match2to3 = new GrowQueue_I32(10);
		GrowQueue_I32 match1to3 = new GrowQueue_I32(10);

		public void swap() {
			GrowQueue_I32 tmp;

			tmp = match2to3;
			match2to3 = match0to1;
			match0to1 = tmp;
		}

		public void reset() {
			match0to1.reset();
			match0to2.reset();
			match2to3.reset();
			match1to3.reset();
		}
	}

	public static class QuadView
	{
		// 3D coordinate in old camera view
		public Point3D_F64 X = new Point3D_F64();
		// pixel observation in each camera view
		public Point2D_F64 v0,v1,v2,v3;

	}
}

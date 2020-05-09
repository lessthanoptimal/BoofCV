/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Set;

/**
 * Stereo visual odometry algorithm which associates image features across two stereo pairs for a total of four images.
 * Image features are first matched between left and right images while applying epipolar constraints.  Then the two
 * more recent sets of stereo  images are associated with each other in a left to left and right to right fashion.
 * Features which are consistently matched across all four images are saved in a list.  RANSAC is then used to
 * remove false positives and estimate camera motion using a
 * {@link boofcv.abst.geo.Estimate1ofPnP PnP} type algorithm.
 *
 * Motion is estimated using PNP algorithms.  These require that each image feature as its 3D coordinate estimated.
 * After a feature is associated between a stereo pair its 3D location is also estimated using triangulation.  Iterative
 * refinement can then be applied after motion has been estimated.
 *
 * Inside the code each camera is some times referred to by number. 0 = left camera previous frame. 1 = right
 * camera previous frame. 2 = left camera current frame. 3 = right camera current frame.
 *
 * Estimated motion is relative to left camera.
 *
 * @author Peter Abeles
 */
public class VisOdomStereoQuadPnP<T extends ImageGray<T>,TD extends TupleDesc>
		implements VerbosePrint
{
	// TODO Add new logic for deciding what the key frame is
	// TODO See upgrades in Stereo Dual Tracker for associate and bundle
	// TODO Add feature sets back in after refactor


	// used to estimate each feature's 3D location using a stereo pair
	private final Triangulate2ViewsMetric triangulate;

	// computes camera motion
	private final @Getter ModelMatcher<Se3_F64, Stereo2D3D> matcher;
	private final ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;

	private final FastQueue<Stereo2D3D> modelFitData = new FastQueue<>(10, Stereo2D3D::new);

	private final BundleAdjustment<SceneStructureMetric> bundleAdjustment;

	// Detects feature inside the image
	private final DetectDescribePoint<T,TD> detector;
	// Associates feature between the same camera
	private final AssociateDescription<TD> assocF2F;
	// Associates features from left to right camera
	private final AssociateDescription2D<TD> assocL2R;

	// Set of associated features across all views
	private final @Getter FastQueue<QuadView> quadViews = new FastQueue<>(QuadView::new,QuadView::reset);

	// features info extracted from the stereo pairs. 0 = previous 1 = current
	private ImageInfo<TD> featsLeft0,featsLeft1;
	private ImageInfo<TD> featsRight0,featsRight1;
	// Matched features between all four images.  One set of matches for each type of detected feature
	private final SetMatches[] setMatches;

	// stereo baseline going from left to right
	private final Se3_F64 leftToRight = new Se3_F64();

	// convert for original image pixels into normalized image coordinates
	private Point2Transform2_F64 leftPixelToNorm;
	private Point2Transform2_F64 rightPixelToNorm;

	// transform from the current view to the old view (left camera)
	private final Se3_F64 new_to_old = new Se3_F64();
	// transform from the current camera view to the world frame
	private final Se3_F64 left_to_world = new Se3_F64();

	/** Unique ID for each frame processed */
	private @Getter long frameID = -1;

	// number of frames that have been processed
	// is this the first frame
	private boolean first = true;

	// Internal profiling
	protected @Getter @Setter PrintStream profileOut;
	// Verbose debug information
	protected @Getter PrintStream verbose;

	// Work space variables
	private final GrowQueue_I32 lookupTable = new GrowQueue_I32();
	private final Se3_F64 prevLeft_to_world = new Se3_F64();

	/**
	 * Specifies internal algorithms
	 *
	 * @param detector Estimates image features
	 * @param assocF2F Association algorithm used for left to left and right to right
	 * @param assocL2R Assocation algorithm used for left to right
	 * @param triangulate Used to estimate 3D location of a feature using stereo correspondence
	 * @param matcher Robust model estimation.  Often RANSAC
	 * @param modelRefiner Non-linear refinement of motion estimation
	 */
	public VisOdomStereoQuadPnP(DetectDescribePoint<T,TD> detector,
								AssociateDescription<TD> assocF2F,
								AssociateDescription2D<TD> assocL2R ,
								Triangulate2ViewsMetric triangulate,
								ModelMatcher<Se3_F64, Stereo2D3D> matcher,
								ModelFitter<Se3_F64, Stereo2D3D> modelRefiner,
								BundleAdjustment<SceneStructureMetric> bundleAdjustment )
	{
		this.detector = detector;
		this.assocF2F = assocF2F;
		this.assocL2R = assocL2R;
		this.triangulate = triangulate;
		this.matcher = matcher;
		this.modelRefiner = modelRefiner;
		this.bundleAdjustment = bundleAdjustment;

		setMatches = new SetMatches[ 1 ];
		for( int i = 0; i < setMatches.length; i++ ) {
			setMatches[i] = new SetMatches();
		}

		featsLeft0 = new ImageInfo<>(detector);
		featsLeft1 = new ImageInfo<>(detector);
		featsRight0 = new ImageInfo<>(detector);
		featsRight1 = new ImageInfo<>(detector);
	}

	public void setCalibration(StereoParameters param)
	{
		param.rightToLeft.invert(leftToRight);
		leftPixelToNorm = LensDistortionFactory.narrow(param.left).undistort_F64(true,false);
		rightPixelToNorm = LensDistortionFactory.narrow(param.right).undistort_F64(true,false);
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
		new_to_old.reset();
		left_to_world.reset();
		first = true;
		frameID = -1;
	}

	/**
	 * Estimates camera egomotion from the stereo pair
	 * @param left Image from left camera
	 * @param right Image from right camera
	 * @return true if motion was estimated and false if not
	 */
	public boolean process( T left , T right ) {
		frameID++;
		if( first ) {
			associateL2R(left, right);

			// for visualization purposes copy tracks into quad
			quadViews.reset();
			FastQueue<Point2D_F64> location = featsLeft1.location[0];
			for (int i = 0; i < location.size; i++) {
				QuadView q = quadViews.grow();
				q.v2 = location.get(i);
			}

			first = false;
		} else {
			long time0 = System.nanoTime();
			associateL2R(left, right);
			long time1 = System.nanoTime();
			associateF2F();
			long time2 = System.nanoTime();
			cyclicConsistency();
			long time3 = System.nanoTime();

			// TODO save pixel coordinates of remaining features

			// Estimate the motion approximately
			if ( !robustMotionEstimate())
				return false;
			Se3_F64 old_to_new = matcher.getModelParameters();

			// get a better estimate
			refineMotionEstimate(old_to_new);

			long time4 = System.nanoTime();

			// get the best estimate
			performBundleAdjustment(old_to_new);

			long time5 = System.nanoTime();

			// compound the just found motion with the previously found motion
			old_to_new.invert(new_to_old);
			prevLeft_to_world.set(left_to_world);
			new_to_old.concat(prevLeft_to_world, left_to_world);

			// TODO save 3D estimate from previous update and don't triangulate it each time

			// TODO decide if it should change the key frame

			if( profileOut != null ) {
				double milliL2R = (time1-time0)*1e-6;
				double milliF2F = (time2-time1)*1e-6;
				double milliCyc = (time3-time2)*1e-6;
				double milliEst = (time4-time3)*1e-6;
				double milliBun = (time5-time4)*1e-6;

				profileOut.printf("TIME: L2R %5.1f F2F %5.1f Cyc %5.1f Est %5.1f Bun %5.1f Total: %5.1f\n",
						milliL2R,milliF2F,milliCyc,milliEst,milliBun,(time5-time0)*1e-6);
			}
		}

		if( verbose != null ) {
			int leftDetections = featsLeft1.location[0].size;
			int inliers = matcher.getMatchSet().size();
			int matchesL2R = assocL2R.getMatches().size;
			verbose.printf("Viso: Det: %4d L2R: %4d, Quad: %4d Inliers: %d\n",
					leftDetections,matchesL2R,quadViews.size,inliers);
		}

		return true;
	}

	/**
	 * Associates image features from the left and right camera together while applying epipolar constraints.
	 *
	 * @param left Image from left camera
	 * @param right Image from right camera
	 */
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

		// detect and associate features in the current stereo pair
		for( int i = 0; i < 1; i++ ) {
			SetMatches matches = setMatches[i];
			matches.swap();
			matches.match2to3.reset();

			FastQueue<Point2D_F64> leftLoc = featsLeft1.location[i];
			FastQueue<Point2D_F64> rightLoc = featsRight1.location[i];

			assocL2R.setSource(leftLoc,featsLeft1.description[i]);
			assocL2R.setDestination(rightLoc, featsRight1.description[i]);
			assocL2R.associate();

			FastAccess<AssociatedIndex> found = assocL2R.getMatches();

			setMatches(matches.match2to3, found, leftLoc.size);
		}
	}

	/**
	 * Associates images between left and left and right and right images
	 */
	private void associateF2F()
	{
		for( int i = 0; i < 1; i++ ) {
			SetMatches matches = setMatches[i];

			// old left to new left
			assocF2F.setSource(featsLeft0.description[i]);
			assocF2F.setDestination(featsLeft1.description[i]);
			assocF2F.associate();

			setMatches(matches.match0to2, assocF2F.getMatches(), featsLeft0.location[i].size);

			// TODO why do we need to do this more than once?
			// old right to new right
			assocF2F.setSource(featsRight0.description[i]);
			assocF2F.setDestination(featsRight1.description[i]);
			assocF2F.associate();

			setMatches(matches.match1to3, assocF2F.getMatches(), featsRight0.location[i].size);
		}
	}

	/**
	 * Create a list of features which have a consistent cycle of matches
	 * 0 -> 1 -> 3 and 0 -> 2 -> 3
	 */
	private void cyclicConsistency() {
		quadViews.reset();
		for( int i = 0; i < 1; i++ ) {
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
							FastAccess<AssociatedIndex> found,
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

	/**
	 * Computes image features and stores the results in info
	 */
	private void describeImage(T image , ImageInfo<TD> info ) {
		detector.detect(image);
		FastQueue<Point2D_F64> l = info.location[0];
		FastQueue<TD> d = info.description[0];
		l.reset();
		d.reset();
		for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
			l.grow().set( detector.getLocation(i) );
			d.grow().setTo( detector.getDescription(i) );
		}
	}



	/**
	 * Robustly estimate the motion
	 */
	private boolean robustMotionEstimate() {
		modelFitData.reset();

		Point2D_F64 normLeft = new Point2D_F64();
		Point2D_F64 normRight = new Point2D_F64();

		// predeclare the table. this is used to go from fit data to quad
		lookupTable.resize(quadViews.size);
		lookupTable.reset();

		// use 0 -> 1 stereo associations to estimate each feature's 3D position
		for( int i = 0; i < quadViews.size; i++ ) {
			QuadView obs = quadViews.get(i);

			// convert old stereo view to normalized coordinates
			leftPixelToNorm.compute(obs.v0.x,obs.v0.y,normLeft);
			rightPixelToNorm.compute(obs.v1.x,obs.v1.y,normRight);

			// compute 3D location using triangulation
			boolean success = triangulate.triangulate(normLeft,normRight,leftToRight,obs.X);
			success &= !Double.isInfinite(obs.X.normSq());

			if( success ) {
				lookupTable.add(i);
				Stereo2D3D data = modelFitData.grow();
				leftPixelToNorm.compute(obs.v2.x,obs.v2.y,data.leftObs);
				rightPixelToNorm.compute(obs.v3.x,obs.v3.y,data.rightObs);
				data.location.set(obs.X);
			}
		}

		// robustly match the data
		if( !matcher.process(modelFitData.toList()) ) {
			return false;
		}

		// mark features which are inliers
		int numInliers = matcher.getMatchSet().size();
		for (int i = 0; i < numInliers; i++) {
			int quadIndex = lookupTable.get(matcher.getInputIndex(i));
			quadViews.get(quadIndex).inlier = true;
		}

		return true;
	}

	/**
	 * Non linear refinement of motion estimate
	 *
	 * @param old_to_new Initial estimate and refined on output
	 */
	private void refineMotionEstimate(Se3_F64 old_to_new ) {
		// optionally refine the results
		if( modelRefiner != null ) {
			Se3_F64 found = new Se3_F64();
			if( modelRefiner.fitModel(matcher.getMatchSet(), old_to_new, found) ) {
				old_to_new.set(found);
			}
		}

		// if disabled or it fails just use the robust estimate
	}

	/**
	 * Optimize cameras and feature locations at the same time
	 */
	private void performBundleAdjustment(Se3_F64 old_to_new) {
		if( bundleAdjustment == null )
			return;
	}

	private String toString( Se3_F64 motion ) {
		double euler[] = ConvertRotation3D_F64.matrixToEuler(motion.getR(), EulerType.XYZ,(double[])null);
		return String.format("%5e %5e %5e",euler[0],euler[1],euler[2]);
	}

	public Se3_F64 getLeftToWorld() {return left_to_world; }

	public Se3_F64 getNewToOld() { return new_to_old; }

	/**
	 * Storage for detected features inside an image
	 */
	public static class ImageInfo<TD>
	{
		FastQueue<Point2D_F64> location[];
		FastQueue<TD> description[];

		public ImageInfo( DetectDescribePoint detector ) {
			location = new FastQueue[ 1 ];
			description = new FastQueue[ 1];

			for( int i = 0; i < location.length; i++ ) {
				location[i] = new FastQueue<>(100, Point2D_F64::new);
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

	/**
	 * Correspondences between images
	 */
	public static class SetMatches {
		// previous left to previous right
		GrowQueue_I32 match0to1 = new GrowQueue_I32(10);
		// previous left to current left
		GrowQueue_I32 match0to2 = new GrowQueue_I32(10);
		// current left to current right
		GrowQueue_I32 match2to3 = new GrowQueue_I32(10);
		// previous right to current right
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

	/**
	 * 3D coordinate of the feature and its observed location in each image
	 */
	public static class QuadView
	{
		// 3D coordinate in old camera view
		public Point3D_F64 X = new Point3D_F64();
		// pixel observation in each camera view
		// left key, right key, left curr, right curr
		public Point2D_F64 v0,v1,v2,v3;
		// If it was an inlier or outlier
		public boolean inlier;

		public void reset() {
			X.set(0,0,0);
			v0=v1=v2=v3=null;
			inlier = false;
		}
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		if( configuration == null ) {
			this.verbose = out;
			return;
		}

		if( configuration.contains(VisualOdometry.VERBOSE_RUNTIME))
			this.profileOut = out;
		if( configuration.contains(VisualOdometry.VERBOSE_TRACKING))
			this.verbose = out;
	}
}

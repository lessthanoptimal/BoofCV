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
import boofcv.abst.feature.associate.AssociateDescriptionSets;
import boofcv.abst.feature.associate.UtilPointFeatures;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.ConfigConverge;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Stereo visual odometry algorithm which associates image features across two stereo pairs for a total of four images.
 * Image features are first matched between left and right images while applying epipolar constraints. Then the two
 * more recent sets of stereo images are associated with each other in a left to left and right to right fashion.
 * Features which are consistently matched across all four images are saved in a list. RANSAC is then used to
 * remove false positives and estimate camera motion using a
 * {@link boofcv.abst.geo.Estimate1ofPnP PnP} type algorithm.
 *
 * Motion is estimated using PNP with RANSAC. The initial 3D location of a feature is found using the stereo pair in
 * the key frame. After the initial motion is found it can optionally be refined. Now that the location of all four
 * cameras is known points are triangulated again using all four views. Then the optional final step is to run
 * bundle adjustment.
 *
 * Features are uniquely tracked from one image to the next. This allows the refined 3D location of each feature
 * to benefit future frames instead of being lost. However, due to the nature of the DDA image tracker, losing track
 * is quite common.
 *
 * The previous stereo pair is referred to as the key frame because it's the reference point that motion is estimated
 * relative to. Inside the code each camera is some times referred to by number. 0 = left camera key frame.
 * 1 = key camera previous frame. 2 = left camera current frame. 3 = right camera current frame.
 *
 * @author Peter Abeles
 */
public class VisOdomStereoQuadPnP<T extends ImageGray<T>,TD extends TupleDesc>
		implements VerbosePrint
{
	// used to estimate each feature's 3D location using a stereo pair
	private final Triangulate2ViewsMetric triangulate;
	private final TriangulateNViewsMetric triangulateN;
	// computes camera motion
	private final @Getter ModelMatcher<Se3_F64, Stereo2D3D> matcher;
	private final ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;

	private final FastQueue<Stereo2D3D> modelFitData = new FastQueue<>(10, Stereo2D3D::new);

	// Bundle Adjustment related data structures. Performing metric bundle adjustment with 3D points
	// To turn off bundle adjustment set the max number of iterations to 0 or less
	private final BundleAdjustment<SceneStructureMetric> bundleAdjustment;
	private final SceneStructureMetric bundleScene = new SceneStructureMetric(false);
	private final SceneObservations bundleObservations = new SceneObservations();
	private final @Getter ConfigConverge bundleConverge = new ConfigConverge(1e-5,1e-5,4);

	// Detects feature inside the image
	private final DetectDescribePoint<T,TD> detector;
	// Associates feature between the same camera
	private final AssociateDescriptionSets<TD> assocF2F;
	// Associates features from left to right camera
	private final AssociateDescription2D<TD> assocL2R;

	// Set of associated features across all views
	private final @Getter FastQueue<TrackQuad> trackQuads = new FastQueue<>(TrackQuad::new, TrackQuad::reset);

	// features info extracted from the stereo pairs. 0 = previous 1 = current
	private ImageInfo featsLeft0,featsLeft1;
	private ImageInfo featsRight0,featsRight1;
	// Matched features between all four images
	private final QuadMatches matches = new QuadMatches();

	// stereo baseline going from left to right
	private final Se3_F64 left_to_right = new Se3_F64();
	private final Se3_F64 right_to_left = new Se3_F64();
	private final StereoParameters stereoParameters = new StereoParameters();

	// convert for original image pixels into normalized image coordinates
	private Point2Transform2_F64 leftPixelToNorm;
	private Point2Transform2_F64 rightPixelToNorm;

	// transform from the current view to the old view (left camera)
	private final Se3_F64 curr_to_key = new Se3_F64();
	// transform from the current camera view to the world frame
	private final Se3_F64 left_to_world = new Se3_F64();

	/** Unique ID for each frame processed */
	private @Getter long frameID = -1;

	// Total number of tracks it has created
	private long totalTracks;
	private final GrowQueue_I32 keyToTrackIdx = new GrowQueue_I32();

	// Internal profiling
	protected @Getter @Setter PrintStream profileOut;
	// Verbose debug information
	protected @Getter PrintStream verbose;

	// Work space variables
	private final Se3_F64 prevLeft_to_world = new Se3_F64();
	private final Point2D_F64 normLeft = new Point2D_F64();
	private final Point2D_F64 normRight = new Point2D_F64();
	private final Point3D_F64 X = new Point3D_F64();
	private final FastQueue<Point2D_F64> listNorm = new FastQueue<>(Point2D_F64::new);
	private final FastQueue<Se3_F64> listWorldToView = new FastQueue<>(Se3_F64::new);
	private final List<TrackQuad> inliers = new ArrayList<>();
	private final List<TrackQuad> consistentTracks = new ArrayList<>();
	private final Se3_F64 found = new Se3_F64();

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
		this.assocF2F = new AssociateDescriptionSets<>(assocF2F,detector.getDescriptionType());
		this.assocL2R = assocL2R;
		this.triangulate = triangulate;
		this.matcher = matcher;
		this.modelRefiner = modelRefiner;
		this.bundleAdjustment = bundleAdjustment;

		this.triangulateN = FactoryMultiView.triangulateNViewCalibrated(ConfigTriangulation.GEOMETRIC());

		featsLeft0 = new ImageInfo();
		featsLeft1 = new ImageInfo();
		featsRight0 = new ImageInfo();
		featsRight1 = new ImageInfo();

		this.assocF2F.initialize(detector.getNumberOfSets());

		listNorm.resize(4);
		listWorldToView.resize(4);
	}

	/**
	 * Sets and saves the stereo camera's calibration
	 */
	public void setCalibration(StereoParameters param)
	{
		this.stereoParameters.set(param);
		right_to_left.set(param.rightToLeft);
		right_to_left.invert(left_to_right);
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
		matches.reset();
		curr_to_key.reset();
		left_to_world.reset();
		frameID = -1;
		totalTracks = 0;
		trackQuads.reset();
	}

	/**
	 * Estimates camera egomotion from the stereo pair
	 * @param left Image from left camera
	 * @param right Image from right camera
	 * @return true if motion was estimated and false if not
	 */
	public boolean process( T left , T right ) {
		frameID++;
		long time0 = System.nanoTime();
		detectFeatures(left,right);
		long time1 = System.nanoTime();
		associateL2R();

		if( frameID==0 ) {
			if( verbose != null ) verbose.println("first frame");
			// mark all features as having no track
			keyToTrackIdx.resize(featsLeft1.locationPixels.size);
			keyToTrackIdx.fill(-1);
		} else {
			long time2 = System.nanoTime();
			associateF2F();
			long time3 = System.nanoTime();
			cyclicConsistency();
			putConsistentTracksIntoList();

			// Estimate the motion robustly
			long time4 = System.nanoTime();
			if ( !robustMotionEstimate()) {
				if( verbose != null ) verbose.println("Failed to estimate motion");
				// odds are that it's totally hosed and you should reset
				// this will undo the most recent tracking results and if the features are still in view it might
				// be able to recover
				abortTrackingResetKeyFrame();
				return false;
			}
			Se3_F64 key_to_curr = matcher.getModelParameters();
			// get a better pose estimate
			refineMotionEstimate(key_to_curr);
			// get better feature locations
			triangulateWithFourCameras(key_to_curr);

			// get the best estimate using bundle adjustment
			long time5 = System.nanoTime();
			performBundleAdjustment(key_to_curr);

			// Drop and update tracks in preperation for the next frame
			long time6 = System.nanoTime();
			performTrackMaintenance(key_to_curr);
			// compound the just found motion with the previously found motion
			key_to_curr.invert(curr_to_key);
			prevLeft_to_world.set(left_to_world);
			curr_to_key.concat(prevLeft_to_world, left_to_world);
			long time7 = System.nanoTime();

			if( profileOut != null ) {
				double milliDet = (time1-time0)*1e-6;
				double milliL2R = (time2-time1)*1e-6;
				double milliF2F = (time3-time2)*1e-6;
				double milliCyc = (time4-time3)*1e-6;
				double milliEst = (time5-time4)*1e-6;
				double milliBun = (time6-time5)*1e-6;
				double milliMnt = (time7-time6)*1e-6;

				profileOut.printf("TIME: Det %5.1f L2R %5.1f F2F %5.1f Cyc %5.1f Est %5.1f Bun %5.1f Mnt %5.1f Total: %5.1f\n",
						milliDet,milliL2R,milliF2F,milliCyc,milliEst,milliBun,milliMnt,(time7-time0)*1e-6);
			}
		}

		if( verbose != null ) {
			int leftDetections = featsLeft1.locationPixels.size;
			int inliers = matcher.getMatchSet().size();
			int matchesL2R = assocL2R.getMatches().size;
			verbose.printf("Viso: Det: %4d L2R: %4d, Quad: %4d Inliers: %d\n",
					leftDetections,matchesL2R, trackQuads.size,inliers);
		}

		return true;
	}

	/**
	 * Handle an aborted update. Undo the latest tracking so that the same frame will be a key frame again.
	 */
	private void abortTrackingResetKeyFrame() {
		swapFeatureFrames();
		matches.swap();
	}

	/**
	 * Puts all the found consistent tracks into a list
	 */
	private void putConsistentTracksIntoList() {
		consistentTracks.clear();
		for (int i = 0; i < trackQuads.size; i++) {
			if( trackQuads.get(i).leftCurrIndex != -1 ) {
				consistentTracks.add(trackQuads.get(i));
			}
		}
	}

	/**
	 * Re-triangulates tracks using observations from all four cameras. Note that geometric error is being minimized
	 * here and not re-projection error
	 */
	private void triangulateWithFourCameras(Se3_F64 key_to_curr) {
		// key left is origin and never changes
		listWorldToView.get(0).reset();
		// key right to key left is also constant and assumed known
		listWorldToView.get(1).set(left_to_right);
		// This was just estimated
		listWorldToView.get(2).set(key_to_curr);
		// (left key -> left curr) -> (left curr -> right curr)
		key_to_curr.concat(left_to_right,listWorldToView.get(3));

		for (int quadIdx = 0; quadIdx < consistentTracks.size(); quadIdx++) {
			TrackQuad q = consistentTracks.get(quadIdx);

			// This could be cached but isn't a bottle neck so it's being left like this since the code is simpler
			leftPixelToNorm.compute(q.v0.x,q.v0.y, listNorm.get(0));
			rightPixelToNorm.compute(q.v1.x,q.v1.y, listNorm.get(1));
			leftPixelToNorm.compute(q.v2.x,q.v2.y, listNorm.get(2));
			rightPixelToNorm.compute(q.v3.x,q.v3.y, listNorm.get(3));

			if( !triangulateN.triangulate(listNorm.toList(), listWorldToView.toList(),X) ) {
				q.leftCurrIndex = -1; // mark it so that it will be remove during maintenance
				continue;
			}

			// something is really messed up if it thinks it's behind the camera
			if( X.z <= 0.0 ) {
				q.leftCurrIndex = -1; // mark it so that it will be remove during maintenance
				continue;
			}

			// save the results
			q.X.set(X);
		}
	}

	/**
	 * Makes the current frame into the key frame. This involves updating the coordinate system of all tracks and
	 * creating a look up table.
	 */
	private void performTrackMaintenance(Se3_F64 key_to_curr) {
		// Drop tracks which do not have known locations in the new frame
		for (int quadIdx = trackQuads.size-1; quadIdx >= 0; quadIdx--) {
			TrackQuad quad = trackQuads.get(quadIdx);
			if( quad.leftCurrIndex == -1 ) {
				trackQuads.removeSwap(quadIdx);
				continue;
			}
			// Convert the coordinate system from the old left to the new left camera
			SePointOps_F64.transform(key_to_curr,quad.X,quad.X);

			// If it's now behind the camera and can't be seen drop the track
			if( quad.X.z <= 0.0 ) {
				trackQuads.removeSwap(quadIdx);
			}
		}

		// Create a lookup table from feature index to track index
		keyToTrackIdx.resize(featsLeft1.locationPixels.size);
		keyToTrackIdx.fill(-1);
		for (int quadIdx = 0; quadIdx < trackQuads.size; quadIdx++) {
			TrackQuad quad = trackQuads.get(quadIdx);
			keyToTrackIdx.data[quad.leftCurrIndex] = quadIdx;
		}
	}

	private void detectFeatures( T left , T right ) {
		// make the previous new observations into the new old ones
		swapFeatureFrames();

		// detect and associate features in the two images
		featsLeft1.reset();
		featsRight1.reset();

		describeImage(left,featsLeft1);
		describeImage(right,featsRight1);
	}

	/**
	 * Swap the feature lists between key and current frames
	 */
	private void swapFeatureFrames() {
		ImageInfo tmp = featsLeft1;
		featsLeft1 = featsLeft0;
		featsLeft0 = tmp;
		tmp = featsRight1;
		featsRight1 = featsRight0;
		featsRight0 = tmp;
	}

	/**
	 * Associates image features from the left and right camera together while applying epipolar constraints.
	 */
	private void associateL2R() {
		// detect and associate features in the current stereo pair
		matches.swap();
		matches.match2to3.reset();

		FastQueue<Point2D_F64> leftLoc = featsLeft1.locationPixels;
		FastQueue<Point2D_F64> rightLoc = featsRight1.locationPixels;

		assocL2R.setSource(leftLoc,featsLeft1.description);
		assocL2R.setDestination(rightLoc, featsRight1.description);
		assocL2R.associate();

		FastAccess<AssociatedIndex> found = assocL2R.getMatches();

		setMatches(matches.match2to3, found, leftLoc.size);
	}

	/**
	 * Associates images between left and left and right and right images
	 */
	private void associateF2F()
	{
		// old left to new left
		UtilPointFeatures.setSource(featsLeft0.description,featsLeft0.sets,assocF2F);
		UtilPointFeatures.setDestination(featsLeft1.description,featsLeft1.sets,assocF2F);
		assocF2F.associate();

		setMatches(matches.match0to2, assocF2F.getMatches(), featsLeft0.locationPixels.size);

		// old right to new right
		UtilPointFeatures.setSource(featsRight0.description,featsRight0.sets,assocF2F);
		UtilPointFeatures.setDestination(featsRight1.description,featsRight1.sets,assocF2F);
		assocF2F.associate();

		setMatches(matches.match1to3, assocF2F.getMatches(), featsRight0.locationPixels.size);
	}

	/**
	 * Create a list of features which have a consistent cycle of matches
	 * 0 -> 1 -> 3 and 0 -> 2 -> 3
	 */
	private void cyclicConsistency() {

		// Initially we don't know the new index of each track
		for (int i = 0; i < trackQuads.size; i++) {
			trackQuads.get(i).leftCurrIndex = -1;
		}

		FastQueue<Point2D_F64> obs0 = featsLeft0.locationPixels;
		FastQueue<Point2D_F64> obs1 = featsRight0.locationPixels;
		FastQueue<Point2D_F64> obs2 = featsLeft1.locationPixels;
		FastQueue<Point2D_F64> obs3 = featsRight1.locationPixels;


		if( matches.match0to1.size != matches.match0to2.size )
			throw new RuntimeException("Failed sanity check");

		for( int indexIn0 = 0; indexIn0 < matches.match0to1.size; indexIn0++ ) {
			int indexIn1 = matches.match0to1.data[indexIn0];
			int indexIn2 = matches.match0to2.data[indexIn0];

			if( indexIn1 < 0 || indexIn2 < 0 )
				continue;

			int indexIn3a = matches.match1to3.data[indexIn1];
			int indexIn3b = matches.match2to3.data[indexIn2];

			if( indexIn3a < 0 || indexIn3b < 0 )
				continue;

			if( indexIn3a != indexIn3b )
				continue;

			// passed the consistency test! Now see if the feature is already matched to a track
			TrackQuad quad;
			int trackIdx = keyToTrackIdx.get(indexIn0);
			if( trackIdx == -1 ) {
				quad = trackQuads.grow();
				quad.id = totalTracks++;
				quad.firstSceneFrameID = frameID;
			} else {
				quad = trackQuads.get(trackIdx);
				quad.inlier = false;
			}
			quad.v0 = obs0.get(indexIn0);
			quad.v1 = obs1.get(indexIn1);
			quad.v2 = obs2.get(indexIn2);
			quad.v3 = obs3.get(indexIn3a);

			// if the feature did't have a track it's location needs to tbe triangulated
			if( trackIdx == -1 ) {
				if (!triangulateTrackTwoViews(quad))
					continue;
			}
			// save it's index in the new frame left frame
			quad.leftCurrIndex = indexIn2;
		}
	}

	/**
	 * Estimate's the 3D location of a track using the key frame stereo
	 */
	private boolean triangulateTrackTwoViews(TrackQuad quad) {
		// convert key frame stereo view to normalized coordinates
		leftPixelToNorm.compute(quad.v0.x, quad.v0.y, normLeft);
		rightPixelToNorm.compute(quad.v1.x, quad.v1.y, normRight);

		// compute 3D location using triangulation
		boolean success = triangulate.triangulate(normLeft, normRight, left_to_right, quad.X);
		success &= !Double.isInfinite(quad.X.normSq());
		success &= quad.X.z > 0.0;

		// Discard this track if it can't be triangulated, discard and abort
		if( !success ) {
			trackQuads.removeTail();
			return false;
		}
		return true;
	}

	/**
	 * Creates a look up table from src image feature index to dst image feature index
	 */
	private void setMatches(GrowQueue_I32 matches, FastAccess<AssociatedIndex> found, int sizeSrc ) {
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
	private void describeImage(T image , ImageInfo info ) {
		detector.detect(image);
		FastQueue<Point2D_F64> l = info.locationPixels;
		FastQueue<TD> d = info.description;
		l.resize(detector.getNumberOfFeatures());
		d.resize(detector.getNumberOfFeatures());
		info.sets.resize(detector.getNumberOfFeatures());
		for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
			l.data[i].set( detector.getLocation(i) );
			d.data[i].setTo( detector.getDescription(i) );
			info.sets.data[i] = detector.getSet(i);
		}
	}

	/**
	 * Robustly estimate the motion
	 */
	private boolean robustMotionEstimate() {
		// create a list of observations and known 3D locations for motion finding
		modelFitData.reset();

		// use 0 -> 1 stereo associations to estimate each feature's 3D position
		for(int i = 0; i < consistentTracks.size(); i++ ) {
			TrackQuad quad = consistentTracks.get(i);
			Stereo2D3D data = modelFitData.grow();
			leftPixelToNorm.compute(quad.v2.x,quad.v2.y,data.leftObs);
			rightPixelToNorm.compute(quad.v3.x,quad.v3.y,data.rightObs);
			data.location.set(quad.X);
		}

		// robustly match the data
		if( !matcher.process(modelFitData.toList()) ) {
			return false;
		}

		// mark features which are inliers
		int numInliers = matcher.getMatchSet().size();
		for (int i = 0; i < numInliers; i++) {
			consistentTracks.get(matcher.getInputIndex(i)).inlier = true;
		}

		return true;
	}

	/**
	 * Non linear refinement of motion estimate
	 *
	 * @param key_to_curr Initial estimate and refined on output
	 */
	private void refineMotionEstimate(Se3_F64 key_to_curr ) {
		// optionally refine the results
		if( modelRefiner != null ) {
			if( modelRefiner.fitModel(matcher.getMatchSet(), key_to_curr, found) ) {
				key_to_curr.set(found);
			}
		}

		// if disabled or it fails just use the robust estimate
	}

	/**
	 * Optimize cameras and feature locations at the same time
	 */
	private void performBundleAdjustment(Se3_F64 key_to_curr ) {
		if( bundleConverge.maxIterations <= 0 )
			return;

		// Must only process inlier tracks here
		inliers.clear();
		for (int trackIdx = 0; trackIdx < consistentTracks.size(); trackIdx++) {
			TrackQuad t = consistentTracks.get(trackIdx);
			if( t.leftCurrIndex != -1 && t.inlier )
				inliers.add(t);
		}

		// Copy the scene into a data structure bundle adjustment understands
		bundleObservations.initialize(4);
		bundleScene.initialize(2,4,inliers.size());
		bundleScene.setCamera(0,true,stereoParameters.left);
		bundleScene.setCamera(1,true,stereoParameters.right);
		bundleScene.setView(0,true,listWorldToView.get(0));
		bundleScene.setView(1,true,listWorldToView.get(1));
		bundleScene.setView(2,false,listWorldToView.get(2));
		bundleScene.setView(3,false,listWorldToView.get(3)); // TODO make fixed relative to 2 in future
		bundleScene.connectViewToCamera(0,0);
		bundleScene.connectViewToCamera(1,1);
		bundleScene.connectViewToCamera(2,0);
		bundleScene.connectViewToCamera(3,1);

		for (int trackIdx = 0; trackIdx < inliers.size(); trackIdx++) {
			TrackQuad t = inliers.get(trackIdx);
			Point3D_F64 X = t.X;
			bundleScene.setPoint(trackIdx,X.x,X.y,X.z);

			bundleObservations.getView(0).add(trackIdx,(float)t.v0.x,(float)t.v0.y);
			bundleObservations.getView(1).add(trackIdx,(float)t.v1.x,(float)t.v1.y);
			bundleObservations.getView(2).add(trackIdx,(float)t.v2.x,(float)t.v2.y);
			bundleObservations.getView(3).add(trackIdx,(float)t.v3.x,(float)t.v3.y);
		}

		bundleAdjustment.setParameters(bundleScene, bundleObservations);
		double scoreBefore = bundleAdjustment.getFitScore();
		bundleAdjustment.configure(bundleConverge.ftol,bundleConverge.gtol,bundleConverge.maxIterations);
		if( !bundleAdjustment.optimize(bundleScene) )
			return;

		if( verbose != null )
			verbose.printf("Bundle: Reduced score by %.2f with tracks %d\n",
					scoreBefore/(1e-16+bundleAdjustment.getFitScore()),bundleScene.points.size);

		// Update the state of tracks and the current views
		for (int trackIdx = 0; trackIdx < inliers.size(); trackIdx++) {
			TrackQuad t = inliers.get(trackIdx);
			bundleScene.points.get(trackIdx).get(t.X);
		}

		// Reminder: World here refers to key left view
		key_to_curr.set(bundleScene.views.get(2).worldToView);
	}

	public Se3_F64 getLeftToWorld() {return left_to_world; }

	/**
	 * Storage for detected features inside an image
	 */
	public class ImageInfo
	{
		// Descriptor of each feature
		FastQueue<TD> description = new FastQueue<>(detector::createDescription);
		// The set each feature belongs in
		GrowQueue_I32 sets = new GrowQueue_I32();
		// The observed location in the image of each feature (pixels)
		FastQueue<Point2D_F64> locationPixels = new FastQueue<>(Point2D_F64::new);

		public void reset() {
			locationPixels.reset();
			description.reset();
			sets.reset();
		}
	}

	/**
	 * Correspondences between images
	 */
	public static class QuadMatches {
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
	public static class TrackQuad
	{
		// Unique ID for this feature
		public long id;
		// Index of the feature in the current left frame
		public int leftCurrIndex;
		// The frame it was first seen in
		public long firstSceneFrameID;

		// 3D coordinate in old camera view
		public Point3D_F64 X = new Point3D_F64();
		// pixel observation in each camera view
		// left key, right key, left curr, right curr
		public Point2D_F64 v0,v1,v2,v3;
		// If it was an inlier in this frame
		public boolean inlier;

		public void reset() {
			X.set(0,0,0);
			v0=v1=v2=v3=null;
			inlier = false;
			id = -1;
			leftCurrIndex = -1;
			firstSceneFrameID = -1;
		}
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		// Default to no verbose messages
		this.verbose = null;
		this.profileOut = null;

		// Update the level of verbosity based on the request
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

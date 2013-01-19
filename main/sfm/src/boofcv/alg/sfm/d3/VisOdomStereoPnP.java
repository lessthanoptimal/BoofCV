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
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTrackerD;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.feature.associate.StereoConsistencyCheck;
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
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO add a second pass option
// TODO add bundle adjustment
// TODO Show right camera tracks in debugger
public class VisOdomStereoPnP<T extends ImageSingleBand,Desc extends TupleDesc> {

	// when the inlier set is less than this number new features are detected
	private int thresholdAdd;

	// discard tracks after they have not been in the inlier set for this many updates in a row
	private int thresholdRetire;

	// computes camera motion
	private ModelMatcher<Se3_F64, Stereo2D3D> matcher;
	private ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;

	// trackers for left and right cameras
	private PointTrackerD<T,Desc> trackerLeft;
	private PointTrackerD<T,Desc> trackerRight;

	FastQueue<Point2D_F64> pointsLeft = new FastQueue<Point2D_F64>(Point2D_F64.class,false);
	FastQueue<Point2D_F64> pointsRight = new FastQueue<Point2D_F64>(Point2D_F64.class,false);
	FastQueue<Desc> descLeft,descRight;

	AssociateDescription2D<Desc> assocL2R;
	TriangulateTwoViewsCalibrated triangulate;

	// convert for original image pixels into normalized image coordinates
	private PointTransform_F64 leftImageToNorm;
	private PointTransform_F64 rightImageToNorm;

	StereoConsistencyCheck stereoCheck;

	// known stereo baseline
	private Se3_F64 leftToRight = new Se3_F64();

	GrowQueue_I32 assignedRight = new GrowQueue_I32();
	// List of tracks from left image that remain after geometric filters have been applied
	private List<PointTrack> candidates = new ArrayList<PointTrack>();

	// transform from key frame to world frame
	private Se3_F64 keyToWorld = new Se3_F64();
	// transform from the current camera view to the key frame
	private Se3_F64 currToKey = new Se3_F64();
	// transform from the current camera view to the world frame
	private Se3_F64 currToWorld = new Se3_F64();

	// number of frames that have been processed
	private int tick;
	// is this the first frame
	private boolean first = true;

	public VisOdomStereoPnP(int thresholdAdd, int thresholdRetire, double epilolarTol,
							PointTrackerD<T,Desc> trackerLeft, PointTrackerD<T,Desc> trackerRight,
							AssociateDescription2D<Desc> assocL2R ,
							TriangulateTwoViewsCalibrated triangulate ,
							ModelMatcher<Se3_F64, Stereo2D3D> matcher ,
							ModelFitter<Se3_F64, Stereo2D3D> modelRefiner ,
							Class<T> imageType )
	{
		this.thresholdAdd = thresholdAdd;
		this.thresholdRetire = thresholdRetire;
		this.trackerLeft = trackerLeft;
		this.trackerRight = trackerRight;
		this.assocL2R = assocL2R;
		this.triangulate = triangulate;
		this.matcher = matcher;
		this.modelRefiner = modelRefiner;

		descLeft = new FastQueue<Desc>(trackerLeft.getDescriptionType(),false);
		descRight = new FastQueue<Desc>(trackerLeft.getDescriptionType(),false);

		stereoCheck = new StereoConsistencyCheck(epilolarTol,epilolarTol);
	}

	public void setCalibration(StereoParameters param) {

		param.rightToLeft.invert(leftToRight);
		leftImageToNorm = LensDistortionOps.transformRadialToNorm_F64(param.left );
		rightImageToNorm = LensDistortionOps.transformRadialToNorm_F64(param.right);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		trackerLeft.reset();
		trackerRight.reset();
		keyToWorld.reset();
		currToKey.reset();
		first = true;
		tick = 0;
	}

	public boolean process( T left , T right ) {

		trackerLeft.process(left);
		trackerRight.process(right);

		if( first ) {
			addNewTracks();
			first = false;
		} else {
			mutualTrackDrop();
			selectCandidateTracks();
			if( !estimateMotion() )
				return false;

			dropUnusedTracks();
			int N = matcher.getMatchSet().size();

			if( modelRefiner != null )
				refineMotionEstimate();

			if( thresholdAdd <= 0 || N < thresholdAdd ) {
				System.out.println("----------- Spawn Tracks --------------");
				changePoseToReference();
				addNewTracks();
			}
		}

		tick++;
		return true;
	}

	private void refineMotionEstimate() {

		// use observations from the inlier set
		List<Stereo2D3D> data = new ArrayList<Stereo2D3D>();

		int N = matcher.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = matcher.getInputIndex(i);

			PointTrack l = candidates.get(index);
			LeftTrackInfo info = l.getCookie();
			PointTrack r = info.right;

			Stereo2D3D stereo = info.location;
			// compute normalized image coordinate for track in left and right image
			leftImageToNorm.compute(l.x,l.y,info.location.leftObs);
			rightImageToNorm.compute(r.x,r.y,info.location.rightObs);

			data.add(stereo);
		}

		// refine the motion estimate using non-linear optimization
		Se3_F64 keyToCurr = currToKey.invert(null);
		Se3_F64 found = new Se3_F64();
		if( modelRefiner.fitModel(data,keyToCurr,found) ) {
			found.invert(currToKey);
		}
	}

	/**
	 * Given the set of active tracks, estimate the cameras motion robustly
	 * @return
	 */
	private boolean estimateMotion() {
		// organize the data
		List<Stereo2D3D> data = new ArrayList<Stereo2D3D>();

		for( PointTrack l : candidates ) {
			LeftTrackInfo info = l.getCookie();
			PointTrack r = info.right;

			Stereo2D3D stereo = info.location;
			// compute normalized image coordinate for track in left and right image
			leftImageToNorm.compute(l.x,l.y,info.location.leftObs);
			rightImageToNorm.compute(r.x,r.y,info.location.rightObs);

			data.add(stereo);
		}

		// Robustly estimate left camera motion
		if( !matcher.process(data) )
			return false;

		Se3_F64 keyToCurr = matcher.getModel();
		keyToCurr.invert(currToKey);

		// mark tracks that are in the inlier set
		int N = matcher.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = matcher.getInputIndex(i);
			LeftTrackInfo info = candidates.get(index).getCookie();
			info.lastConsistent = tick;
		}

		System.out.println("Inlier set size: "+N);

		return true;
	}

	/**
	 * If a track was dropped in one image make sure it was dropped in the other image
	 */
	private void mutualTrackDrop() {
		for( PointTrack t : trackerLeft.getDroppedTracks(null) ) {
			LeftTrackInfo info = t.getCookie();
			trackerRight.dropTrack(info.right);
		}
		for( PointTrack t : trackerRight.getDroppedTracks(null) ) {
			RightTrackInfo info = t.getCookie();
			// a track could be dropped twice here, such requests are ignored by the tracker
			trackerLeft.dropTrack(info.left);
		}
	}


	/**
	 * Searches for tracks which are active and meet the epipolar constraints
	 */
	private void selectCandidateTracks() {
		// mark tracks in right frame that are active
		for( PointTrack t : trackerRight.getActiveTracks(null) ) {
			RightTrackInfo info = t.getCookie();
			info.lastActiveList = tick;
		}

		candidates.clear();
		for( PointTrack left : trackerLeft.getActiveTracks(null) ) {
			LeftTrackInfo info = left.getCookie();

			// for each active left track, see if its right track has been marked as active
			RightTrackInfo infoRight = info.right.getCookie();
			if( infoRight.lastActiveList != tick ) {
				continue;
			}

			// check epipolar constraint and see if it is still valid
			if( stereoCheck.checkPixel(left, info.right) ) {
				candidates.add(left);
			}
		}

	}


	/**
	 * Removes tracks which have not been included in the inlier set recently
	 *
	 * @return Number of dropped tracks
	 */
	private int dropUnusedTracks() {

		List<PointTrack> all = trackerLeft.getAllTracks(null);
		int num = 0;

		for( PointTrack t : all ) {
			LeftTrackInfo info = t.getCookie();
			if( tick - info.lastConsistent >= thresholdRetire ) {
				trackerLeft.dropTrack(t);
				trackerRight.dropTrack(info.right);
				num++;
			}
		}

		return num;
	}

	/**
	 * Updates the relative position of all points so that the current frame is the reference frame.  Mathematically
	 * this is not needed, but should help keep numbers from getting too large.
	 */
	private void changePoseToReference() {
		Se3_F64 keyToCurr = currToKey.invert(null);

		List<PointTrack> all = trackerLeft.getAllTracks(null);

		for( PointTrack t : all ) {
			LeftTrackInfo p = t.getCookie();
			SePointOps_F64.transform(keyToCurr, p.location.location, p.location.location);
		}

		concatMotion();
	}

	private void addNewTracks() {
		trackerLeft.spawnTracks();
		trackerRight.spawnTracks();

		List<PointTrack> newLeft = trackerLeft.getNewTracks(null);
		List<PointTrack> newRight = trackerRight.getNewTracks(null);

		// get a list of new tracks and their descriptions
		addNewToList(trackerLeft,newLeft,pointsLeft,descLeft);
		addNewToList(trackerRight,newRight,pointsRight,descRight);

		// set up data structures
		assignedRight.resize(pointsRight.size);
		for( int i = 0; i < assignedRight.size; i++ )
			assignedRight.data[i] = -1;

		// associate using L2R
		assocL2R.setSource(pointsLeft,descLeft);
		assocL2R.setDestination(pointsRight, descRight);

		FastQueue<AssociatedIndex> matches = assocL2R.getMatches();

		// storage for the triangulated location in the camera frame
		Point3D_F64 cameraP3 = new Point3D_F64();

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex m = matches.get(i);

			if( assignedRight.data[m.dst] != -1 ) {
				throw new RuntimeException("The association must ensure only unique associations for src and dst");
			}
			assignedRight.data[m.dst] = 1;

			PointTrack trackL = newLeft.get(m.src);
			PointTrack trackR = newRight.get(m.dst);

			// declare additional track information stored in each track.  Tracks can be recycled so it
			// might not always need to be declared
			LeftTrackInfo infoLeft = trackL.getCookie();
			if( infoLeft == null )
				trackL.cookie = infoLeft = new LeftTrackInfo();

			RightTrackInfo infoRight = trackR.getCookie();
			if( infoRight == null )
				trackR.cookie = infoRight = new RightTrackInfo();

			Stereo2D3D p2d3d = infoLeft.location;

			// convert pixel observations into normalized image coordinates
			leftImageToNorm.compute(trackL.x,trackL.y,p2d3d.leftObs);
			leftImageToNorm.compute(trackR.x,trackR.y,p2d3d.rightObs);

			// triangulate 3D coordinate in the current camera frame
			if( triangulate.triangulate(p2d3d.leftObs,p2d3d.rightObs,leftToRight,cameraP3) )
			{
				// put the track into the current keyframe coordinate system
				SePointOps_F64.transform(currToKey,cameraP3,p2d3d.location);
				// save a reference to the matching track in the right camera frame
				infoLeft.right = trackR;
				infoLeft.lastConsistent = infoLeft.lastInlier = tick;
				infoRight.left = trackL;
			} else {
				// triangulation failed, drop track
				assignedRight.data[m.dst] = -1;
				trackerLeft.dropTrack(trackL);
			}
		}

		// drop tracks that were not associated
		for( int i = 0; i < assignedRight.size; i++ ) {
			if( assignedRight.data[i] != -1 )
				continue;
			trackerRight.dropTrack(newRight.get(i));
		}
		GrowQueue_I32 unassignedLeft = assocL2R.getUnassociatedSource();
		for( int i = 0; i < unassignedLeft.size; i++ ) {
			int index = unassignedLeft.get(i);
			trackerLeft.dropTrack(newLeft.get(index));
		}
	}

	private void addNewToList( PointTrackerD<T,Desc> tracker , List<PointTrack> tracks ,
							   FastQueue<Point2D_F64> points , FastQueue<Desc> descs )
	{
		points.reset(); descs.reset();

		for( int i = 0; i < tracks.size(); i++ ) {
			PointTrack t = tracks.get(i);

			points.add( t );
			descs.add( tracker.extractDescription(t));
		}
	}


	private void concatMotion() {
		Se3_F64 temp = new Se3_F64();
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se3_F64 getCurrToWorld() {
		currToKey.concat(keyToWorld, currToWorld);
		return currToWorld;
	}

	/**
	 * Returns a list of active tracks that passed geometric constraints
	 */
	public List<PointTrack> getCandidates() {
		return candidates;
	}

	public ModelMatcher<Se3_F64, Stereo2D3D> getMatcher() {
		return matcher;
	}

	public static class LeftTrackInfo
	{
		public Stereo2D3D location = new Stereo2D3D();
		// last time the track was declared as being geometrically consistent
		public int lastConsistent;
		// last time it was in the inlier list
		public int lastInlier;
		// right camera track it is associated with
		public PointTrack right;
	}

	public static class RightTrackInfo
	{
		// used to see if the right track is currently in the active list
		public int lastActiveList;
		// left camera track it is associated with
		public PointTrack left;
	}
}

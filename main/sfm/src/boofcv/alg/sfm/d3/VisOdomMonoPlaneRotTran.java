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

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.sfm.overhead.CameraPlaneProjection;
import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_F64;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.sfm.PlanePtPixel;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.sorting.QuickSelectArray;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates camera egomotion by assuming the ground is a flat plane and that objects not on the plane are an infinite
 * distance away.  Algorithm steps: 1) Estimate rotation using off plane points at infinity. 2) Use rotation estimate
 * to remove rotation component from points on plane. 3) Estimate translation from on plane points.  This is intended
 * to provide fast and reasonably accurate motion estimation.
 *
 * Robust estimation (e.g. RANSAC) is used for estimating yaw and translation, separately.  Location of points is found
 * by intersecting a ray with the ground plane.  At a high level this approach is inspired by [1], but the actual
 * implementation has been heavily modified.
 *
 * No assumptions
 *
 * TODO update documentation
 *
 * @author Peter Abeles
 */
public class VisOdomMonoPlaneRotTran<T extends ImageBase> {

	// Motion estimator for points on plane.  Model is 2D rigid body.  Samples are the 2D coordinate in key frame
	// and pixel observation in current frame.  Motion estimated is from key-frame to current-frame
	private ModelMatcher<Se2_F64, PlanePtPixel> planeMotion;
	// storage for data passed into planeEstimator
	private FastQueue<PlanePtPixel> planeSamples = new FastQueue<PlanePtPixel>(PlanePtPixel.class,true);

	// when the inlier set is less than this number new features are detected
	private int thresholdAdd;
	// discard tracks after they have not been in the inlier set for this many updates in a row
	private int thresholdRetire;
	// maximum allowed pixel error
	private double thresholdPixelError;

	// transform from the plane to the camera
	private Se3_F64 planeToCamera;
	private Se3_F64 cameraToPlane = new Se3_F64();

	// code for projection to/from plane
	private CameraPlaneProjection planeProjection = new CameraPlaneProjection();

	// tracks point features
	private PointTracker<T> tracker;

	// converts pixels between normalized image coordinates and pixel coordinates
	private PointTransform_F64 normToPixel;
	private PointTransform_F64 pixelToNorm;

	// tracks which are assumed to be at an infinite distance away
	private List<PointTrack> tracksFar = new ArrayList<PointTrack>();
	// trans which lie on the ground plane
	private List<PointTrack> tracksOnPlane = new ArrayList<PointTrack>();

	// storage for normalized image coordinate
	private Point2D_F64 n = new Point2D_F64();
	// storage for image pixel coordinate
	private Point2D_F64 pixel = new Point2D_F64();
	// 3D pointing vector of pixel observation
	private Vector3D_F64 pointing = new Vector3D_F64();
	// Adjusted pointing vector which removes off plane rotation
	private Vector3D_F64 pointingAdj = new Vector3D_F64();
	// intersection of the ray with the plane
	private Point3D_F64 rayHitPlane = new Point3D_F64();

	// pointing vector on ground in current frame
	private Point2D_F64 groundCurr = new Point2D_F64();

	// transform from key frame to world frame
	private Se2_F64 keyToWorld = new Se2_F64();
	// transform from the current camera view to the key frame
	private Se2_F64 currToKey = new Se2_F64();
	// transform from the current camera view to the world frame
	private Se2_F64 currToWorld = new Se2_F64();

	// storage for 3D transform
	private Se3_F64 currPlaneToWorld3D = new Se3_F64();
	private Se3_F64 worldToCurrPlane3D = new Se3_F64();
	private Se3_F64 worldToCurrCam3D = new Se3_F64();

	// local variable used in concating transforms
	private Se2_F64 temp = new Se2_F64();

	// angles of rotation computed from points far away
	private GrowQueue_F64 farAngles = new GrowQueue_F64();

	// select angle from points far and the number of points used to estimate it
	private double farAngle;
	private int farCount;

	// found motion for close points.  From key-frame to current-frame
	private Se2_F64 closeMotionKeyToCurr;
	private int closeCount;

	// inlier lists
	private List<PointTrack> farInliers = new ArrayList<PointTrack>();
	private List<PointTrack> closeInliers = new ArrayList<PointTrack>();

	// number of frames processed.  used to decide when tracks should get dropped
	private int tick = 0;
	// is this the first frame being processed?
	private boolean first = true;

	// todo comment
	public VisOdomMonoPlaneRotTran(int thresholdAdd, int thresholdRetire, double thresholdPixelError,
								   ModelMatcher<Se2_F64, PlanePtPixel> planeMotion, PointTracker<T> tracker)
	{
		this.thresholdAdd = thresholdAdd;
		this.thresholdRetire = thresholdRetire;
		this.thresholdPixelError = thresholdPixelError;
		this.planeMotion = planeMotion;
		this.tracker = tracker;
	}

	/**
	 * Camera the camera's intrinsic parameters.  Can be called at any time.
	 * @param intrinsic Intrinsic camera parameters
	 */
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		planeProjection.setIntrinsic(intrinsic);
		normToPixel = LensDistortionOps.transformNormToRadial_F64(intrinsic);
		pixelToNorm = LensDistortionOps.transformRadialToNorm_F64(intrinsic);
	}

	/**
	 * Camera the camera's extrinsic parameters.  Can be called at any time.
	 * @param planeToCamera Transform from the plane to camera.
	 */
	public void setExtrinsic(Se3_F64 planeToCamera) {
		this.planeToCamera = planeToCamera;
		planeToCamera.invert(cameraToPlane);

		planeProjection.setPlaneToCamera(planeToCamera, true);
	}

	/**
	 * Resets the algorithm into its initial state
	 */
	public void reset() {
		first = true;
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		currToWorld.reset();
		worldToCurrCam3D.reset();
	}

	/**
	 * Estimates the motion which the camera undergoes relative to the first frame processed.
	 *
	 * @param image Most recent camera image.
	 * @return true if motion was estimated or false if a fault occurred.  Should reset after a fault.
	 */
	public boolean process( T image ) {
		tracker.process(image);

		tick++;
		closeInliers.clear();
		farInliers.clear();

		if( first ) {
			addNewTracks();
			first = false;
		} else {
			sortTracksForEstimation();

			estimateFar();

			if( !estimateClose() ) {
				return false;
			}

			fuseEstimates();

			dropUnusedTracks();

			if( thresholdAdd <= 0 || closeCount < thresholdAdd ) {
//				System.out.println("Spawning new tracks");
				changeCurrToReference();
				addNewTracks();
			}

//			System.out.println("  num inliers = "+N+"  num dropped "+numDropped+" total active "+tracker.getActivePairs().size());
		}

		return true;
	}

	private void addNewTracks() {
//		System.out.println("----------- Adding new tracks ---------------");

		tracker.spawnTracks();
		List<PointTrack> spawned = tracker.getNewTracks(null);

		// estimate 3D coordinate using stereo vision
		for( PointTrack t : spawned ) {
//			System.out.println("track spawn "+t.x+" "+t.y);
			VoTrack p = t.getCookie();
			if( p == null) {
				t.cookie = p = new VoTrack();
			}

			// compute normalized image coordinate
			pixelToNorm.compute(t.x,t.y,n);

//			System.out.println("         pointing "+pointing.x+" "+pointing.y+" "+pointing.z);

			// See if the point ever intersects the ground plane or not
			if( planeProjection.normalToPlane(n.x,n.y,p.ground) ) {
				// already wrote results to p.ground
				p.onPlane = true;
			} else {
				// rotate pointing vector into plane reference frame
				pointing.set(n.x,n.y,1);
				GeometryMath_F64.mult(cameraToPlane.getR(),pointing,pointing);
				pointing.normalize();

				// save pointing direction y-component.
				p.pointingY = pointing.y;

				// save the angle as a vector
				p.ground.x = pointing.z;
				p.ground.y = -pointing.x;
				// normalize to make later calculations easier
				double norm = p.ground.norm();
				p.ground.x /= norm;
				p.ground.y /= norm;

//				System.out.println("         ground "+p.ground.x+" "+p.ground.y);

				p.onPlane = false;
			}

			p.lastInlier = tick;
		}
	}


	/**
	 * Removes tracks which have not been included in the inlier set recently
	 *
	 * @return Number of dropped tracks
	 */
	private int dropUnusedTracks() {

		List<PointTrack> all = tracker.getAllTracks(null);
		int num = 0;

		for( PointTrack t : all ) {
			VoTrack p = t.getCookie();
			if( tick - p.lastInlier > thresholdRetire ) {
				tracker.dropTrack(t);
				num++;
			}
		}

		return num;
	}

	/**
	 * Splits active tracks into close and far lists.  Prunes obviously bad tracks.
	 */
	private void sortTracksForEstimation() {
//		System.out.println("----------------- Sort");

		// reset data structures
		planeSamples.reset();
		farAngles.reset();
		tracksOnPlane.clear();
		tracksFar.clear();

		// list of active tracks
		List<PointTrack> active = tracker.getActiveTracks(null);

		for( PointTrack t : active ) {
//			System.out.println("sort track "+t.x+" "+t.y);
			VoTrack p = t.getCookie();

			// compute normalized image coordinate
			pixelToNorm.compute(t.x,t.y,n);

			// rotate pointing vector into plane reference frame
			pointing.set(n.x,n.y,1);
			GeometryMath_F64.mult(cameraToPlane.getR(),pointing,pointing);
			pointing.normalize();
//			System.out.println("         pointing "+pointing.x+" "+pointing.y+" "+pointing.z);

			if( p.onPlane ) {
				// see if it still intersects the plane
				if( pointing.y > 0  ) {
					// create data structure for robust motion estimation
					PlanePtPixel ppp = planeSamples.grow();
					ppp.normalizedCurr.set(n);
					ppp.planeKey.set(p.ground);

					tracksOnPlane.add(t);
				}
			} else if( isRotationOnPlane(t,pointing) ) {
				computeAngleOfRotation(t,pointing);
				tracksFar.add(t);
			}
		}
	}

	/**
	 * Determine if there is too much rotation off plane.  Removes change caused by off plane rotation and project
	 * the adjusted point back into.
	 *
	 * NO
	 */
	protected boolean isRotationOnPlane(PointTrack t, Vector3D_F64 pointing ) {

		VoTrack p = t.getCookie();

		// Remove off plane rotation from the pointing vector.
		// TODO this is approximate.  what would exact be?
		pointingAdj.set(pointing.x,pointing.y,pointing.z);
		// Remove any rotation which isn't from the plane
		pointingAdj.y = p.pointingY;
		// Put pointing vector back into camera frame
		GeometryMath_F64.multTran(cameraToPlane.getR(),pointingAdj,pointingAdj);

		// compute normalized image coordinates
		n.x = pointingAdj.x/pointingAdj.z;
		n.y = pointingAdj.y/pointingAdj.z;

		// compute pixel of projected point
		normToPixel.compute(n.x,n.y,pixel);

		// compute error
		double error = pixel.distance2(t);

		return error < thresholdPixelError*thresholdPixelError;
	}

	/**
	 * Computes the angle of rotation between two pointing vectors on the ground plane
	 */
	private void computeAngleOfRotation( PointTrack t, Vector3D_F64 pointing ) {
		VoTrack p = t.getCookie();

		// Compute ground pointing vector
		groundCurr.x = pointing.z;
		groundCurr.y = -pointing.x;
		double norm = groundCurr.norm();
		groundCurr.x /= norm;
		groundCurr.y /= norm;

//		System.out.println("         ground "+groundCurr.x+" "+groundCurr.y);

		// dot product.  vectors are normalized to 1 already
		double dot = groundCurr.x * p.ground.x + groundCurr.y * p.ground.y;
		// floating point round off error some times knocks it above 1.0
		if( dot > 1.0 )
			dot = 1.0;
		double angle = Math.acos(dot);

		// cross product to figure out direction
		if( groundCurr.x * p.ground.y - groundCurr.y * p.ground.x > 0 )
			angle = -angle;

		farAngles.add(angle);

		// make the track as being used
		p.lastInlier = tick;
	}

	/**
	 * Estimates only rotation from points far away
	 */
	private void estimateFar() {

		// select the median value
		farAngle = QuickSelectArray.select(farAngles.data, farAngles.size / 2, farAngles.size);
		farCount = tracksFar.size();

		// TODO might over weight these far points.  select peak instead and count number of angles near by
		// TODO possible to select inliers using pixels?
	}

	/**
	 * Estimates the full 3-dof 2D motion estimate from ground points.
	 * @return true if successful or false if not
	 */
	private boolean estimateClose() {

		// estimate 2D motion
		if( !planeMotion.process(planeSamples.toList()) )
			return false;

		// save solutions
		closeMotionKeyToCurr = planeMotion.getModel();
		closeCount = planeMotion.getMatchSet().size();

		// make inliers as used
		for( int i = 0; i < closeCount; i++ ) {
			int index = planeMotion.getInputIndex(i);
			VoTrack p = tracksOnPlane.get(index).getCookie();
			p.lastInlier = tick;
		}

		return true;
	}

	/**
	 * Fuse the estimates for yaw from both sets of points using a weighted average and save the results into
	 * currToKey
	 */
	private void fuseEstimates() {

		double closeYaw = closeMotionKeyToCurr.getYaw();

		// weighted average for angle
		double x = Math.cos(closeYaw)*closeCount + Math.cos(farAngle)*farCount;
		double y = Math.sin(closeYaw)*closeCount + Math.sin(farAngle)*farCount;

//		System.out.println("Angle close: "+closeYaw+"  far "+farAngle+"  counts "+closeCount+" "+farCount);

		closeMotionKeyToCurr.setYaw(Math.atan2(y, x));

		// save the results
		closeMotionKeyToCurr.invert(currToKey);
	}

	/**
	 * Updates the relative position of all points so that the current frame is the reference frame.  Mathematically
	 * this is not needed, but should help keep numbers from getting too large.
	 */
	private void changeCurrToReference() {
		Se2_F64 keyToCurr = currToKey.invert(null);

		List<PointTrack> all = tracker.getAllTracks(null);

		for( PointTrack t : all ) {
			VoTrack p = t.getCookie();

			if( p.onPlane ) {
				SePointOps_F64.transform(keyToCurr,p.ground,p.ground);
			} else {
				GeometryMath_F64.rotate(keyToCurr.c, keyToCurr.s, p.ground, p.ground);
			}
		}

		concatMotion();
	}

	private void concatMotion() {
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se2_F64 getCurrToWorld2D() {
		currToKey.concat(keyToWorld,currToWorld);
		return currToWorld;
	}

	/**
	 * 3D motion.
	 *
	 * @return from world to current frame.
	 */
	public Se3_F64 getWorldToCurr3D() {

		// compute transform in 2D space
		Se2_F64 currToWorld = getCurrToWorld2D();

		// TODO pull out this code and put into abstract class?
		// 2D to 3D coordinates
		currPlaneToWorld3D.getT().set(-currToWorld.T.y,0,currToWorld.T.x);
		DenseMatrix64F R = currPlaneToWorld3D.getR();

		// set rotation around Y axis.
		// Transpose the 2D transform since the rotation are pointing in opposite directions
		R.unsafe_set(0, 0, currToWorld.c);
		R.unsafe_set(0, 2, -currToWorld.s);
		R.unsafe_set(1, 1, 1);
		R.unsafe_set(2, 0, currToWorld.s);
		R.unsafe_set(2, 2, currToWorld.c);

		currPlaneToWorld3D.invert(worldToCurrPlane3D);

		worldToCurrPlane3D.concat(planeToCamera, worldToCurrCam3D);

		return worldToCurrCam3D;
	}

	public List<PointTrack> getFarInliers() {
		return farInliers;
	}

	public List<PointTrack> getCloseInliers() {
		return closeInliers;
	}

	public static class VoTrack
	{
		// y-axis component of normalized pointing vector in plane reference frame.
		// for points at infinity this should never change, even after key-frame changes.
		// Used only by points at infinity
		double pointingY;

		// ----- Observations in key-frame
		// 2D location or angle vector on ground plane for point on ground and at infinity, respectively
		Point2D_F64 ground = new Point2D_F64();

		// the tick in which it was last an inlier
		public long lastInlier;

		// true for point on plane and false for infinity
		boolean onPlane;

	}
}

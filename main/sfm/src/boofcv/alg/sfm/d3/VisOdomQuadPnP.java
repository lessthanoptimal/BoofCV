///*
// * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package boofcv.alg.sfm.d3;
//
//import boofcv.abst.feature.associate.AssociateDescription2D;
//import boofcv.abst.feature.detdesc.DetectDescribeMulti;
//import boofcv.abst.feature.detdesc.PointDescSet;
//import boofcv.abst.feature.disparity.StereoDisparitySparse;
//import boofcv.abst.feature.tracker.PointTrack;
//import boofcv.abst.feature.tracker.PointTrackerAux;
//import boofcv.alg.distort.LensDistortionOps;
//import boofcv.alg.feature.UtilFeature;
//import boofcv.alg.geo.RectifyImageOps;
//import boofcv.alg.sfm.StereoProcessingBase;
//import boofcv.struct.FastQueue;
//import boofcv.struct.GrowQueue_I32;
//import boofcv.struct.GrowingArrayInt;
//import boofcv.struct.QueueCorner;
//import boofcv.struct.calib.StereoParameters;
//import boofcv.struct.distort.PointTransform_F64;
//import boofcv.struct.feature.AssociatedIndex;
//import boofcv.struct.feature.TupleDesc;
//import boofcv.struct.geo.AssociatedPair;
//import boofcv.struct.image.ImageSingleBand;
//import boofcv.struct.sfm.Stereo2D3D;
//import georegression.struct.point.Point2D_F64;
//import georegression.struct.point.Point3D_F64;
//import georegression.struct.se.Se3_F64;
//import georegression.transform.se.SePointOps_F64;
//import org.ddogleg.fitting.modelset.ModelFitter;
//import org.ddogleg.fitting.modelset.ModelMatcher;
//import org.ejml.data.DenseMatrix64F;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @author Peter Abeles
// */
//// TODO add a second pass option
//// TODO add bundle adjustment
//// TODO Show right camera tracks in debugger
//public class VisOdomQuadPnP<T extends ImageSingleBand,D extends ImageSingleBand,TD extends TupleDesc> {
//
//	// when the inlier set is less than this number new features are detected
//	private int thresholdAdd;
//
//	// discard tracks after they have not been in the inlier set for this many updates in a row
//	private int thresholdRetire;
//
//	// tolerance for deviations in epipolar check
//	private double epilolarTol;
//
//	private QueueCorner excludeList = new QueueCorner(10);
//
//	// computes disparity from two rectified images
//	private StereoDisparitySparse<T> disparity;
//	// Provides standard stereo image processing
//	private StereoProcessingBase<T> stereoIP;
//
//	// storage for rectified left pixel
//	private Point2D_F64 rectLeftPixel = new Point2D_F64();
//	// storage for right pixel
//	private Point2D_F64 imageRightPixel = new Point2D_F64();
//
//	// computes camera motion
//	private ModelMatcher<Se3_F64, Stereo2D3D> matcher;
//	private ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;
//
//	// Detects feature inside the image
//	DetectDescribeMulti<T,TD> detector;
//	// Associates feature between the same camera
//	AssociateDescription2D<TD> assocSame;
//	// Associates features from left to right camera
//	AssociateDescription2D<TD> assocL2R;
//
//	ImageInfo<TD> featsLeft0,featsLeft1;
//	ImageInfo<TD> featsRight0,featsRight1;
//
//	List<AssociatedIndex> match0to1 = new ArrayList<AssociatedIndex>();
//	List<AssociatedIndex> match1to2 = new ArrayList<AssociatedIndex>();
//	List<AssociatedIndex> match2to3 = new ArrayList<AssociatedIndex>();
//	List<AssociatedIndex> match3to4 = new ArrayList<AssociatedIndex>();
//
//	// convert for original image pixels into normalized image coordinates
//	private PointTransform_F64 leftImageToNorm;
//	private PointTransform_F64 rightImageToNorm;
//	// convert from original image pixels into rectified image pixels
//	private PointTransform_F64 leftImageToRect;
//	private PointTransform_F64 rightImageToRect;
//	// convert from rectified pixels into original pixels
//	private PointTransform_F64 rightRectToImage;
//
//
//	// List of tracks from left image that remain after geometric filters have been applied
//	private List<PointTrack> candidates = new ArrayList<PointTrack>();
//
//	// transform from key frame to world frame
//	private Se3_F64 keyToWorld = new Se3_F64();
//	// transform from the current camera view to the key frame
//	private Se3_F64 currToKey = new Se3_F64();
//	// transform from the current camera view to the world frame
//	private Se3_F64 currToWorld = new Se3_F64();
//
//	// number of frames that have been processed
//	private int tick;
//	// is this the first frame
//	private boolean first = true;
//
//	public VisOdomQuadPnP(int thresholdAdd, int thresholdRetire, double epilolarTol,
//						  DetectDescribeMulti<T,TD> detector,
//						  AssociateDescription2D<TD> assocSame , AssociateDescription2D assocL2R ,
//						  ModelMatcher<Se3_F64, Stereo2D3D> matcher,
//						  ModelFitter<Se3_F64, Stereo2D3D> modelRefiner,
//						  StereoDisparitySparse<T> disparity,
//						  Class<T> imageType)
//	{
//		this.thresholdAdd = thresholdAdd;
//		this.thresholdRetire = thresholdRetire;
//		this.epilolarTol = epilolarTol;
//		this.matcher = matcher;
//		this.modelRefiner = modelRefiner;
//		this.disparity = disparity;
//
//		featsLeft0 = new ImageInfo<TD>(detector);
//		featsLeft1 = new ImageInfo<TD>(detector);
//		featsRight0 = new ImageInfo<TD>(detector);
//		featsRight1 = new ImageInfo<TD>(detector);
//
//
//		stereoIP = new StereoProcessingBase<T>(imageType);
//	}
//
//	public void setCalibration(StereoParameters param) {
//		stereoIP.setCalibration(param);
//
//		// rectification matrices
//		DenseMatrix64F rect1 = stereoIP.getRect1();
//		DenseMatrix64F rect2 = stereoIP.getRect2();
//
//		leftImageToNorm = LensDistortionOps.transformRadialToNorm_F64(param.left );
//		rightImageToNorm = LensDistortionOps.transformRadialToNorm_F64(param.right);
//		leftImageToRect = RectifyImageOps.transformPixelToRect_F64(param.left, rect1);
//		rightImageToRect = RectifyImageOps.transformPixelToRect_F64(param.right, rect2);
//		rightRectToImage = RectifyImageOps.transformRectToPixel_F64(param.right, rect2);
//	}
//
//	/**
//	 * Resets the algorithm into its original state
//	 */
//	public void reset() {
//		trackerLeft.reset();
//		trackerRight.reset();
//		keyToWorld.reset();
//		currToKey.reset();
//		first = true;
//		tick = 0;
//	}
//
//	public boolean process( T left , T right ) {
//
//		trackerLeft.process(left);
//		trackerRight.process(right);
//
//		if( first ) {
//			addNewTracks(left,right);
//			first = false;
//		} else {
//			applyGeometryConstraints();
//			if( !estimateMotion() )
//				return false;
//
//			dropUnusedTracks();
//			int N = matcher.getMatchSet().size();
//
//			if( modelRefiner != null )
//				refineMotionEstimate();
//
//			if( thresholdAdd <= 0 || N < thresholdAdd ) {
//				System.out.println("----------- Spawn Tracks --------------");
//				changePoseToReference();
//				addNewTracks(left,right);
//			}
//		}
//
//		tick++;
//		return true;
//	}
//
//	private void associate( T left , T right ) {
//		featsLeft1.reset();
//		featsRight1.reset();
//
//		describeImage(left,featsLeft1);
//		describeImage(right,featsRight1);
//
//		for( int i = 0; i < detector.getNumberOfSets(); i++ ) {
//			FastQueue<Point2D_F64> leftLoc = featsLeft1.location[i];
//			FastQueue<Point2D_F64> rightLoc = featsRight1.location[i];
//
//			assocL2R.setSource(featsLeft1.location[i],featsLeft1.description[i]);
//			assocL2R.setDestination(featsRight1.location[i], featsRight1.description[i]);
//			assocL2R.associate();
//
//			FastQueue<AssociatedIndex> matches = assocL2R.getMatches();
//
//			Point2D_F64 l = featsLeft1.
//
//		}
//	}
//
//	private boolean checkEpipolarConstraint( Point2D_F64 left , Point2D_F64 right ) {
//
//	}
//
//	private void describeImage(T left , ImageInfo<TD> info ) {
//		detector.process(left);
//		for( int i = 0; i < detector.getNumberOfSets(); i++ ) {
//			PointDescSet<TD> set = detector.getFeatureSet(i);
//			FastQueue<Point2D_F64> l = info.location[i];
//			FastQueue<TD> d = info.description[i];
//
//			for( int j = 0; j < set.getNumberOfFeatures(); j++ ) {
//				l.grow().set( set.getLocation(j) );
//				d.grow().setTo( set.getDescription(j) );
//			}
//		}
//	}
//
//	private void refineMotionEstimate() {
//
//		// use observations from the inlier set
//		List<Stereo2D3D> data = new ArrayList<Stereo2D3D>();
//
//		int N = matcher.getMatchSet().size();
//		for( int i = 0; i < N; i++ ) {
//			int index = matcher.getInputIndex(i);
//
//			PointTrack l = candidates.get(index);
//			LeftTrackInfo info = l.getCookie();
//			PointTrack r = info.right;
//
//			Stereo2D3D stereo = info.location;
//			// compute normalized image coordinate for track in left and right image
//			leftImageToNorm.compute(l.x,l.y,info.location.leftObs);
//			rightImageToNorm.compute(r.x,r.y,info.location.rightObs);
//
//			data.add(stereo);
//		}
//
//		// refine the motion estimate using non-linear optimization
//		Se3_F64 keyToCurr = currToKey.invert(null);
//		Se3_F64 found = new Se3_F64();
//		if( modelRefiner.fitModel(data,keyToCurr,found) ) {
//			found.invert(currToKey);
//		}
//	}
//
//	private void applyGeometryConstraints() {
//
//		System.out.println("Before check mutual drop "+trackerLeft.getActiveTracks(null).size());
//		// if a track was dropped in one image drop it in the other
//		for( PointTrack t : trackerLeft.getDroppedTracks(null) ) {
//			LeftTrackInfo info = t.getCookie();
//			trackerRight.dropTrack(info.right);
//		}
//		for( PointTrack t : trackerRight.getDroppedTracks(null) ) {
//			trackerLeft.dropTrack((PointTrack)t.cookie);
//		}
//
//		System.out.println("Before epipolar check "+trackerLeft.getActiveTracks(null).size());
//		// Make a list of tracks which pass an epipolar stereo geometric test
//		candidates.clear();
//		for( PointTrack left : trackerLeft.getActiveTracks(null) ) {
//			LeftTrackInfo info = left.getCookie();
//			PointTrack right = info.right;
//
//			if( checkEpipolar(left,right) ) {
//				candidates.add(left);
//			}
//		}
//		System.out.println("Number of candidates "+candidates.size());
//	}
//
//	private boolean estimateMotion() {
//		// organize the data
//		List<Stereo2D3D> data = new ArrayList<Stereo2D3D>();
//
//		for( PointTrack l : candidates ) {
//			LeftTrackInfo info = l.getCookie();
//			PointTrack r = info.right;
//
//			Stereo2D3D stereo = info.location;
//			// compute normalized image coordinate for track in left and right image
//			leftImageToNorm.compute(l.x,l.y,info.location.leftObs);
//			rightImageToNorm.compute(r.x,r.y,info.location.rightObs);
//
//			data.add(stereo);
//		}
//
//		// Robustly estimate left camera motion
//		if( !matcher.process(data) )
//			return false;
//
//		Se3_F64 keyToCurr = matcher.getModel();
//		keyToCurr.invert(currToKey);
//
//		// mark tracks that are in the inlier set
//		int N = matcher.getMatchSet().size();
//		for( int i = 0; i < N; i++ ) {
//			int index = matcher.getInputIndex(i);
//			LeftTrackInfo info = candidates.get(index).getCookie();
//			info.lastInlier = tick;
//		}
//
//		System.out.println("Inlier set size: "+N);
//
//		return true;
//	}
//
//	/**
//	 * Removes tracks which have not been included in the inlier set recently
//	 *
//	 * @return Number of dropped tracks
//	 */
//	private int dropUnusedTracks() {
//
//		List<PointTrack> all = trackerLeft.getAllTracks(null);
//		int num = 0;
//
//		for( PointTrack t : all ) {
//			LeftTrackInfo info = t.getCookie();
//			if( tick - info.lastInlier >= thresholdRetire ) {
//				trackerLeft.dropTrack(t);
//				trackerRight.dropTrack(info.right);
//				num++;
//			}
//		}
//
//		return num;
//	}
//
//	private boolean checkEpipolar( PointTrack left , PointTrack right ) {
//
//		Point2D_F64 rectLeft = new Point2D_F64();
//		Point2D_F64 rectRight = new Point2D_F64();
//
//		leftImageToRect.compute(left.x,left.y,rectLeft);
//		rightImageToRect.compute(right.x, right.y, rectRight);
//
//		// rectifications should make them appear along the same y-coordinate/epipolar line
//		if( Math.abs(rectLeft.y - rectRight.y) > epilolarTol )
//			return false;
//
//		// features in the right camera should appear left of features in the image image
//		return rectLeft.x - epilolarTol > rectRight.x;
//	}
//
//
//	/**
//	 * Updates the relative position of all points so that the current frame is the reference frame.  Mathematically
//	 * this is not needed, but should help keep numbers from getting too large.
//	 */
//	private void changePoseToReference() {
//		Se3_F64 keyToCurr = currToKey.invert(null);
//
//		List<PointTrack> all = trackerLeft.getAllTracks(null);
//
//		for( PointTrack t : all ) {
//			LeftTrackInfo p = t.getCookie();
//			SePointOps_F64.transform(keyToCurr, p.location.location, p.location.location);
//		}
//
//		concatMotion();
//	}
//
//	private void addNewTracks( T left , T right ) {
//		stereoIP.setImages(left,right);
//		stereoIP.initialize();
//		disparity.setImages(stereoIP.getImageLeftRect(), stereoIP.getImageRightRect());
//
//		List<PointTrack> tracks = trackerLeft.getActiveTracks(null);
//
//		// don't detect existing tracks twice
//		excludeList.reset();
//		for( PointTrack t : tracks ) {
//			excludeList.add( (int)t.x , (int)t.y );
//		}
//
//		// detect new features
//		trackerLeft.spawnTracks();
//		List<PointTrack> spawned = trackerLeft.getNewTracks(null);
//
//		for( int i = 0; i < spawned.size(); i++ ) {
//			// point in original image coordinates
//			PointTrack trackLeft = spawned.get(i);
//
//			// convert point into rectified coordinates
//			leftImageToRect.compute(trackLeft.x,trackLeft.y, rectLeftPixel);
//
//			// find the disparity between the two images
//			double d = computeDisparity(rectLeftPixel.x, rectLeftPixel.y);
//
//			// exclude points at infinity
//			if( d > 0 ) {
//				// start a track in the right image at the found location
//				// and convert back into original pixel coordinates
//				rightRectToImage.compute(rectLeftPixel.x-d, rectLeftPixel.y, imageRightPixel);
//
//				// Create track in right image and drop it in both if it fails
//				PointTrack trackRight = trackerRight.addTrack(imageRightPixel.x, imageRightPixel.y,null);
//
//				if( trackRight == null ) {
//					trackerLeft.dropTrack(trackLeft);
//					continue;
//				}
//
//				if( trackLeft.cookie == null ) {
//					trackLeft.cookie = new LeftTrackInfo();
//				}
//				// compute the point's 3D coordinate in the camera's reference frame
//				LeftTrackInfo leftInfo = trackLeft.getCookie();
//				disparityTo3D(rectLeftPixel.x, rectLeftPixel.y,d,leftInfo.location.location);
//				// Mark the tick so it isn't immediately dropped
//				leftInfo.lastInlier = tick;
//
//				// Save reference to both tracks in each other
//				leftInfo.right = trackRight;
//				trackRight.cookie = trackLeft;
//			} else {
//				trackerLeft.dropTrack(trackLeft);
//			}
//		}
//	}
//
//	private double computeDisparity( double x , double y ) {
//		if( disparity.process((int)(x+0.5),(int)(y+0.5)) ) {
//			return disparity.getDisparity();
//		} else {
//			return 0;
//		}
//	}
//
//	private void disparityTo3D( double x , double y, double disparity , Point3D_F64 location ) {
//		stereoIP.computeHomo3D(x, y, location);
//
//		// convert from homogeneous coordinates into 3D
//		location.x /= disparity;
//		location.y /= disparity;
//		location.z /= disparity;
//	}
//
//	private void concatMotion() {
//		Se3_F64 temp = new Se3_F64();
//		currToKey.concat(keyToWorld,temp);
//		keyToWorld.set(temp);
//		currToKey.reset();
//	}
//
//	public Se3_F64 getCurrToWorld() {
//		currToKey.concat(keyToWorld, currToWorld);
//		return currToWorld;
//	}
//
//	/**
//	 * Returns a list of active tracks that passed geometric constraints
//	 */
//	public List<PointTrack> getCandidates() {
//		return candidates;
//	}
//
//	public ModelMatcher<Se3_F64, Stereo2D3D> getMatcher() {
//		return matcher;
//	}
//
//	public static class ImageInfo<TD extends TupleDesc>
//	{
//		FastQueue<Point2D_F64> location[];
//		FastQueue<TD> description[];
//
//		public ImageInfo( DetectDescribeMulti<?,TD> detector ) {
//			location = new FastQueue[ detector.getNumberOfSets() ];
//			description = new FastQueue[ detector.getNumberOfSets() ];
//
//			for( int i = 0; i < location.length; i++ ) {
//				location[i] = new FastQueue<Point2D_F64>(100,Point2D_F64.class,true);
//				description[i] = UtilFeature.createQueue(detector,100);
//			}
//		}
//
//		public void reset() {
//			for( int i = 0; i < location.length; i++ ) {
//				location[i].reset();
//				description[i].reset();
//			}
//		}
//	}
//
//	public static class LeftTrackInfo
//	{
//		public Stereo2D3D location = new Stereo2D3D();
//		public int lastInlier;
//		public PointTrack right;
//	}
//}

/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.sfm;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.BundleAdjustmentCalibrated;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.bundle.CalibratedPoseAndPoint;
import boofcv.alg.geo.bundle.ViewPointObservations;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.sfm.robust.DistanceSe3SymmetricSq;
import boofcv.alg.sfm.robust.EstimatorToGenerator;
import boofcv.alg.sfm.robust.Se3FromEssentialGenerator;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageFloat32;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO example with more images maybe 6?
// TODO discard features which are visible for less than 3 images
// TODO visualize results
// TODO unit tests
// TODO comment
public class ExampleStructureFromMotion {

	IntrinsicParameters intrinsic;
	PointTransform_F64 pixelToNorm;

	// tolerance for inliers in pixels
	double inlierTol = 0.5;

	DetectDescribePoint<ImageFloat32, SurfFeature> detDesc = FactoryDetectDescribe.surfStable(null, null, null, ImageFloat32.class);
	ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(SurfFeature.class, true);
	AssociateDescription<SurfFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

	TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

	FastQueue<SurfFeature> featuresA = new SurfFeatureQueue(64);
	FastQueue<SurfFeature> featuresB = new SurfFeatureQueue(64);

	// pixels stored as normalized image coordinates
	FastQueue<Point2D_F64> pixelsA = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
	FastQueue<Point2D_F64> pixelsB = new FastQueue<Point2D_F64>(Point2D_F64.class, true);

	GrowQueue_I32 colorsA = new GrowQueue_I32();
	GrowQueue_I32 colorsB = new GrowQueue_I32();


	List<Track> tracks = new ArrayList<Track>();
	List<Track> activeTracks = new ArrayList<Track>();
	List<Track> activeTracks2 = new ArrayList<Track>();
	// transforms from camera to world for each frame after frame 0
	List<Se3_F64> motionWorldToCamera = new ArrayList<Se3_F64>();

	public void process(IntrinsicParameters intrinsic , List<ImageFloat32> images) {

		this.intrinsic = intrinsic;
		pixelToNorm = LensDistortionOps.transformRadialToNorm_F64(intrinsic);

		detectFeatures(images.get(0), featuresA, pixelsA, colorsA);
		detectFeatures(images.get(1), featuresB, pixelsB, colorsB);

		initialize(featuresA,featuresB,pixelsA,pixelsB,colorsA);

		swap();
		for( int i = 2; i < images.size(); i++ ) {
			detectFeatures(images.get(i), featuresB, pixelsB, colorsB);
			addFrame(featuresA, featuresB, pixelsA, pixelsB,colorsA);
			swap();
		}
;
		normalizeScale();
//		performBundleAdjustment();
//		normalizeScale();

		for( Se3_F64 m : motionWorldToCamera) {
			m.print();
		}

		PointCloudViewer gui = new PointCloudViewer(intrinsic,1);

		for( Track t : tracks ) {
			gui.addPoint(t.worldPt.x,t.worldPt.y,t.worldPt.z,t.color);
		}

		gui.setPreferredSize(new Dimension(500,500));
		ShowImages.showWindow(gui,"Points");
	}

	/**
	 * Swap features and pixel lists A and B to recycle data
	 */
	private void swap() {
		FastQueue<SurfFeature> tmpF = featuresA;
		featuresA = featuresB;
		featuresB = tmpF;

		FastQueue<Point2D_F64> tmpP = pixelsA;
		pixelsA = pixelsB;
		pixelsB = tmpP;

		GrowQueue_I32 tmpC = colorsA;
		colorsA = colorsB;
		colorsB = tmpC;
	}

	private void detectFeatures(ImageFloat32 image,
								FastQueue<SurfFeature> features,
								FastQueue<Point2D_F64> pixels,
								GrowQueue_I32 colors ) {
		features.reset();
		pixels.reset();
		colors.reset();
		detDesc.detect(image);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 p = detDesc.getLocation(i);

			features.grow().set(detDesc.getDescription(i));
			// store pixels are normalized image coordinates
			pixelToNorm.compute(p.x, p.y, pixels.grow());

			int v = (int)image.get((int)p.x,(int)p.y);
			colors.add( v << 16 | v << 8 | v );
		}
	}

	protected void initialize(FastQueue<SurfFeature> featuresA, FastQueue<SurfFeature> featuresB,
							  FastQueue<Point2D_F64> pixelsA, FastQueue<Point2D_F64> pixelsB,
							  GrowQueue_I32 colorsA ) {

		// associate the features together
		associate.setSource(featuresA);
		associate.setDestination(featuresB);
		associate.associate();

		FastQueue<AssociatedIndex> matches = associate.getMatches();

		// create the associated pair for motion estimation
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for (int i = 0; i < matches.size(); i++) {
			AssociatedIndex a = matches.get(i);
			pairs.add(new AssociatedPair(pixelsA.get(a.src), pixelsB.get(a.dst)));
		}

		// Since this is the first frame estimate the camera motion up to a translational scale factor
		Estimate1ofEpipolar essentialAlg = FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_5_NISTER, 5);

		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg, triangulate);

		DistanceFromModel<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,
						intrinsic.fx, intrinsic.fy, intrinsic.skew,
						intrinsic.fx, intrinsic.fy, intrinsic.skew);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierTol * inlierTol * 2.0;

		ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
				new Ransac<Se3_F64, AssociatedPair>(2323, manager, generateEpipolarMotion, distanceSe3,
						200, ransacTOL);

		// don't forget that the observations in pairs is in normalized image coordinates
		if (!epipolarMotion.process(pairs))
			throw new RuntimeException("Motion estimation failed");

		// the motion is estimated from src to dst
		Se3_F64 motionAtoB = epipolarMotion.getModelParameters();
		motionWorldToCamera.add(motionAtoB.copy());

		// create tracks for only those features in the inlier list
		int N = epipolarMotion.getMatchSet().size();
		for (int i = 0; i < N; i++) {
			int index = epipolarMotion.getInputIndex(i);
			AssociatedIndex a = matches.get(index);

			Track t = new Track();
			t.color = colorsA.get(a.src);
			t.prevIndex = a.dst;
			t.obs.grow().set( pixelsA.get(a.src) );
			t.obs.grow().set( pixelsB.get(a.dst) );
			t.frame.add(0);
			t.frame.add(1);
			// compute the 3D coordinate of the feature
			triangulate.triangulate(pixelsA.get(a.src),pixelsB.get(a.dst),motionAtoB,t.worldPt);

			tracks.add(t);
		}

		activeTracks.clear();
		activeTracks.addAll(tracks);
	}

	public void addFrame( FastQueue<SurfFeature> featuresA, FastQueue<SurfFeature> featuresB,
						  FastQueue<Point2D_F64> pixelsA, FastQueue<Point2D_F64> pixelsB ,
						  GrowQueue_I32 colorsA )
	{
		// associate the features together
		associate.setSource(featuresA);
		associate.setDestination(featuresB);
		associate.associate();

		FastQueue<AssociatedIndex> matches = associate.getMatches();

		// create the associated pair for motion estimation
		List<Point2D3D> features = new ArrayList<Point2D3D>();
		for (int i = 0; i < matches.size(); i++) {
			AssociatedIndex a = matches.get(i);
			Track t = lookupTrack(a.src);
			if( t != null ) {
				Point2D_F64 p = pixelsB.get(a.dst);
				features.add(new Point2D3D(p, t.worldPt));
			}
		}

		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		final DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();
		distance.setIntrinsic(intrinsic.fx,intrinsic.fy,intrinsic.skew);

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator = new EstimatorToGenerator<Se3_F64,Point2D3D>(estimator);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierTol * inlierTol;

		ModelMatcher<Se3_F64, Point2D3D> motionEstimator =
				new Ransac<Se3_F64, Point2D3D>(2323, manager, generator, distance, 200, ransacTOL);

		if( !motionEstimator.process(features))
			throw new RuntimeException("Motion estimation failed");

		Se3_F64 motionWorldToB = motionEstimator.getModelParameters().copy();
		motionWorldToCamera.add(motionWorldToB);
		Se3_F64 motionBtoWorld = motionWorldToB.invert(null);
		Se3_F64 motionWorldToA = motionWorldToCamera.get(motionWorldToCamera.size() - 2);

		Se3_F64 motionBtoA =  motionBtoWorld.concat( motionWorldToA,null);

		// create tracks for only those features in the inlier list
		activeTracks2.clear();
		int N = motionEstimator.getMatchSet().size();
		Point3D_F64 pt_in_b = new Point3D_F64();
		for (int i = 0; i < N; i++) {
			int index = motionEstimator.getInputIndex(i);
			AssociatedIndex a = matches.get(index);

			Track t = lookupTrack(a.src);
			if( t == null ) {
				// TODO these haven't been verified through RANSAC and shouldn't be part of the final output
				t = new Track();
				t.color = colorsA.get(a.src);
				t.prevIndex = a.dst;
				t.obs.grow().set( pixelsA.get(a.src) );
				t.obs.grow().set( pixelsB.get(a.dst) );
				t.frame.add(this.motionWorldToCamera.size()-1);
				t.frame.add(this.motionWorldToCamera.size());
				// compute point location in B frame
				triangulate.triangulate(pixelsB.get(a.dst),pixelsA.get(a.src),motionBtoA,pt_in_b);
				// transform from B back to world frame
				SePointOps_F64.transform(motionBtoWorld,pt_in_b,t.worldPt);

				tracks.add(t);
			} else {
				t.prevIndex = a.dst;
				t.obs.grow().set(pixelsB.get(a.dst));
				t.frame.add(motionWorldToCamera.size());
			}

			activeTracks2.add(t);
		}

		List<Track> tmp = activeTracks2;
		activeTracks2 = activeTracks;
		activeTracks = tmp;
	}

	private Track lookupTrack( int prevIndex ) {
		for (int i = 0; i < activeTracks.size(); i++) {
			Track t = activeTracks.get(i);
			if( t.prevIndex == prevIndex )
				return t;
		}
		return null;
	}

	protected void performBundleAdjustment() {
		System.out.println("Starting bundle adjustment");
		BundleAdjustmentCalibrated bundle = FactoryMultiView.bundleCalibrated(1e-8,3);

		CalibratedPoseAndPoint initialModel = new CalibratedPoseAndPoint();
		List<ViewPointObservations> observations = new ArrayList<ViewPointObservations>();

		initialModel.configure(motionWorldToCamera.size()+1,tracks.size());

		initialModel.setViewKnown(0,true);
		initialModel.getWorldToCamera(0).reset();

		for (int i = 0; i < motionWorldToCamera.size(); i++) {
			initialModel.getWorldToCamera(i+1).set(motionWorldToCamera.get(i));
		}

		for (int i = 0; i < tracks.size(); i++) {
			Track t = tracks.get(i);

			initialModel.getPoint(i).set(t.worldPt);
		}

		for( int frameIndex = 0; frameIndex <= motionWorldToCamera.size(); frameIndex++ ) {
			ViewPointObservations o = new ViewPointObservations();

			for (int j = 0; j < tracks.size(); j++) {
				Track t = tracks.get(j);
				for (int k = 0; k < t.obs.size(); k++) {
					int obsFrame = t.frame.get(k);

					if( obsFrame == frameIndex ) {
						Point2D_F64 p = t.obs.get(k);
						o.getPoints().grow().set(j,p);
					}
				}
			}

			observations.add(o);
		}

		if( !bundle.process(initialModel,observations) )
			throw new RuntimeException("Bundle adjustment failed!");

		// TODO Copy results!
		System.out.println("Done with BA!");
	}

	public void normalizeScale() {

		double T = motionWorldToCamera.get(0).T.norm();
		double scale = 1.0/T;

		for( Se3_F64 m : motionWorldToCamera) {
			m.T.scale(scale);
		}

		for( Track t : tracks ) {
			t.worldPt.scale(scale);
		}
	}

	public static class Track {
		// color of the pixel first found int
		int color;
		// estimate 3D position of the feature
		Point3D_F64 worldPt = new Point3D_F64();
		// observations in each frame that it's visible
		FastQueue<Point2D_F64> obs = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
		// index of each frame its visible in
		GrowQueue_I32 frame = new GrowQueue_I32();

		// index of the feature in the previous frame.  Used to build tracks
		int prevIndex = 0;
	}

	public static void main(String[] args) {

		List<ImageFloat32> images = new ArrayList<ImageFloat32>();

		IntrinsicParameters intrinsic =
				UtilIO.loadXML("../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/intrinsic.xml");

		images.add(UtilImageIO.loadImage("../data/applet/stereo/mono_wall_01.jpg",ImageFloat32.class));
		images.add(UtilImageIO.loadImage("../data/applet/stereo/mono_wall_02.jpg",ImageFloat32.class));
		images.add(UtilImageIO.loadImage("../data/applet/stereo/mono_wall_03.jpg",ImageFloat32.class));

		ExampleStructureFromMotion example = new ExampleStructureFromMotion();

		example.process(intrinsic,images);
	}
}

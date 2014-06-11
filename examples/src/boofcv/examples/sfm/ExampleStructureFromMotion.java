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
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.CalibratedPoseAndPoint;
import boofcv.alg.geo.bundle.ViewPointObservations;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.sfm.robust.*;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryTriangulate;
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
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.*;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Doesn't handle pure rotations.  Almost pure rotation has poor performance.
 * Doesn't close the loop.  Pose error increases with each image.
 *
 * @author Peter Abeles
 */
// TODO Incorporate test to see if two sequential images are good for computing epipolar geometry
// TODO use results from essential matrix calculation to prune initial list of p3p points

// TODO example with more images maybe 6?
// TODO discard features which are visible for less than 3 images
// TODO visualize results
// TODO unit tests
// TODO comment
public class ExampleStructureFromMotion {

	IntrinsicParameters intrinsic;
	PointTransform_F64 pixelToNorm;

	double keyframeRatio = 1.6;

	// tolerance for inliers in pixels
	double inlierTol = 1.5;

	DetectDescribePoint<ImageFloat32, SurfFeature> detDesc = FactoryDetectDescribe.surfStable(null, null, null, ImageFloat32.class);
	ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(SurfFeature.class, true);
	AssociateDescription<SurfFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

	TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

	List<FastQueue<SurfFeature>> imageFeatures = new ArrayList<FastQueue<SurfFeature>>();
	List<FastQueue<Point2D_F64>> imagePixels = new ArrayList<FastQueue<Point2D_F64>>();
	List<GrowQueue_I32> imageColors = new ArrayList<GrowQueue_I32>();

	List<Track> tracks = new ArrayList<Track>();
	List<Track> candidate = new ArrayList<Track>();
	List<Track> activeTracks = new ArrayList<Track>();
	List<Track> activeTracks2 = new ArrayList<Track>();
	// transforms from camera to world for each frame after frame 0
	List<Se3_F64> motionWorldToCamera = new ArrayList<Se3_F64>();

	ModelMatcher<Se3_F64, AssociatedPair> estimateEssential;
	ModelMatcher<Homography2D_F64,AssociatedPair> estimateHomography;
	ModelMatcher<Se3_F64, Point2D3D> estimatePnP;
	ModelFitter<Se3_F64, Point2D3D> refinePnP = FactoryMultiView.refinePnP(1e-12,40);


	public void process(IntrinsicParameters intrinsic , List<BufferedImage> colorImages ) {

		this.intrinsic = intrinsic;
		pixelToNorm = LensDistortionOps.transformRadialToNorm_F64(intrinsic);

		setupHomography();
		setupEssential();
		setupPnP();

		// find features in each image
		System.out.println("Detecting Features in each image");
		for (BufferedImage colorImage : colorImages) {
			FastQueue<SurfFeature> features = new SurfFeatureQueue(64);
			FastQueue<Point2D_F64> pixels = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
			GrowQueue_I32 colors = new GrowQueue_I32();
			detectFeatures(colorImage, features, pixels, colors);

			imageFeatures.add(features);
			imagePixels.add(pixels);
			imageColors.add(colors);
		}

		// see which images are the most similar to each o ther
		double matrix[][] = new double[colorImages.size()][colorImages.size()];

		for (int i = 0; i < colorImages.size(); i++) {
			for (int j = i+1; j < colorImages.size(); j++) {
				System.out.printf("Associated %02d %02d ",i,j);
				associate.setSource(imageFeatures.get(i));
				associate.setDestination(imageFeatures.get(j));
				associate.associate();

				matrix[i][j] = associate.getMatches().size()/(double)imageFeatures.get(i).size();
				matrix[j][i] = associate.getMatches().size()/(double)imageFeatures.get(j).size();

				System.out.println(" = "+matrix[i][j]);
			}
		}

		// find the image which is connected to the most other images.  Use that as the origin of the arbitrary
		// coordinate system
		int bestImage = -1;
		int bestCount = 0;
		for (int i = 0; i < colorImages.size(); i++) {
			int count = 0;
			for (int j = 0; j < colorImages.size(); j++) {
				if( matrix[i][j] > 0.3 ) {
					count++;
				}
			}
			System.out.println(i+"  count "+count);
			if( count > bestCount ) {
				bestCount = count;
				bestImage = i;
			}
		}

		// pick the image most similar to the original image to initialize pose estimation


		// estimate the motion to the rest in order of greatest similarity

		// for each connected image see if there are images connected to it without their motion estimated


		// continue until no more images can be estimated


//		System.out.println("Processing initial pair");
//		detectFeatures(colorImages.get(0), featuresA, pixelsA, colorsA);
//		detectFeatures(colorImages.get(1), featuresB, pixelsB, colorsB);
//
//		initialize(featuresA,featuresB,pixelsA,pixelsB,colorsA);
//
//		swap();
//		for( int i = 2; i < colorImages.size(); i++ ) {
//			System.out.println("Processing image "+i);
//			detectFeatures(colorImages.get(i), featuresB, pixelsB, colorsB);
//			addFrame(featuresA, featuresB, pixelsA, pixelsB,colorsA);
//			swap();
//		}
//
//		normalizeScale();
////		performBundleAdjustment();
////		normalizeScale();
//
//		for( Se3_F64 m : motionWorldToCamera) {
//			m.print();
//		}
//
//		PointCloudViewer gui = new PointCloudViewer(intrinsic,1);
//
//		for( Track t : tracks ) {
//			gui.addPoint(t.worldPt.x,t.worldPt.y,t.worldPt.z,t.color);
//		}
//
//		gui.setPreferredSize(new Dimension(500,500));
//		ShowImages.showWindow(gui,"Points");
	}

	private void setupPnP() {
		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER, -1, 2);
		final DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();
		distance.setIntrinsic(intrinsic.fx,intrinsic.fy,intrinsic.skew);

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator = new EstimatorToGenerator<Se3_F64,Point2D3D>(estimator);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierTol * inlierTol;

		estimatePnP = new Ransac<Se3_F64, Point2D3D>(2323, manager, generator, distance, 2000, ransacTOL);
	}

	private void setupEssential() {
		// Since this is the first frame estimate the camera motion up to a translational scale factor
		Estimate1ofEpipolar essentialAlg = FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_5_NISTER, 5);

		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg, triangulate);

		DistanceFromModel<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,
						intrinsic.fx, intrinsic.fy, intrinsic.skew,
						intrinsic.fx, intrinsic.fy, intrinsic.skew);

		// tolerance for RANSAC inliers
		double ransacTOL = inlierTol * inlierTol * 2.0;

		estimateEssential = new Ransac<Se3_F64, AssociatedPair>(2323, manager, generateEpipolarMotion, distanceSe3,
				4000, ransacTOL);
	}

	private void setupHomography() {
		ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
		GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(true);
		DistanceHomographySq distance = new DistanceHomographySq();

		// tolerance for RANSAC inliers
		double ransacTOL = inlierTol * inlierTol;

		estimateHomography =
				new Ransac<Homography2D_F64,AssociatedPair>(123,manager,modelFitter,distance,300,ransacTOL);
	}

	private void detectFeatures(BufferedImage colorImage,
								FastQueue<SurfFeature> features,
								FastQueue<Point2D_F64> pixels,
								GrowQueue_I32 colors ) {

		ImageFloat32 image = ConvertBufferedImage.convertFrom(colorImage, (ImageFloat32) null);

		features.reset();
		pixels.reset();
		colors.reset();
		detDesc.detect(image);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 p = detDesc.getLocation(i);

			features.grow().set(detDesc.getDescription(i));
			// store pixels are normalized image coordinates
			pixelToNorm.compute(p.x, p.y, pixels.grow());

			colors.add( colorImage.getRGB((int)p.x,(int)p.y) );
		}
	}

	protected void initialize(FastQueue<SurfFeature> featuresA, FastQueue<SurfFeature> featuresB,
							  FastQueue<Point2D_F64> pixelsA, FastQueue<Point2D_F64> pixelsB,
							  GrowQueue_I32 colorsA ) {

		Se3_F64 motionAtoB = new Se3_F64();
		List<AssociatedIndex> inliers = new ArrayList<AssociatedIndex>();
		if( !findMatches(featuresA,featuresB,pixelsA,pixelsB,motionAtoB,inliers))
			throw new RuntimeException("The first image pair is a bad keyframe!");

		motionWorldToCamera.add(motionAtoB.copy());

		// create tracks for only those features in the inlier list
		for (int i = 0; i < inliers.size(); i++) {
			AssociatedIndex a = inliers.get(i);

			Track t = new Track();
			t.stable = true;
			t.color = colorsA.get(a.src);
			t.prevIndex = a.dst;
			t.obs.grow().set( pixelsA.get(a.src) );
			t.obs.grow().set( pixelsB.get(a.dst) );
			t.frame.add(0);
			t.frame.add(1);
			// compute the 3D coordinate of the feature
			Point2D_F64 pa = pixelsA.get(a.src);
			Point2D_F64 pb = pixelsB.get(a.dst);

//			double theta = computeAngle(pa, pb);
//
//			if( theta >= Math.PI*10/180.0 ) {
				triangulate.triangulate(pa, pb, motionAtoB, t.worldPt);
				// the feature has to be in front of the camera
				if (t.worldPt.z > 0) {
					tracks.add(t);
				}
//			}
		}

		activeTracks.clear();
		activeTracks.addAll(tracks);
	}

	private double computeAngle(Point2D_F64 pa, Point2D_F64 pb) {
		double dotAB = pa.x*pb.x + pa.y*pb.y + 1;
		double nA = Math.sqrt(pa.x*pa.x + pa.y*pa.y + 1);
		double nB = Math.sqrt(pb.x*pb.x + pb.y*pb.y + 1);
		return Math.acos(dotAB/(nA*nB));
	}

	public void addFrame( FastQueue<SurfFeature> featuresA, FastQueue<SurfFeature> featuresB,
						  FastQueue<Point2D_F64> pixelsA, FastQueue<Point2D_F64> pixelsB ,
						  GrowQueue_I32 colorsA )
	{
		List<AssociatedIndex> inliers = new ArrayList<AssociatedIndex>();
		if( !findMatches(featuresA,featuresB,pixelsA,pixelsB,new Se3_F64(),inliers))
			throw new RuntimeException("Bad image pair is a bad keyframe!");

		// create the associated pair for motion estimation
		List<Point2D3D> features = new ArrayList<Point2D3D>();
		List<AssociatedIndex> inputRansac = new ArrayList<AssociatedIndex>();
		List<AssociatedIndex> unmatched = new ArrayList<AssociatedIndex>();
		for (int i = 0; i < inliers.size(); i++) {
			AssociatedIndex a = inliers.get(i);
			Track t = lookupTrack(a.src);
			if( t != null ) {
				Point2D_F64 p = pixelsB.get(a.dst);
				features.add(new Point2D3D(p, t.worldPt));
				inputRansac.add(a);
			} else {
				unmatched.add(a);
			}
		}

		if( !estimatePnP.process(features))
			throw new RuntimeException("Motion estimation failed");


		Se3_F64 motionWorldToB = new Se3_F64();
		refinePnP.fitModel(estimatePnP.getMatchSet(),estimatePnP.getModelParameters(),motionWorldToB);
//		motionWorldToB.set(estimatePnP.getModelParameters());

		motionWorldToCamera.add(motionWorldToB);
		Se3_F64 motionBtoWorld = motionWorldToB.invert(null);
		Se3_F64 motionWorldToA = motionWorldToCamera.get(motionWorldToCamera.size() - 2);

		Se3_F64 motionBtoA =  motionBtoWorld.concat(motionWorldToA, null);

		// create tracks for only those features in the inlier list
		activeTracks2.clear();
		int N = estimatePnP.getMatchSet().size();
		System.out.println("  PNP inliers "+N+"  ransac input "+inputRansac.size());
		Point3D_F64 pt_in_b = new Point3D_F64();
		for (int i = 0; i < N; i++) {
			int index = estimatePnP.getInputIndex(i);
			AssociatedIndex a = inputRansac.get(index);

			Track t = lookupTrack(a.src);
			t.prevIndex = a.dst;
			t.obs.grow().set(pixelsB.get(a.dst));
			t.frame.add(motionWorldToCamera.size());
			if( !t.stable ) {
				t.stable = true;
				tracks.add(t);
			}

//			Point3D_F64 tmp3 = new Point3D_F64();
//			triangulate.triangulate(pixelsB.get(a.dst),pixelsA.get(a.src),motionBtoA,pt_in_b);
//			SePointOps_F64.transform(motionBtoWorld, pt_in_b, tmp3);
//			System.out.println("  actual " + t.worldPt);
//			System.out.println("  found  "+tmp3);


			activeTracks2.add(t);
		}

		int totalAdded = 0;
		// create new tracks from the set of matched features which were not used.
		for( int i = 0; i < unmatched.size(); i++ ) {
			AssociatedIndex a = unmatched.get(i);
			Track t = new Track();
			t.stable = false;
			t.color = colorsA.get(a.src);
			t.prevIndex = a.dst;
			t.obs.grow().set( pixelsA.get(a.src) );
			t.obs.grow().set( pixelsB.get(a.dst) );
			t.frame.add(this.motionWorldToCamera.size()-1);
			t.frame.add(this.motionWorldToCamera.size());

			Point2D_F64 pa = pixelsA.get(a.src);
			Point2D_F64 pb = pixelsB.get(a.dst);

			double theta = computeAngle(pa, pb);

			// make sure it can be triangulated with a little bit of accuracy
//			if( theta >= Math.PI*10/180.0 ) {
				// compute point location in B frame
				triangulate.triangulate(pixelsB.get(a.dst),pixelsA.get(a.src),motionBtoA,pt_in_b);

				// the feature has to be in front of the camera
				if( pt_in_b.z > 0 ) {
					// transform from B back to world frame
					SePointOps_F64.transform(motionBtoWorld, pt_in_b, t.worldPt);

					activeTracks2.add(t);
					totalAdded++;
				}
//			}
		}

		System.out.println("New ones added "+totalAdded);

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

	protected boolean findMatches(FastQueue<SurfFeature> featuresA, FastQueue<SurfFeature> featuresB,
								  FastQueue<Point2D_F64> pixelsA, FastQueue<Point2D_F64> pixelsB,
								  Se3_F64 motionAtoB ,
								  List<AssociatedIndex> inliers )
	{
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

		if( !estimateHomography.process(pairs) )
			throw new RuntimeException("Failed to estimate homography");

		List<AssociatedPair> inliersHomography = estimateHomography.getMatchSet();

		if( !estimateEssential.process(pairs) )
			throw new RuntimeException("Motion estimation failed");

		List<AssociatedPair> inliersEssential = estimateEssential.getMatchSet();

		System.out.println("Homography "+inliersHomography.size()+"  essential "+inliersEssential.size());

		if( inliersHomography.size() >= inliersEssential.size()*keyframeRatio ) {
			System.err.println("WARNING: Degenerate geometry found.");
//			return false;
		}

		motionAtoB.set(estimateEssential.getModelParameters());

		for (int i = 0; i < inliersEssential.size(); i++) {
			int index = estimateEssential.getInputIndex(i);

			inliers.add( matches.get(index));
		}

		return true;
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
		boolean stable;
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

		List<BufferedImage> images = new ArrayList<BufferedImage>();

		String directory = "/home/pja/Desktop/sfm/day02/a/";

		double scaleFactor = 1.0;

		IntrinsicParameters intrinsic = UtilIO.loadXML(directory+"intrinsic_DSC-HX5_3648x2736_to_640x480.xml");
		if( scaleFactor != 1.0 )
			PerspectiveOps.scaleIntrinsic(intrinsic,scaleFactor);

		for( int i = 0; i < 18; i++ ) {
			BufferedImage b = UtilImageIO.loadImage(directory+String.format("image%03d.jpg",i));

			BufferedImage c;
			if( scaleFactor != 1.0 ) {
				c = new BufferedImage((int) (b.getWidth() * scaleFactor), (int) (b.getHeight() * scaleFactor), b.getType());

				Graphics2D g2 = c.createGraphics();

				g2.scale(scaleFactor, scaleFactor);
				g2.drawImage(b, 0, 0, null);
			} else {
				c = b;
			}

			images.add(c);
		}

		ExampleStructureFromMotion example = new ExampleStructureFromMotion();

		example.process(intrinsic,images);
	}
}

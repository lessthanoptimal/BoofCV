/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.RefineEpipolar;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.robust.RansacMultiView;
import boofcv.alg.geo.robust.SelectBestStereoTransform;
import boofcv.alg.sfm.EstimateSceneStructure;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.geo.*;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilVector3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.*;

/**
 * Assumes the input images are in an arbitrary order and that any image can be connected to any other image.
 * A brute force approach is used to determine connectivity between images making this an O(N<sup>2</sup>) algorithm.
 * This its not suitable for a large number of images.
 *
 * All cameras must either be calibrated or uncalibrated.
 *
 * @author Peter Abeles
 */
// TODO This approach is very sensitive to the origin which is selected
	//
public class EstimateSceneUnordered<T extends ImageBase<T>> implements EstimateSceneStructure<T>
{
	// Used to pre-maturely stop the scene estimation process
	private volatile boolean stopRequested = false;

	private double MIN_ASSOCIATE_FRACTION = 0.05;
	private int MIN_FEATURE_ASSOCIATED = 30;
	private double TRIANGULATE_MIN_ANGLE = Math.PI/20.0;

	ConfigEssential configEssential = new ConfigEssential();
	ConfigFundamental configFundamental = new ConfigFundamental();
	ConfigRansac configRansac = new ConfigRansac();

	// Transform (including distortion terms) from pixel into normalized image coordinates
	Map<String,Point2Transform2_F64> camerasPixelToNorm = new HashMap<>();
	// Approximate camera model used to compute pixel errors
	Map<String,CameraPinhole> camerasIntrinsc = new HashMap<>();
	// camera name to index
	Map<String,Integer> cameraToIndex = new HashMap<>();

	// indicates if all the cameras are calibrated or uncalibrated
	boolean calibrated;

	DetectDescribePoint<T,TupleDesc> detDesc;
	// score ans association algorithm
	ScoreAssociation<TupleDesc> scorer;
	AssociateDescription<TupleDesc> associate;

	RansacMultiView<Se3_F64,AssociatedPair> ransacEssential;
	Ransac<DMatrixRMaj,AssociatedPair> ransacFundamental;
	RansacMultiView<Se3_F64, Point2D3D> ransacPnP;

	RefineEpipolar refineEpipolar;

	// Temporary storage for feature pairs which are inliers
	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair.class,true);

	// Triangulates the 3D coordinate of a point from two observations
	TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

	List<CameraView> graphNodes = new ArrayList<>();
	List<CameraMotion> graphEdges = new ArrayList<>();
	List<Feature3D> features3D = new ArrayList<>();

	// This are the views were actually added
	List<CameraView> viewsAdded = new ArrayList<>();

	// work space for feature angles
	private Vector3D_F64 arrowA = new Vector3D_F64();
	private Vector3D_F64 arrowB = new Vector3D_F64();

	// Output
	BundleAdjustmentSceneStructure structure;
	BundleAdjustmentObservations observations;

	// Verbose output to standard out
	boolean verbose = false;

	public EstimateSceneUnordered( DetectDescribePoint<T,TupleDesc> detDesc ) {
		this();
		this.detDesc = detDesc;
		scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
		associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);
	}

	/**
	 * Constructor for unit tests
	 */
	EstimateSceneUnordered() {
		configRansac.inlierThreshold = 2.5;
		configRansac.maxIterations = 4000;
	}

	@Override
	public void addCamera(String cameraName) {
		if( camerasPixelToNorm.isEmpty() )
			calibrated = false;
		else if( calibrated )
			throw new IllegalArgumentException("All cameras must be calibrated or uncalibrated");
		camerasPixelToNorm.put( cameraName , null );
	}

	@Override
	public void addCamera(String cameraName, LensDistortionNarrowFOV intrinsic, int width, int height ) {
		if( camerasPixelToNorm.isEmpty() )
			calibrated = true;
		else if( !calibrated )
			throw new IllegalArgumentException("All cameras must be calibrated or uncalibrated");

		Point2Transform2_F64 pixelToNorm = intrinsic.undistort_F64(true,false);

		camerasPixelToNorm.put( cameraName, pixelToNorm );
		camerasIntrinsc.put(cameraName, PerspectiveOps.estimatePinhole(pixelToNorm,width,height));
		cameraToIndex.put( cameraName , cameraToIndex.size() );
	}

	/**
	 * Detect features inside the image and save the results
	 * @param image
	 */
	@Override
	public void add(T image , String cameraName )
	{
		if( cameraName == null || !camerasPixelToNorm.containsKey(cameraName))
			throw new IllegalArgumentException("Must specify the camera first");

		CameraView view = new CameraView(graphNodes.size(),
				new FastQueue<TupleDesc>(TupleDesc.class,true) {
					@Override
					protected TupleDesc createInstance() {
						return detDesc.createDescription();
					}
				});

		view.camera = cameraName;
		graphNodes.add(view);

		detDesc.detect(image);

		// Pre-declare memory
		view.descriptions.growArray(detDesc.getNumberOfFeatures());
		view.observationPixels.growArray(detDesc.getNumberOfFeatures());
		view.features3D = new Feature3D[detDesc.getNumberOfFeatures()];

		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 p = detDesc.getLocation(i);

			// save copies since detDesc recycles memory
			view.descriptions.grow().setTo(detDesc.getDescription(i));
			view.observationPixels.grow().set(p);
		}

		Point2Transform2_F64 pixelToNorm = camerasPixelToNorm.get(cameraName);
		if( pixelToNorm == null ){
			return;
		}

		view.observationNorm.growArray(detDesc.getNumberOfFeatures());
		for (int i = 0; i < view.observationPixels.size; i++) {
			Point2D_F64 p = view.observationPixels.get(i);
			pixelToNorm.compute(p.x,p.y,view.observationNorm.grow());
		}

		if( verbose ) {
			System.out.println("Detected Features: "+detDesc.getNumberOfFeatures());
		}
	}

	@Override
	public boolean estimate() {
		if( graphNodes.size() < 2 )
			return false;

		declareModelFitting();

		for (int i = 0; i < graphNodes.size(); i++) {
			for (int j = i+1; j < graphNodes.size(); j++) {
				connectViews( graphNodes.get(i), graphNodes.get(j));
				if( stopRequested )
					return false;
			}
		}
		if( graphEdges.size() < 1 )
			return false;

		// Select the view which will act as the origin
		CameraView origin = selectOriginNode();
		// Select the motion which will define the coordinate system
		CameraMotion baseMotion = selectCoordinateBase( origin );
		defineCoordinateSystem(origin, baseMotion);
		if( stopRequested )
			return false;
		// Triangulate initial points
		triangulateInitialSeed(origin,baseMotion);

		// Now estimate all the other view locations and 3D features
		estimateAllFeatures(origin, baseMotion.destination(origin));
		if( stopRequested )
			return false;

		// Convert the graph into the output format
		convertToOutput(origin);

		return viewsAdded.size() >= 2;
	}

	protected void declareModelFitting() {
		if( calibrated ) {
			ransacEssential = FactoryMultiViewRobust.essentialRansac(configEssential, configRansac);
			ransacPnP = FactoryMultiViewRobust.pnpRansac(null,configRansac);
		} else {
			ransacFundamental = FactoryMultiViewRobust.fundamentalRansac(configFundamental, configRansac);
			// TODO figure out how to do  PnP in uncalibrated case
		}
		refineEpipolar = FactoryMultiView.refineFundamental(1e-32,10,EpipolarError.SAMPSON);
	}

	/**
	 * Converts the internal data structures into the output format for bundle adjustment. Camera models are omitted
	 * since they are not available
	 * @param origin The origin of the coordinate system
	 */
	private void convertToOutput( CameraView origin ) {
		structure = new BundleAdjustmentSceneStructure(false);
		observations = new BundleAdjustmentObservations(viewsAdded.size());

		structure.initialize(cameraToIndex.size(),viewsAdded.size(), features3D.size());

		// look up table from old index to new index
		int viewOldToView[] = new int[ graphNodes.size() ];
		Arrays.fill(viewOldToView,-1);
		for (int i = 0; i < viewsAdded.size(); i++) {
			viewOldToView[ graphNodes.indexOf(viewsAdded.get(i))] = i;
		}


		for( int i = 0; i < viewsAdded.size(); i++ ) {
			CameraView v = viewsAdded.get(i);
			int cameraIndex = cameraToIndex.get(v.camera);
			structure.setView(i,v==origin,v.viewToWorld.invert(null));
			structure.connectViewToCamera(i,cameraIndex);
		}

		for (int indexPoint = 0; indexPoint < features3D.size(); indexPoint++) {
			Feature3D f = features3D.get(indexPoint);

			structure.setPoint(indexPoint,f.worldPt.x,f.worldPt.y,f.worldPt.z);

			if( f.views.size() != f.obsIdx.size )
				throw new RuntimeException("BUG!");

			for (int j = 0; j < f.views.size(); j++) {
				CameraView view = f.views.get(j);
				int viewIndex = viewOldToView[view.index];
				structure.connectPointToView(indexPoint,viewIndex);

				Point2D_F64 pixel = viewsAdded.get(viewIndex).observationPixels.get(f.obsIdx.get(j));
				observations.getView(viewIndex).add(indexPoint,(float)(pixel.x),(float)(pixel.y));
			}
		}

	}

	/**
	 * Perform a breath first search to find the structure of all the remaining camrea views
	 */
	private void estimateAllFeatures(CameraView seedA, CameraView seedB ) {
		List<CameraView> open = new ArrayList<>();

		// Add features for all the other views connected to the root view and determine the translation scale factor
		addUnvistedToStack(seedA, open);
		addUnvistedToStack(seedB, open);

		// Do a breath first search. The queue is first in first out
		while( !open.isEmpty() ) {
			if( stopRequested )
				return;
			if( verbose )
				System.out.println("### open.size="+open.size());

			// select the view with the 3D features. This view can be estimated which the highest degree of confience
			int bestCount = countFeaturesWith3D(open.get(0));
			int bestIndex = 0;

			for (int i = 1; i < open.size(); i++) {
				int count = countFeaturesWith3D(open.get(i));
				if( count > bestCount ) {
					bestCount = count;
					bestIndex = i;
				}
			}

			CameraView v = open.remove(bestIndex);
			if( verbose )
				System.out.println("   processing view="+v.index+" | 3D Features="+bestCount);

			// Figure out it's 3D structure
			if( !determinePose(v) ) {
				// The pose could not be determined, so remove it from the graph
				if( verbose )
					System.out.println("   Removing connection");
				for (CameraMotion m : v.connections) {
					CameraView a = m.destination(v);
					a.connections.remove(m);
					graphEdges.remove(m);
				}
				graphNodes.remove(v);

				for (int i = 0; i < graphNodes.size(); i++) {
					graphNodes.get(i).index = i;
				}

//				throw new RuntimeException("Crap handle this");
			} else {
				triangulateNoLocation(v);

				viewsAdded.add(v);

				// Update the open list
				addUnvistedToStack(v, open);
			}
		}
	}

	/**
	 * Count how many 3D features are in view.
	 */
	private int countFeaturesWith3D(CameraView v ) {

		int count = 0;

		for (int i = 0; i < v.connections.size(); i++) {
			CameraMotion m = v.connections.get(i);

			boolean isSrc = m.viewSrc == v;

			for (int j = 0; j < m.features.size(); j++) {
				AssociatedIndex a = m.features.get(j);

				if( isSrc ) {
					count += m.viewDst.features3D[a.dst] != null ? 1 : 0;
				} else {
					count += m.viewSrc.features3D[a.src] != null ? 1 : 0;
				}
			}
		}

		return count;
	}

	/**
	 * Uses the previously found motion between the two cameras to estimate the scale and 3D point of common features.
	 * If a feature already has a known 3D point that is not modified. Scale is found by computing the 3D coordinate
	 * of all points with a 3D point again then dividing the two distances. New features are also triangulated
	 * and have their location's update using this scale.
	 *
	 * A known feature has the current view added to its list of views.
	 */
	private boolean determinePose(CameraView target ) {

		// Find all Features which are visible in this view and have a known 3D location
		List<Point2D3D> list = new ArrayList<>();
		List<Feature3D> features = new ArrayList<>();
		GrowQueue_I32 featureIndexes = new GrowQueue_I32();


		// TODO mark need to handle casees where the target's index has changed due to node removal
		// Find all the known 3D features which are visible in this view
		for( CameraMotion c : target.connections ) {
			boolean isSrc = c.viewSrc == target;
			CameraView other = c.destination(target);
			if( other.state != ViewState.PROCESSED )
				continue;

			for (int i = 0; i < c.features.size(); i++) {
				AssociatedIndex a = c.features.get(i);

				Feature3D f = other.features3D[isSrc?a.dst:a.src];
				if( f == null || f.mark == target.index)
					continue;
				f.mark = target.index;
				features.add(f);
				featureIndexes.add( isSrc?a.src:a.dst);
				Point2D_F64 norm = target.observationNorm.get( isSrc?a.src:a.dst);

				Point2D3D p = new Point2D3D();
				p.location.set(f.worldPt);
				p.observation.set(norm);
				list.add(p);
			}
		}

		// Estimate the target's location using robust PNP
		ransacPnP.setIntrinsic(0,camerasIntrinsc.get(target.camera));
		if( list.size() < 100 || !ransacPnP.process(list) ) {
			if (verbose)
				System.out.println("   View="+target.index+" RANSAC failed. list.size="+list.size());
			return false;
		}

		target.state = ViewState.PROCESSED;

		// add inliers to the features
		int N = ransacPnP.getMatchSet().size();
		if( verbose )
			System.out.println("   View="+target.index+" PNP RANSAC "+N+"/"+list.size());
		for (int i = 0; i < N; i++) {
			int which = ransacPnP.getInputIndex(i);
			Feature3D f = features.get(which);
			if( f.views.contains(target))
				continue;
			f.views.add(target);
			f.obsIdx.add(featureIndexes.get(which));
			target.features3D[featureIndexes.get(which)] = f;

			if( f.views.size() != f.obsIdx.size )
				throw new RuntimeException("BUG!");
		}

		Se3_F64 worldToView = ransacPnP.getModelParameters();
		target.viewToWorld.set( worldToView.invert(null) );

		return true;
	}

	private void triangulateNoLocation( CameraView target ) {

		TriangulateTwoViewsCalibrated triangulator = FactoryMultiView.triangulateTwoGeometric();

		Se3_F64 otherToTarget = new Se3_F64();

		Se3_F64 worldToTarget = target.viewToWorld.invert(null);

		for( CameraMotion c : target.connections ) {
			boolean isSrc = c.viewSrc == target;
			CameraView other = c.destination(target);
			if( other.state != ViewState.PROCESSED )
				continue;

			other.viewToWorld.concat(worldToTarget,otherToTarget);

			for (int i = 0; i < c.features.size(); i++) {
				AssociatedIndex a = c.features.get(i);

				int indexTarget = isSrc ? a.src : a.dst;
				int indexOther = isSrc ? a.dst : a.src;
				if( target.features3D[indexTarget] != null || other.features3D[indexOther] != null )
					continue;

				Point2D_F64 normOther = other.observationNorm.get( indexOther );
				Point2D_F64 normTarget = target.observationNorm.get( indexTarget );

				// Skip points with poor geometry
				double angle = triangulationAngle(normOther,normTarget,otherToTarget);
				if( angle < TRIANGULATE_MIN_ANGLE )
					continue;

				Feature3D f = new Feature3D();
				if( !triangulator.triangulate(normOther,normTarget,otherToTarget,f.worldPt))
					continue;

				SePointOps_F64.transform(other.viewToWorld,f.worldPt,f.worldPt);
				f.views.add( target );
				f.views.add( other );
				f.obsIdx.add( indexTarget );
				f.obsIdx.add( indexOther );

				features3D.add(f);
				target.features3D[indexTarget] = f;
				other.features3D[indexOther] = f;
			}
		}
	}

	/**
	 * Computes the acture angle between two vectors. Larger this angle is the better the triangulation
	 * of the features 3D location is in general
	 */
	private double triangulationAngle( Point2D_F64 normA , Point2D_F64 normB , Se3_F64 a_to_b ) {
		// the more parallel a line is worse the triangulation. Get rid of bad ideas early here
		arrowA.set(normA.x,normA.y,1);
		arrowB.set(normB.x,normB.y,1);
		GeometryMath_F64.mult(a_to_b.R,arrowA,arrowA); // put them into the same reference frame

		return UtilVector3D_F64.acute(arrowA,arrowB);
	}

	/**
	 * Looks to see which connections have yet to be visited and adds them to the open list
	 */
	private void addUnvistedToStack(CameraView viewed, List<CameraView> open) {
		for (int i = 0; i < viewed.connections.size(); i++) {
			CameraView other = viewed.connections.get(i).destination(viewed);
			if( other.state == ViewState.UNPROCESSED) {
				other.state = ViewState.PENDING;
				open.add(other);
				if( verbose)
					System.out.println("  adding to open "+viewed.index+"->"+other.index);
			}
		}
	}

	private void defineCoordinateSystem(CameraView viewA, CameraMotion baseMotion) {

//		refineEpipolarMotion(baseMotion);

		CameraView viewB = baseMotion.destination(viewA);
		viewA.viewToWorld.reset(); // identity since it's the origin
		viewB.viewToWorld.set(baseMotion.motionSrcToDst(viewB));
		viewB.viewToWorld.T.normalize(); // only known up to a scale factor

		viewsAdded.add(viewA);
		viewsAdded.add(viewB);

		viewA.state = ViewState.PROCESSED;
		viewB.state = ViewState.PROCESSED;

		System.out.println("root  = "+viewA.index);
		System.out.println("other = "+viewB.index);
		System.out.println("-------------");

	}

	// TODO does this make a difference???
	private void refineEpipolarMotion( CameraMotion motion ) {
		CameraView viewA = motion.viewSrc;
		CameraView viewB = motion.viewDst;

		pairs.resize(motion.features.size());
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex a = motion.features.get(i);
			pairs.get(i).p1.set(viewA.observationNorm.get(a.src));
			pairs.get(i).p2.set(viewB.observationNorm.get(a.dst));

		}
		motion.a_to_b.T.normalize();
		DMatrixRMaj E = MultiViewOps.createEssential(motion.a_to_b.R,motion.a_to_b.T);
		DMatrixRMaj refinedE = new DMatrixRMaj(3,3);
		refineEpipolar.fitModel(pairs.toList(),E,refinedE);

		List<Se3_F64> candidates = MultiViewOps.decomposeEssential(refinedE);
		new SelectBestStereoTransform().select(candidates,pairs.toList(),motion.a_to_b);
	}

	CameraView selectOriginNode() {
		double bestScore = 0;
		CameraView best = null;

		if( verbose )
			System.out.println("selectOriginNode");
		for (int i = 0; i < graphNodes.size(); i++) {
			double score = 0;
			List<CameraMotion> edges = graphNodes.get(i).connections;

			for (int j = 0; j < edges.size(); j++) {
				CameraMotion e = edges.get(j);
				score += e.scoreTriangulation();
			}

			if( score > bestScore ) {
				bestScore = score;
				best = graphNodes.get(i);
			}

			if( verbose )
				System.out.printf("  [%2d] score = %s\n",i,score);
		}
		if( verbose )
			System.out.println("     selected = "+best.index);

		return best;
	}

	/**
	 * Select motion which will define the coordinate system.
	 */
	CameraMotion selectCoordinateBase(CameraView view ) {
		double bestScore = 0;
		CameraMotion best = null;

		if( verbose )
			System.out.println("selectCoordinateBase");
		for (int i = 0; i < view.connections.size(); i++) {
			CameraMotion e = view.connections.get(i);

			double s = e.scoreTriangulation();
			if( verbose )
				System.out.printf("  [%2d] score = %s\n",i,s);
			if( s > bestScore ) {
				bestScore = s;
				best = e;
			}
		}
		return best;
	}

	/**
	 * For the two seed views just triangulate all the common features. The motion already has its translation
	 * normalized to one
	 */
	private void triangulateInitialSeed( CameraView origin, CameraMotion edge ) {

		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		Se3_F64 viewAtoB = new Se3_F64();
		viewA.viewToWorld.concat(viewB.viewToWorld.invert(null),viewAtoB);

		for (int i = 0; i < edge.features.size(); i++) {
			AssociatedIndex f = edge.features.get(i);

			// some association algorithms allow multiple solutions. Just ignore that to keep the code simple
			if( viewA.features3D[f.src]!=null || viewB.features3D[f.dst]!=null )
				continue;

			Point2D_F64 normA = viewA.observationNorm.get(f.src);
			Point2D_F64 normB = viewB.observationNorm.get(f.dst);

			Feature3D feature3D = new Feature3D();

			feature3D.obsIdx.add( f.src );
			feature3D.obsIdx.add( f.dst );

			double angle = triangulationAngle(normA,normB,viewAtoB);
			if( angle < TRIANGULATE_MIN_ANGLE )
				continue;

			if( !triangulate.triangulate(normA,normB,viewAtoB,feature3D.worldPt) ) {
				System.out.println("  triangulation failed??!");
				continue;
			}

			if( viewB == origin ) {
				SePointOps_F64.transform(viewAtoB,feature3D.worldPt,feature3D.worldPt);
			}

			// mark this feature3D as being associated with these image features
			viewA.features3D[f.src] = feature3D;
			viewB.features3D[f.dst] = feature3D;

			// record which frame the feature was seen in
			if( feature3D.views.contains(viewA))
				throw new RuntimeException("Egads");
			if( feature3D.views.contains(viewB))
				throw new RuntimeException("Egads");
			feature3D.views.add(viewA);
			feature3D.views.add(viewB);

			// Add it to the overall list
			features3D.add(feature3D);
		}

		System.out.println("Initialized features from views "+viewA.index+" and "+viewB.index+" features3D="+ features3D.size()+"  edges.size="+edge.features.size());
	}

	/**
	 * Associate features between the two views. Then compute a homography and essential matrix using LSMed. Add
	 * features to the edge if they an inlier in essential. Save fit score of homography vs essential.
	 */
	void connectViews( CameraView viewA , CameraView viewB ) {
		associate.setSource(viewA.descriptions);
		associate.setDestination(viewB.descriptions);
		associate.associate();

		if( associate.getMatches().size < MIN_FEATURE_ASSOCIATED )
			return;

		FastQueue<AssociatedIndex> matches = associate.getMatches();

		// Estimate fundamental/essential with RANSAC
		CameraMotion edge = new CameraMotion();
		int inliersEpipolar;
		if( calibrated ) {
			ransacEssential.setIntrinsic(0,camerasIntrinsc.get(viewA.camera));
			ransacEssential.setIntrinsic(1,camerasIntrinsc.get(viewB.camera));
			if( !fitEpipolar(matches, viewA.observationNorm.toList(), viewB.observationNorm.toList(),ransacEssential,edge) )
				return;
			inliersEpipolar = ransacEssential.getMatchSet().size();
			edge.a_to_b.set( ransacEssential.getModelParameters() );
		} else {
			if( !fitEpipolar(matches, viewA.observationPixels.toList(), viewB.observationPixels.toList(),ransacFundamental,edge) )
				return;
			inliersEpipolar = ransacFundamental.getMatchSet().size();
			// TODO save rigid body estimate
		}

		if( inliersEpipolar < MIN_FEATURE_ASSOCIATED )
			return;

		// If only a very small number of features are associated do not consider the view
		double fractionA = inliersEpipolar/(double)viewA.descriptions.size;
		double fractionB = inliersEpipolar/(double)viewB.descriptions.size;

		if( fractionA < MIN_ASSOCIATE_FRACTION | fractionB < MIN_ASSOCIATE_FRACTION )
			return;

		// If the geometry is good for triangulation this number will be lower
		edge.viewSrc = viewA;
		edge.viewDst = viewB;
		edge.triangulationAngle = medianTriangulationAngle(edge);
		viewA.connections.add(edge);
		viewB.connections.add(edge);
		graphEdges.add(edge);

		if( verbose )
			System.out.println("Connected "+viewA.index+" -> "+viewB.index);
	}

	double medianTriangulationAngle( CameraMotion edge ) {

		GrowQueue_F64 angles = new GrowQueue_F64(edge.features.size());
		angles.size = edge.features.size();

		for (int i = 0; i < edge.features.size(); i++) {
			AssociatedIndex a = edge.features.get(i);
			Point2D_F64 normA = edge.viewSrc.observationNorm.get( a.src );
			Point2D_F64 normB = edge.viewDst.observationNorm.get( a.dst );

			double acute = triangulationAngle(normA,normB,edge.a_to_b);
			angles.data[i] = acute;
		}

		angles.sort();
		return angles.getFraction(0.5);
	}

	/**
	 * Uses ransac to fit an epipolar model to the associated features. Adds list of matched features to the edge.
	 *
	 * @param matches List of matched features by index
	 * @param pointsA Set of observations from image A
	 * @param pointsB Set of observations from image B
	 * @param ransac Model fitter
	 * @param edge Edge which will contain a description of found motion
	 * @return true if no error
	 */
	boolean fitEpipolar( FastQueue<AssociatedIndex> matches ,
						 List<Point2D_F64> pointsA , List<Point2D_F64> pointsB ,
						 Ransac<?,AssociatedPair> ransac ,
						 CameraMotion edge )
	{
		pairs.resize(matches.size);
		for (int i = 0; i < matches.size; i++) {
			AssociatedIndex a = matches.get(i);
			pairs.get(i).p1.set(pointsA.get(a.src));
			pairs.get(i).p2.set(pointsB.get(a.dst));
		}
		if( !ransac.process(pairs.toList()) )
			return false;
		int N = ransac.getMatchSet().size();
		for (int i = 0; i < N; i++) {
			AssociatedIndex a = matches.get(ransac.getInputIndex(i));
			edge.features.add( a.copy() );
		}
		return true;
	}

	@Override
	public BundleAdjustmentSceneStructure getSceneStructure() {
		return structure;
	}

	@Override
	public BundleAdjustmentObservations getObservations() {
		return observations;
	}

	@Override
	public void reset() {
		stopRequested = false;
		graphNodes.clear();
	}

	@Override
	public void requestStop() {
		stopRequested = true;
	}

	@Override
	public boolean isStopRequested() {
		return stopRequested;
	}

	public ConfigEssential getConfigEssential() {
		return configEssential;
	}

	public ConfigFundamental getConfigFundamental() {
		return configFundamental;
	}

	public ConfigRansac getConfigRansac() {
		return configRansac;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	static class CameraView {
		String camera;
		int index;
		Se3_F64 viewToWorld = new Se3_F64();
		ViewState state = ViewState.UNPROCESSED;

		List<CameraMotion> connections = new ArrayList<>();

		// feature descriptor of all features in this image
		FastQueue<TupleDesc> descriptions;
		// observed location of all features in pixels
		FastQueue<Point2D_F64> observationPixels = new FastQueue<>(Point2D_F64.class, true);
		FastQueue<Point2D_F64> observationNorm = new FastQueue<>(Point2D_F64.class, true);

		// Estimated 3D location for SOME of the features
		Feature3D[] features3D;

		public CameraView(int index, FastQueue<TupleDesc> descriptions ) {
			this.index = index;
			this.descriptions = descriptions;
		}
	}

	enum ViewState {
		UNPROCESSED,
		PENDING,
		PROCESSED
	}

	static class CameraMotion {
		// if the transform of both views is known then this will be scaled to be in world units
		// otherwise it's in arbitrary units
		Se3_F64 a_to_b = new Se3_F64();

		// index
		List<AssociatedIndex> features = new ArrayList<>();

		CameraView viewSrc;
		CameraView viewDst;

		// Average angle of features in this motion for triangulation
		double triangulationAngle;

		/**
		 * Score how well this motion can be used to provide an initial set of triangulated feature points.
		 * More features the better but you want the epipolar estimate to be a better model than homography
		 * since the epipolar includes translation.
		 * @return the score
		 */
		public double scoreTriangulation() {
			return features.size()*triangulationAngle;
		}

		public Se3_F64 motionSrcToDst( CameraView src ) {
			if( src == viewSrc) {
				return a_to_b.copy();
			} else if( src == viewDst){
				return a_to_b.invert(null);
			} else {
				throw new RuntimeException("BUG!");
			}
		}

		public CameraView destination( CameraView src ) {
			if( src == viewSrc) {
				return viewDst;
			} else if( src == viewDst){
				return viewSrc;
			} else {
				throw new RuntimeException("BUG!");
			}
		}
	}

	static class Feature3D {
		// estimate 3D position of the feature in world frame
		Point3D_F64 worldPt = new Point3D_F64();
		// Index of the obsrevation in the corresponding view which the feature is visible in
		GrowQueue_I32 obsIdx = new GrowQueue_I32();
		// List of views this feature is visible in
		List<CameraView> views = new ArrayList<>();
		int mark = -1;
	}
}

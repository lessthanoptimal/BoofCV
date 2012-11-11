package boofcv.alg.tracker.fused;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.associate.EnsureUniqueAssociation;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 *
 * <p>
 * Association between dropped tracks and spawned tracks is not done because how ambiguous matches are
 * handled will depend on the application.  Should matches be forced to be unambiguous?  How are
 * data structures handled to maintain track history?
 * </p>
 *
 * @author Peter Abeles
 */
// TODO instead of enforceUnique assicate current tracks to new ones, discard matches, spawn new stuff
	// compare both techniques performance


// TODO Remove unused stuff
// TODO Two versions.  One for InterestPointDetector and one for corners
public class CombinedPointTracker
		<I extends ImageSingleBand , D extends ImageSingleBand, TD extends TupleDesc> {

	// input image types
	Class<I> imageType;
	Class<D> derivType;

	// current image in sequence
	I input;
	I inputPrevious;

	DetectTrackKLT<I,D> trackerKlt;
	InterestPointDetector<I> detector;
	DescribeRegionPoint<I,TD> describe;

	// association
	GeneralAssociation<TD> associate;

	// make sure that no two points are centered at the same location
	EnforceUniquePoints enforceUnique;

	// all active tracks that have been tracked purely by KLT
	List<CombinedTrack<TD>> tracksPureKlt = new ArrayList<CombinedTrack<TD>>();
	// tracks that were dropped but then reactivated
	List<CombinedTrack<TD>> tracksReactivated = new ArrayList<CombinedTrack<TD>>();
	// recently dropped tracks
	List<CombinedTrack<TD>> tracksDropped = new ArrayList<CombinedTrack<TD>>();
	// recently spawned tracks
	List<CombinedTrack<TD>> tracksSpawned = new ArrayList<CombinedTrack<TD>>();
	// track points whose data is to be reused
	Stack<CombinedTrack<TD>> tracksUnused = new Stack<CombinedTrack<TD>>();

	// list of descriptions that are available for reuse
	Stack<TD> descUnused = new Stack<TD>();

	long totalTracks = 0;

	// The descriptor region should be larger than the circle, otherwise
	// it would just have a featureless region inside
	double regionRadiusScale;

	int descToDropped[] = new int[10000];
	double droppedAssocScore[] = new double[10000];
	int droppedAssocIndex[] = new int[10000];

	public CombinedPointTracker(DetectTrackKLT<I, D> trackerKlt,
								InterestPointDetector<I> detector,
								DescribeRegionPoint<I, TD> describe,
								GeneralAssociation<TD> associate,
								double regionRadiusScale,
								Class<I> imageType, Class<D> derivType) {

		if( describe != null ) {
			if( describe.requiresOrientation() && !detector.hasOrientation() )
				throw new IllegalArgumentException("Descriptor requires orientation");
			if( describe.requiresScale() && !detector.hasScale() )
				throw new IllegalArgumentException("Descriptor requires scale");
		}

		this.imageType = imageType;
		this.derivType = derivType;

		this.trackerKlt = trackerKlt;
		this.detector = detector;
		this.describe = describe;
		this.regionRadiusScale = regionRadiusScale;

		enforceUnique = new EnforceUniquePoints(trackerKlt.featureRadius);

		this.associate = associate;
		this.inputPrevious = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	public void reset() {
		tracksPureKlt.clear();
		tracksReactivated.clear();
		tracksDropped.clear();
		tracksSpawned.clear();
		tracksUnused.clear();
	}

	// TODO is this needed?
	public void maintenance() {
		inputPrevious.reshape(input.width, input.height);
		inputPrevious.setTo(input);
	}

	public void recycleDroppedTracks() {
		for(CombinedTrack info : tracksDropped ) {
			descUnused.addAll(info.desc);
			info.desc.clear();
		}

		// recycle dropped tracks here
		tracksUnused.addAll(tracksDropped);
		tracksDropped.clear();
	}

	public void track( I input ,
					   PyramidDiscrete<I> pyramid ,
					   PyramidDiscrete<D> derivX,
					   PyramidDiscrete<D> derivY ) {
		// forget recently dropped or spawned tracks
		tracksSpawned.clear();

		// save references
		this.input = input;

		trackerKlt.setInputs(pyramid,derivX,derivY);

		trackList(tracksPureKlt);
		trackList(tracksReactivated);
	}

	private void trackList( List<CombinedTrack<TD>> tracks ) {
		for( int i = 0; i < tracks.size();  ) {
			CombinedTrack track = tracks.get(i);

			if( !trackerKlt.performTracking(track.track) ) {
				// handle the dropped track
				tracks.remove(i);
				tracksDropped.add(track);
			} else {
				track.set(track.track.x,track.track.y);
				i++;
			}
		}
	}

	/**
	 * Selects new interest points in the image.
	 */
	public void detectInterestPoints() {
		// initialize
		if( enforceUnique != null )
			enforceUnique.setInputSize(input.width,input.height);

		// detect new interest points
		detector.detect(input);
	}

	/**
	 * From the found interest points create new tracks.  Tracks are only created at points
	 * where there are no existing tracks.
	 */
	public void spawnTracksFromPoints() {
		if( describe != null )
			describe.setImage(input);


		// only add tracks which are not the same as existing tracks and avoid
		// new tracks which are too close to each other
		enforceUnique.process(detector, (List)tracksPureKlt); // todo add other tracks

		// add the new tracks to the tracker
		for( Point2D_F64 w : enforceUnique.getFound().toList() ) {
			CombinedTrack<TD> track;

			if( tracksUnused.size() > 0 ) {
				track = tracksUnused.pop();
			} else {
				track = new CombinedTrack();
			}

			if( describe != null ) {
				// create a region description at each scale
				List<EnforceUniquePoints.Info> l = enforceUnique.getFeatureInfo((int) w.x, (int) w.y);
				for( EnforceUniquePoints.Info i : l ) {
					TD desc = descUnused.isEmpty() ? null : descUnused.pop();

					desc = describe.process(w.x,w.y,i.orientation,i.scale,desc);
					track.desc.add(desc);
				}
			}

			// create the descriptor for tracking
			track.track = trackerKlt.setDescription((float)w.x,(float)w.y,track.track);
			// set track ID and location
			track.id = totalTracks++;
			track.set(w);

			// update list of active tracks
			tracksPureKlt.add(track);
			tracksSpawned.add(track);
		}
	}

	public void associateTaintedToPoints() {
		List<CombinedTrack<TD>> all = new ArrayList<CombinedTrack<TD>>();
		all.addAll(tracksReactivated);
		all.addAll(tracksDropped);
		all.addAll(tracksPureKlt);

		int num = tracksReactivated.size() + tracksDropped.size();

		tracksReactivated.clear();
		tracksDropped.clear();

		describe.setImage(input);

		FastQueue<TD> detectedDesc = new TupleDescQueue<TD>(describe,false);
		FastQueue<TD> droppedDesc = new TupleDescQueue<TD>(describe,false);

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {

			Point2D_F64 p = detector.getLocation(i);
			double scale = detector.getScale(i);
			double orientation = detector.getOrientation(i);

			if( !describe.isInBounds(p.x,p.y,orientation,scale)) {
				throw new RuntimeException("Must be in bounds, or rewrite this section");
			}
			TD desc = describe.process(p.x,p.y,orientation,scale,null);
			detectedDesc.add(desc);
		}

		int index = 0;
		for( int i = 0; i < all.size(); i++ ) {
			CombinedTrack<TD> info = all.get(i);
			List<TD> l = info.desc;

			for( TD t : l )  {
				droppedDesc.add( t );
				descToDropped[index++] = i;
			}
		}

		// check for duplicates
		associate.associate(droppedDesc,detectedDesc);

		FastQueue<AssociatedIndex> matches = associate.getMatches();

		// Check for multiple matches and select the match with the best score
		for( int i = 0; i < num; i++ ) {
			droppedAssocScore[i] = Double.MAX_VALUE;
		}

		for( AssociatedIndex a : matches.toList() ) {
			double s = droppedAssocScore[descToDropped[a.src]];
			if( s > a.fitScore ) {
				droppedAssocScore[descToDropped[a.src]] = a.fitScore;
				droppedAssocIndex[descToDropped[a.src]] = a.dst;
			}
		}

		// associate matches
		for( int i = 0; i < num; i++ ) {
			CombinedTrack<TD> t = all.get( i );

			if( droppedAssocScore[i] != Double.MAX_VALUE ) {
				int indexSrc = droppedAssocIndex[i];
				Point2D_F64 p = detector.getLocation(indexSrc);

				t.set(p);

				trackerKlt.setDescription((float)p.x,(float)p.y,t.track);
				tracksReactivated.add(t);
			} else {
				tracksDropped.add(t);
			}
		}
//		System.out.println("  num assoc "+matches.size+"  reactive "+tracksReactivated.size()+" dropped "+tracksDropped.size());
	}

	/**
	 * TODO comment
	 */
	public void discardSpawned() {
		for( CombinedTrack<TD> t : tracksSpawned )
			tracksPureKlt.remove(t);
		tracksUnused.addAll(tracksSpawned);
	}

	public List<CombinedTrack<TD>> getSpawned() {
		return tracksSpawned;
	}

	public List<CombinedTrack<TD>> getPureKlt() {
		return tracksPureKlt;
	}

	public List<CombinedTrack<TD>> getReactivated() {
		return tracksReactivated;
	}

	public List<CombinedTrack<TD>> getDropped() {
		return tracksDropped;
	}

	public Class<I> getImageType() {
		return imageType;
	}

	public Class<D> getDerivType() {
		return derivType;
	}

	public DetectTrackKLT<I, D> getTrackerKlt() {
		return trackerKlt;
	}
}

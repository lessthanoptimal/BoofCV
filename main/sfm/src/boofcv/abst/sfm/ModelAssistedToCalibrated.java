package boofcv.abst.sfm;

import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.struct.image.ImageBase;

import java.util.List;

/**
 * Abstract class which handles all the wrapped function for converting a {@link ModelAssistedTracker} into
 * a {@link ModelAssistedTrackerCalibrated}.
 *
 * @author Peter Abeles
 */
public abstract class ModelAssistedToCalibrated<T extends ImageBase,Model,Info>
	implements ModelAssistedTrackerCalibrated<T,Model,Info>
{
	ModelAssistedTracker<T,Model,Info> tracker;

	protected ModelAssistedToCalibrated(ModelAssistedTracker<T, Model, Info> tracker) {
		this.tracker = tracker;
	}

	@Override
	public void setTrackGeometry(TrackGeometryManager<Model, Info> manager) {
		tracker.setTrackGeometry(manager);
	}

	@Override
	public boolean foundModel() {
		return tracker.foundModel();
	}

	@Override
	public Model getModel() {
		return tracker.getModel();
	}

	@Override
	public List<Info> getMatchSet() {
		return tracker.getMatchSet();
	}

	@Override
	public int convertMatchToActiveIndex(int matchIndex) {
		return tracker.convertMatchToActiveIndex(matchIndex);
	}

	@Override
	public void reset() {
		tracker.reset();
	}

	@Override
	public void process(T image) {
		tracker.process(image);
	}

	@Override
	public void spawnTracks() {
		tracker.spawnTracks();
	}

	@Override
	public void dropAllTracks() {
		tracker.dropAllTracks();
	}

	@Override
	public void dropTrack(PointTrack track) {
		tracker.dropTrack(track);
	}

	@Override
	public List<PointTrack> getAllTracks(List<PointTrack> list) {
		return tracker.getAllTracks(list);
	}

	@Override
	public List<PointTrack> getActiveTracks(List<PointTrack> list) {
		return tracker.getActiveTracks(list);
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		return tracker.getInactiveTracks(list);
	}

	@Override
	public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
		return tracker.getDroppedTracks(list);
	}

	@Override
	public List<PointTrack> getNewTracks(List<PointTrack> list) {
		return tracker.getNewTracks(list);
	}
}

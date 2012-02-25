package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class SimulatedTracker <T extends ImageBase>
		implements ImagePointTracker<T>
{
	// the simulated world
	SimulatePointWorld3D world;
	// camera position
	Se3_F64 cameraPose;

	// the camera's FOV
	double fov;

	// camera calibration matrix
	DenseMatrix64F K;

	// amount of pixel noise that's added
	double pixelNoise;

	@Override
	public void process(T image) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean addTrack(double x, double y) {
		throw new RuntimeException("Add track not supported");
	}

	@Override
	public void spawnTracks() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void dropTracks() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void dropTrack(PointTrack track) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<PointTrack> getActiveTracks() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<PointTrack> getDroppedTracks() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<PointTrack> getNewTracks() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}

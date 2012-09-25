package boofcv.abst.sfm;

import boofcv.alg.sfm.AccessSfmPointTracks;
import boofcv.alg.sfm.StereoSimpleVo;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapStereoSimpleVo<T extends ImageSingleBand>
		implements StereoVisualOdometry<T>, AccessSfmPointTracks

{
	StereoSimpleVo<T> alg;

	Se3_F64 c2w = new Se3_F64();

	public WrapStereoSimpleVo(StereoSimpleVo<T> alg) {
		this.alg = alg;
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		return alg.process(leftImage,rightImage);
	}

	@Override
	public void reset() {
		//Todo implement
	}

	@Override
	public boolean isFatal() {
		return alg.hadFault();
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		Se3_F64 w2c = alg.getWorldToCamera();
		w2c.invert(c2w);
		return c2w;
	}

	@Override
	public Point3D_F64 getTrackLocation(int index) {
		return alg.getTracker().getPairs().get(index).getLocation();
	}

	@Override
	public long getTrackId(int index) {
		return alg.getTracker().getPairs().get(index).getTrackID();
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		return (List)alg.getTracker().getActiveTracks();
	}

	@Override
	public List<Point2D_F64> getInlierTracks() {
		List<StereoSimpleVo.PointPoseD> t = (List)alg.getComputeMotion().getMatchSet();

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();
		for( StereoSimpleVo.PointPoseD p : t ) {
			ret.add(p.currentPixel);
		}

//		List<PointPoseTrack> tracks = alg.getTriangulated();
//
//		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();
//		for( PointPoseTrack t : tracks ) {
//			ret.add( t.getPixel().currLoc );
//		}

		return ret;
	}

	@Override
	public int fromInlierToAllIndex(int inlierIndex) {
		return alg.getComputeMotion().getInputIndex(inlierIndex);
	}

	@Override
	public List<Point2D_F64> getNewTracks() {
		return (List)alg.getTracker().getTracker().getNewTracks();
	}
}

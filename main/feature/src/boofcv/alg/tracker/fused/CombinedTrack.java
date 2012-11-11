package boofcv.alg.tracker.fused;

import boofcv.alg.tracker.pklt.PyramidKltFeature;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CombinedTrack<TD extends TupleDesc> extends Point2D_F64 {
	public PyramidKltFeature track;
	public List<TD> desc = new ArrayList<TD>();
	long id;

	Object cookie;

	public <T>T getCookie() {
		return (T)cookie;
	}

	public void setCookie(Object cookie) {
		this.cookie = cookie;
	}
}

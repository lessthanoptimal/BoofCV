package boofcv.struct;

/**
 * Simple data structure for passing a pair of data.
 *
 * @author Peter Abeles
 */
public class Tuple2<A,B> {
	public A data0;
	public B data1;

	public Tuple2(A data0, B data1) {
		this.data0 = data0;
		this.data1 = data1;
	}

	public Tuple2() {
	}

	public A getData0() {
		return data0;
	}

	public void setData0(A data0) {
		this.data0 = data0;
	}

	public B getData1() {
		return data1;
	}

	public void setData1(B data1) {
		this.data1 = data1;
	}
}


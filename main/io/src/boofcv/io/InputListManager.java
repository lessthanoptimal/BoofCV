package boofcv.io;

import java.util.List;


/**
 * @author Peter Abeles
 */
public interface InputListManager {
	public int size();

	public List<String> getLabels();

	public String getLabel( int index );
}

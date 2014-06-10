package boofcv.processing;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified point feature association
 *
 * @see boofcv.abst.feature.associate.AssociateDescription
 *
 * @author Peter Abeles
 */
public class SimpleAssociateDescription< Desc extends TupleDesc > {

	AssociateDescription<Desc> associate;
	FastQueue<Desc> queueSrc = new FastQueue(TupleDesc.class,false);
	FastQueue<Desc> queueDst = new FastQueue(TupleDesc.class,false);

	public SimpleAssociateDescription(AssociateDescription<Desc> associate) {
		this.associate = associate;
	}

	public void associate( List<Desc> src , List<Desc> dst ) {
		// convert the list format
		queueSrc.reset(); queueDst.reset();

		for( Desc d : src ) {
			queueSrc.add(d);
		}
		for( Desc d : dst ) {
			queueDst.add(d);
		}

		associate.setSource(queueSrc);
		associate.setDestination(queueDst);
		associate.associate();

	}

	public List<AssociatedIndex> getMatches() {
		List<AssociatedIndex> ret = new ArrayList<AssociatedIndex>();
		ret.addAll(associate.getMatches().toList());
		return ret;
	}

	public AssociateDescription<Desc> getAssociateDescription() {
		return associate;
	}
}

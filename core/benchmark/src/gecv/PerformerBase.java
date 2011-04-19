package gecv;


/**
 * Optional base class for performers
 *
 * @author Peter Abeles
 */
public abstract class PerformerBase implements Performer {
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}

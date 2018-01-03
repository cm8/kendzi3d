package kendzi.josm.kendzi3d.data.event;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;

import kendzi.josm.kendzi3d.ui.Resumer;

public abstract class DataEvent implements Resumer {

    private final AbstractDatasetChangedEvent e;

    private Resumable resumable = () -> {
    };

    public DataEvent() {
        this.e = null;
    }

    public DataEvent(AbstractDatasetChangedEvent e) {
        this.e = e;
    }

    public AbstractDatasetChangedEvent getJosmEvent() {
        return e;
    }

    public Collection<OsmPrimitive> getJosmData() {
        return null;
    }

    @Override
    public void resumeResumable() {
        resumable.resume();
    }

    @Override
    public void setResumable(Resumable r) {
        resumable = r;
    }
}

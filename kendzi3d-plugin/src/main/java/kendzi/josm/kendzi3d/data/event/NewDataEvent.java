package kendzi.josm.kendzi3d.data.event;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.gui.MainApplication;

public class NewDataEvent extends DataEvent {

    public NewDataEvent() {
        super();
    }

    public NewDataEvent(AbstractDatasetChangedEvent e) {
        super(e);
    }

    @Override
    public Collection<OsmPrimitive> getJosmData() {
        // try to use latest data, this is called when the event is processed, not when generated
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        return (ds != null) ? ds.allNonDeletedCompletePrimitives() : Collections.<OsmPrimitive>emptySet();
    }
}

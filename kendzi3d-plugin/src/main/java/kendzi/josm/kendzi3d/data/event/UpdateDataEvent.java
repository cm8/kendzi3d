package kendzi.josm.kendzi3d.data.event;

import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;

public class UpdateDataEvent extends DataEvent {

    public UpdateDataEvent() {
        super();
    }

    public UpdateDataEvent(AbstractDatasetChangedEvent e) {
        super(e);
    }

    @Override
    public Collection<OsmPrimitive> getJosmData() {
        final Collection<OsmPrimitive> ret = new ArrayList<>();

        AbstractDatasetChangedEvent e = getJosmEvent();
        ret.addAll(e.getPrimitives());

        // TODO: this is expensive, tame it down
        if (e instanceof NodeMovedEvent) {
            e.getPrimitives()
            .forEach(p -> p.getReferrers()
                    .forEach(w -> { ret.add(w); ret.addAll(w.getReferrers()); }));

        } else if (e instanceof RelationMembersChangedEvent) {
            e.getPrimitives().forEach(p -> ret.addAll(p.getReferrers()));
        }

        return ret;
    }
}

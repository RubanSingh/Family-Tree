package dna.familytree.visitor;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every media of the tree (shared media records and simple media) together with its container and leader object.
 * Unlike {@link FindStack} and {@link MediaContainers} it has no side effects: nothing is modified, no Memory stack is created.
 */
public class MediaLeaders extends TotalVisitor {

    public static class Entry {
        public final Media media;
        /**
         * The record or sub-record that contains a simple media, null for a shared media record.
         */
        public final MediaContainer container;
        /**
         * The top-level object to update the change date of: the shared media itself, or the record containing the simple media.
         */
        public final Object leader;

        public Entry(Media media, MediaContainer container, Object leader) {
            this.media = media;
            this.container = container;
            this.leader = leader;
        }
    }

    public final List<Entry> list = new ArrayList<>();
    private Object leader;

    public MediaLeaders(Gedcom gedcom) {
        gedcom.accept(this);
    }

    @Override
    boolean visit(Object object, boolean isLeader) {
        if (isLeader)
            leader = object;
        if (object instanceof Media && ((Media)object).getId() != null)
            list.add(new Entry((Media)object, null, object));
        if (object instanceof MediaContainer) {
            MediaContainer container = (MediaContainer)object;
            for (Media media : container.getMedia())
                list.add(new Entry(media, container, leader));
        }
        return true;
    }
}

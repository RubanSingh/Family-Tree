package dna.familytree.feature.media;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.MediaRef;
import org.folg.gedcom.parser.JsonParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dna.familytree.visitor.MediaContainers;
import dna.familytree.visitor.MediaLeaders;
import dna.familytree.visitor.MediaReferences;

/**
 * Conversion between simple media (inside one record) and shared media (a record linked by many), adapted from Family Gem.
 * Only GEDCOM objects are touched: the caller saves the tree with the returned leader objects.
 */
public final class MediaConversions {

    private MediaConversions() {
    }

    /**
     * Turns a simple media into a shared media record with the given ID.
     * Other simple media with exactly the same content (same file, title, notes, flags...) become links to it as well,
     * so nothing changes in how any record looks.
     *
     * @return The modified leader objects, plus the new shared media
     */
    public static Object[] makeShared(Gedcom gedcom, Media media, String newId) {
        String content = contentOf(media);
        Set<Object> leaders = new LinkedHashSet<>();
        for (MediaLeaders.Entry entry : new MediaLeaders(gedcom).list) {
            if (entry.container == null) continue; // Shared media
            if (entry.media != media && (!Objects.equals(entry.media.getFile(), media.getFile()) // Quick check first
                    || !content.equals(contentOf(entry.media)))) continue;
            MediaContainer container = entry.container;
            container.getMedia().remove(entry.media);
            if (container.getMedia().isEmpty())
                container.setMedia(null);
            boolean linked = false;
            for (MediaRef ref : container.getMediaRefs())
                if (newId.equals(ref.getRef())) linked = true;
            if (!linked) { // Two identical media in the same record become one link
                MediaRef mediaRef = new MediaRef();
                mediaRef.setRef(newId);
                container.addMediaRef(mediaRef);
            }
            leaders.add(entry.leader);
        }
        media.setId(newId);
        gedcom.addMedia(media);
        gedcom.createIndexes();
        leaders.add(media);
        return leaders.toArray();
    }

    /**
     * Turns a shared media record into a simple media inside each record linking it.
     * The first record gets the media itself, the others an identical copy.
     *
     * @return The modified leader objects, empty if the media is not linked by any record (nothing is changed then)
     */
    public static Object[] makeSimple(Gedcom gedcom, Media media) {
        String id = media.getId();
        if (id == null) return new Object[0];
        MediaReferences references = new MediaReferences(gedcom, media, false);
        if (references.num == 0) return new Object[0];
        // Containers linking the media; the ref is "renamed" to the same ID, so nothing changes
        List<MediaContainer> containers = new ArrayList<>(new MediaContainers(gedcom, media, id).containers);
        // Copies are made before the media loses its ID
        List<Media> copies = new ArrayList<>();
        for (int i = 1; i < containers.size(); i++) {
            Media copy = copyOf(media);
            copy.setId(null);
            copy.setChange(null);
            copies.add(copy);
        }
        gedcom.getMedia().remove(media);
        media.setId(null);
        media.setChange(null);
        for (int i = 0; i < containers.size(); i++) {
            MediaContainer container = containers.get(i);
            Iterator<MediaRef> refs = container.getMediaRefs().iterator();
            while (refs.hasNext())
                if (id.equals(refs.next().getRef()))
                    refs.remove();
            if (container.getMediaRefs().isEmpty())
                container.setMediaRefs(null);
            container.addMedia(i == 0 ? media : copies.get(i - 1));
        }
        gedcom.createIndexes();
        return references.leaders.toArray();
    }

    /**
     * A shared media that is linked by at least one record can be made simple.
     */
    public static boolean canMakeSimple(Gedcom gedcom, Media media) {
        return media.getId() != null && new MediaReferences(gedcom, media, false).num > 0;
    }

    /**
     * Deep copy made with the same JSON serialization used to save the tree, so extensions are kept as they are.
     */
    static Media copyOf(Media media) {
        Gedcom wrapper = new Gedcom();
        wrapper.addMedia(media);
        JsonParser parser = new JsonParser();
        return parser.fromJson(parser.toJson(wrapper)).getMedia().get(0);
    }

    /**
     * Everything saved for a media apart from its ID.
     */
    static String contentOf(Media media) {
        String id = media.getId();
        media.setId(null);
        try {
            Gedcom wrapper = new Gedcom();
            wrapper.addMedia(media);
            return new JsonParser().toJson(wrapper);
        } finally {
            media.setId(id);
        }
    }
}

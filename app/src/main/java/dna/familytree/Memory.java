package dna.familytree;

import androidx.annotation.NonNull;

import org.folg.gedcom.model.Address;
import org.folg.gedcom.model.Change;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.RepositoryRef;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.Submitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import dna.familytree.detail.AddressController;
import dna.familytree.detail.SubmitterController;
import dna.familytree.detail.ChangeController;
import dna.familytree.detail.EventController;
import dna.familytree.detail.ExtensionController;
import dna.familytree.detail.FamilyController;
import dna.familytree.detail.MediaController;
import dna.familytree.detail.NameController;
import dna.familytree.detail.NoteController;
import dna.familytree.detail.RepositoryController;
import dna.familytree.detail.RepositoryRefController;
import dna.familytree.detail.SourceController;
import dna.familytree.detail.SourceCitationController;
import dna.familytree.visitor.FindStack;

/**
 * Manager of stacks of hierarchical objects, mainly to display the breadcrumbs in DetailActivity.
 */
public class Memory {

    static Map<Class, Class> classes = new HashMap<>();
    private static final Memory memory = new Memory();
    List<StepStack> list = new ArrayList<>();

    Memory() {
        classes.put(Person.class, ProfileController.class);
        classes.put(Repository.class, RepositoryController.class);
        classes.put(RepositoryRef.class, RepositoryRefController.class);
        classes.put(Submitter.class, SubmitterController.class);
        classes.put(Change.class, ChangeController.class);
        classes.put(SourceCitation.class, SourceCitationController.class);
        classes.put(GedcomTag.class, ExtensionController.class);
        classes.put(EventFact.class, EventController.class);
        classes.put(Family.class, FamilyController.class);
        classes.put(Source.class, SourceController.class);
        classes.put(Media.class, MediaController.class);
        classes.put(Address.class, AddressController.class);
        classes.put(Name.class, NameController.class);
        classes.put(Note.class, NoteController.class);
    }

    /**
     * Returns the last stack of the list, if there is at least one.
     * Otherwise returns an empty stack just to not return null.
     */
    static StepStack getLastStack() {
        if (memory.list.size() > 0)
            return memory.list.get(memory.list.size() - 1);
        else
            return new StepStack(); // An empty stack that is not added to the list
    }

    /**
     * Adds a new stack to the list and returns it.
     */
    public static StepStack addStack() {
        StepStack stepStack = new StepStack();
        memory.list.add(stepStack);
        return stepStack;
    }

    /**
     * Adds an object in the first position of a new stack.
     */
    public static void setLeader(Object object) {
        setLeader(object, null);
    }

    public static void setLeader(Object object, String tag) {
        addStack();
        Step step = add(object);
        if (tag != null)
            step.tag = tag;
        else if (object instanceof Person)
            step.tag = "INDI";
        //log("setLeader");
    }

    // Aggiunge un object alla fine dell'ultima pila esistente.
    public static Step add(Object object) {
        Step step = new Step();
        step.object = object;
        getLastStack().add(step);
        //log("add");
        return step;
    }

    /**
     * Puts an object in a first step if there are no stacks, or replaces the first step in the last existing stack.
     * In other words, puts the leader object without adding any more stacks.
     */
    public static void replaceLeader(Object object) {
        String tag = object instanceof Family ? "FAM" : "INDI";
        if (memory.list.size() == 0) {
            setLeader(object, tag);
        } else {
            getLastStack().clear();
            Step step = add(object);
            step.tag = tag;
        }
        //log("replaceLeader");
    }

    /**
     * Returns the object of the first step of the last stack.
     */
    public static Object getLeaderObject() {
        if (getLastStack().size() > 0)
            return getLastStack().firstElement().object;
        else
            return null;
    }

    /**
     * If the stack has more than one object, gets the second to last object, otherwise returns null.
     */
    public static Object getSecondToLastObject() { // TODO: getPreviousObject()? getPenultimateObject()?
        StepStack stepStack = getLastStack();
        if (stepStack.size() > 1)
            return stepStack.get(stepStack.size() - 2).object;
        else
            return null;
    }

    /**
     * Returns the object in the last step of the last stack.
     */
    public static Object getLastObject() {
        if (getLastStack().size() == 0)
            return null;
        else
            return getLastStack().peek().object;
    }

    /**
     * Removes one or maybe more steps at the end of the stacks.
     * Represents "one step back", called after onBackPressed().
     */
    public static void stepBack() {
        while (getLastStack().size() > 0 && getLastStack().lastElement().deleteOnBackPressed)
            getLastStack().pop();
        if (getLastStack().size() > 0)
            getLastStack().pop();
        if (getLastStack().isEmpty())
            memory.list.remove(getLastStack());
        //log("stepBack");
    }

    /**
     * Makes an object null in all steps of all stacks.
     * The objects in any subsequent step are also annulled.
     */
    public static void setInstanceAndAllSubsequentToNull(Object object) {
        for (StepStack stepStack : memory.list) {
            boolean following = false;
            for (Step step : stepStack) {
                if (step.object != null && (step.object.equals(object) || following)) {
                    step.object = null;
                    following = true;
                }
            }
        }
    }

    static class StepStack extends Stack<Step> {
        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Step step : this) {
                builder.append(step);
            }
            return builder.toString();
        }
    }

    public static class Step {
        public Object object;
        public String tag;
        /**
         * {@link FindStack} sets it to true then onBackPressed() the step will be deleted.
         */
        public boolean deleteOnBackPressed;

        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (deleteOnBackPressed) builder.append("< ");
            if (tag != null)
                builder.append(tag).append(" ");
            else if (object != null)
                builder.append(object.getClass().getSimpleName()).append(" ");
            else
                builder.append("null "); // This should never happen
            return builder.toString();
        }
    }
}

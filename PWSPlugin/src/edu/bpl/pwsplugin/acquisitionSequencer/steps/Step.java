/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.UI.tree.CopyableMutableTreeNode;
import edu.bpl.pwsplugin.utils.GsonUtils;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.swing.tree.TreeNode;

/**
 *
 * @author nick
 */
public abstract class Step<T extends JsonableParam> extends CopyableMutableTreeNode {
    protected T settings; 
    private final SequencerConsts.Type stepType;
    protected final List<SequencerFunction> callbacks = new ArrayList<>();
    private static final AtomicInteger counter = new AtomicInteger(); //This static counter makes sure that each Step object has it's own uid during runtime.
    private Integer uid = counter.getAndIncrement();

    
    public Step(T settings, SequencerConsts.Type type) {
        super();
        this.stepType = type;
        this.setSettings(settings);
    }
    
    
    public Step(Step step) { //copy constructor
        this((T) step.settings.copy(), step.stepType);        
    }
    
    public Integer getID() { return this.uid; }
        
    public final SequencerConsts.Type getType() {
        return stepType;
    }

    @Override
    public Step copy() { //Use json to safely copy the object
        Gson gson = GsonUtils.getGson();
        return (Step) gson.fromJson(gson.toJson(this), this.getClass());
    }
    
    public final T getSettings() { return (T) settings.copy(); }
    
    public final void setSettings(T settings) { this.settings = settings; }
    
    protected abstract SequencerFunction getStepFunction(); //Return  function to run for this step during execution. Does not include callbacks and mandatory changes to the status object which are handled automatically by `getFunction`. This should initialize any variables that are used for context during runtime.
          
    protected abstract SimFn getSimulatedFunction(); //return a function that simulates how folder usage and cell number changes through the run.
    
    protected static class SimulatedStatus {
        public Integer cellNum = 1;
        public List<String> requiredPaths = new ArrayList<>();
        public String workingDirectory = "";
    }
    
    @FunctionalInterface
    protected static interface SimFn extends Function<SimulatedStatus, SimulatedStatus> {} 
    
    public final SequencerFunction getFunction() {
        SequencerFunction stepFunc = this.getStepFunction();
        TreeNode[] path = this.getPath();
        Step[] treePath = Arrays.copyOf(path, path.length, Step[].class); //cast to Step[].
        return new SequencerFunction() {
            @Override
            public AcquisitionStatus applyThrows(AcquisitionStatus status) throws Exception {
                //Update the status object with information about the current step.
                Step[] origPath = status.getTreePath();
                status.setTreePath(treePath);
                //Run any callbacks that have been set for this step.
                for (SequencerFunction func : callbacks) {
                    status = func.apply(status);
                } 
                //Run the function for this step
                status = stepFunc.apply(status);
                status.setTreePath(origPath); //Set the path back to where it was
                return status;
            }
        };
    }
    
    public final void addCallback(SequencerFunction cb) { 
        callbacks.add(cb);
    }
        
    @Override
    public String toString() { //this determines how its labeled in a JTree
        return SequencerConsts.getFactory(this.getType()).getName();
    }
    
    public abstract List<String> validate(); //Return a list of any errors for this step.
    
    public static void registerGsonType() { //This must be called for GSON loading/saving to work.
        GsonUtils.registerType(StepTypeAdapter.FACTORY);
    }
    
    public void saveToJson(String savePath) throws IOException {
        if(!savePath.endsWith(".pwsseq")) {
            savePath = savePath + ".pwsseq"; //Make sure the extension is there.
        }
        try (FileWriter writer = new FileWriter(savePath)) { //Writer is automatically closed at the end of this statement.
            Gson gson = GsonUtils.getGson();
            String json = gson.toJson(this);
            writer.write(json);
        }
    }
}

class StepTypeAdapter extends TypeAdapter<Step> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (Step.class.isAssignableFrom(type.getRawType())) { //Allow subtypes to use this factory.
                return (TypeAdapter<T>) new StepTypeAdapter(gson);
            }
            return null;
        }
    };

    private final Gson gson;

    private StepTypeAdapter(Gson gson) {
        this.gson = gson;
    }
    
    @Override
    public void write(JsonWriter out, Step step) throws IOException {
        out.beginObject();
        out.name("id");
        out.value(step.getID());
        out.name("stepType");
        out.value(step.getType().name());
        out.name("settings");
        gson.toJson(step.getSettings(), SequencerConsts.getFactory(step.getType()).getSettings(), out);
        if (step.getAllowsChildren()) {
            out.name("children");
            gson.toJson(Collections.list(step.children()), List.class, out); // recursion!
        }
        // No need to write node.getParent(), it would lead to infinite recursion.
        out.endObject();
    }

    @Override
    public Step read(JsonReader in) throws IOException {
        try {
            in.beginObject();
            if (!in.nextName().equals("id")) { throw new RuntimeException(); } //ID is determined at runtime don't load it.
            int id = in.nextInt(); //read the id to get rid of it.
            if (!in.nextName().equals("stepType")) { throw new RuntimeException(); } //This must be "stepType" 
            SequencerConsts.Type stepType = SequencerConsts.Type.valueOf(in.nextString());
            Step step = SequencerConsts.getFactory(stepType).getStep().newInstance();
            if (!in.nextName().equals("settings")) { throw new RuntimeException(); }
            JsonableParam settings = gson.fromJson(in, SequencerConsts.getFactory(stepType).getSettings());
            step.setSettings(settings);
            if (step.getAllowsChildren()) {
                if (!in.nextName().equals("children")) { throw new RuntimeException(); }
                in.beginArray();
                while (in.hasNext()) {
                    step.add(read(in)); // recursion! this did also set the parent of the child-node
                }
                in.endArray();
            }
            in.endObject();
            return step;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
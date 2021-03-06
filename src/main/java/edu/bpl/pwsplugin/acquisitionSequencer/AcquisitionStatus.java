/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer;

import edu.bpl.pwsplugin.Globals;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class AcquisitionStatus {
    //This object acts as a go-between between the UI and the acquisition thread.
    private String currentPath;
    protected Integer currentCellNum; //The folder number we are currently acquiring.
    private List<String> statusMsg = new ArrayList<>(); //A string describing what is currently happening.
    private final Function<AcquisitionStatus, Void> publishCallBack; //This callback should link to the `publish` method of the swingworker running the acquisition thread.
    private final Function<Void, Void> pauseCallBack; // This callback should link to the `pausepoint` method of a pause button.
    private final SequencerCoordinate coords = new SequencerCoordinate(); //This keeps track of where in the sequence we are. Callbacks can use this to determine where they are being called from.
    
    public AcquisitionStatus(Function<AcquisitionStatus, Void> publishCallBack, Function<Void, Void> pauseCallBack) {
        //Create a new status object 
        this.publishCallBack = publishCallBack;
        this.pauseCallBack = pauseCallBack;
    }
    
    public AcquisitionStatus(AcquisitionStatus status) { //This isn't used and maybe thats a good thing, maybe a single sequence should just have a single status object that is completely mutable.
        //Copy an existing status object to a new object, avoids issues with this being a mutable object.
        currentPath = status.currentPath;
        currentCellNum = status.currentCellNum;
        statusMsg = status.statusMsg;
        publishCallBack = status.publishCallBack;
        pauseCallBack = status.pauseCallBack;
    }
    
    private void publish() {
        //Send a copy of this object back to the swingworker so it can be accessed from the `process` method. 
        //We really only have one instance of this class which is not really how the publish/process mechanism is designed, but it still works as a convenient way to make UI events happen in response to calling this publish method.
        publishCallBack.apply(this);
    }
    
    public synchronized Integer newStatusMessage(String message) {
        Integer indentation = this.coords.getTreePath().length - 2; //The length of the treepath controls the indentation of messages for more readable log. The rootstep doesn't log anything so a 2 length treepath should have no indentation.
        String indent = StringUtils.repeat("  ", indentation);
        this.statusMsg.add(indent + message);
        Globals.logger().logSequence(message);
        this.publish();
        return this.statusMsg.size()-1; //This can be used as a pointer to update the message later.
    }
    
    public synchronized void updateStatusMessage(Integer idx, String msg) { //Idx is the number that was returned by `newStatusMessage`
        //Find the original indentation so we can replicate it.
        String oldMsg = this.statusMsg.get(idx);
        int index = oldMsg.indexOf(oldMsg.trim());
        String indent = StringUtils.repeat(" ", index);
        this.statusMsg.set(idx, indent + msg);
        this.publish();
    }
    
    public synchronized  List<String> getStatusMessage() {
        return this.statusMsg;
    }
    
    public void allowPauseHere() {
        pauseCallBack.apply(null); //If the pause button was armed then block this thread until it is disarmed.
    }
    
    public synchronized String getSavePath() {
       return currentPath; 
    }
    
    public synchronized void setSavePath(String path) {
        Globals.acqManager().setSavePath(path);
        currentPath = path;
    }
    
    public synchronized void setCellNum(Integer num) {
        currentCellNum = num;
        Globals.acqManager().setCellNum(num);
        this.publish();
    }
    
    public synchronized Integer getCellNum() {
        return currentCellNum;
    }
    
    /*public synchronized List<Step> getTreePath() {
        return coords.getTreePath();
    }
    
    //Coordinate tracking methods. It is important that each step calls these to keep track of where we are in the sequence, this is mostly handled by the base classes.
    public synchronized void moveDownTree(Step step) {
        this.coords.moveDownTree(step);
    }
    
    public synchronized void moveUpTree() {
        this.coords.moveUpTree();
    }
    
    public synchronized void incrementIteration() {
        this.coords.incrementIteration();
    }
    
    public JsonObject getCoordsAsJson() {
        return this.coords.toJson();
    }*/
    
    public synchronized SequencerCoordinate coords() {
        return this.coords;
    }
}

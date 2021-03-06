/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author nick
 */
public abstract class EndpointStep<T extends JsonableParam> extends Step<T> {
    //A `Step` which is an endpoint (does not support containing any substeps
    public EndpointStep(T settings, SequencerConsts.Type type) {
        super(settings, type);
        this.setAllowsChildren(false);
    }
    
    public EndpointStep(EndpointStep step) {
        super(step);
        this.setAllowsChildren(false);
    }
}

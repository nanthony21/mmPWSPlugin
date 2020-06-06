/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.UI.tree;

import edu.bpl.pwsplugin.acquisitionSequencer.Consts;
import edu.bpl.pwsplugin.utils.JsonableParam;

/**
 *
 * @author nick
 */
public class EndpointStepNode extends StepNode {
    public EndpointStepNode(EndpointStepNode node) { //copy constructor
        super(node);
    }
    
    public EndpointStepNode(JsonableParam settings, Consts.Type type) {
        super(settings, type);
        this.setAllowsChildren(false);
        if (Consts.isContainer(type)) {
            throw new RuntimeException("Creating EndpointStepNode for a container step type.");
        }
    }
    
}

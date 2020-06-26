/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import java.util.List;

/**
 *
 * @author nick
 */
public class FocusLock extends ContainerStep<SequencerSettings.FocusLockSettings> {
    
    public FocusLock() {
        super(new SequencerSettings.FocusLockSettings(), SequencerConsts.Type.PFS);
    }

    private SequencerFunction getCallback() {
        return (status) -> {
            Step[] path = status.coords().getTreePath(); //Indicates our current location in the tree of steps.
            if (path[path.length - 1].getType() == SequencerConsts.Type.ACQ) {
                //If the current  step is an acquisition then check for refocus.
                if (!Globals.core().isContinuousFocusLocked()) {
                    //Check if focused. and log. later we will add refocusing.
                    Globals.mm().logs().logMessage("PWSPlugin: Focus is unlocked");
                    Globals.core().fullFocus();
                    Globals.core().enableContinuousFocus(true);
                    Thread.sleep((long) (settings.preDelay * 1000.0));
                } else {
                    Globals.mm().logs().logMessage("PWSPLugin: Focus is locked");
                }
            }
            return status;
        };
    }

    @Override
    public SequencerFunction getStepFunction(List<SequencerFunction> callbacks) {
        callbacks.add(this.getCallback());
        SequencerFunction stepFunction = super.getSubstepsFunction(callbacks);
        SequencerSettings.FocusLockSettings settings = this.getSettings();
        return new SequencerFunction() {
            @Override
            public AcquisitionStatus applyThrows(AcquisitionStatus status) throws Exception {
                //FocusLock A function that turns on the PFS, runs substep and then turns it off.
                Globals.core().setAutoFocusOffset(settings.zOffset);
                Globals.core().fullFocus();
                Globals.core().enableContinuousFocus(true);
                Thread.sleep((long) (settings.preDelay * 1000.0));
                AcquisitionStatus newstatus = stepFunction.apply(status);
                if (!Globals.core().isContinuousFocusLocked()) {
                    Globals.mm().logs().logMessage("Autofocus failed!");
                    status.newStatusMessage("Autofocus failed!");
                }
                Globals.core().enableContinuousFocus(false);
                return newstatus;
            }
        };
    }

    @Override
    protected SimFn getSimulatedFunction() {
        SimFn subStepSimFn = this.getSubStepSimFunction();
        return (Step.SimulatedStatus status) -> {
            status = subStepSimFn.apply(status);
            return status;
        };
    }
    
}

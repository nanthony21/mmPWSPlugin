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
import edu.bpl.pwsplugin.hardware.MMDeviceException;
import edu.bpl.pwsplugin.hardware.translationStages.TranslationStage1d;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;

/**
 *
 * @author nick
 */
public class FocusLock extends ContainerStep<SequencerSettings.FocusLockSettings> {
    
    public FocusLock() {
        super(new SequencerSettings.FocusLockSettings(), SequencerConsts.Type.PFS);
    }

    @Override
    protected SequencerFunction getCallback() {
        return (status) -> {
            Step[] path = status.coords().getTreePath(); //Indicates our current location in the tree of steps.
            if (path[path.length - 1].getType() == SequencerConsts.Type.ACQ) { //If the current  step is an acquisition then check for refocus.
                TranslationStage1d zStage = Globals.getHardwareConfiguration().getActiveConfiguration().zStage();
                if (!zStage.hasAutoFocus()) {
                    status.newStatusMessage("Focus Lock: Error: The current zStage has no autofocus functionality.");
                    return status;
                }
                if (!zStage.getAutoFocusLocked()) { //Check if focused. and log.
                    status.newStatusMessage("Focus Lock: Focus is unlocked. Reacquiring.");
                    try {
                        zStage.runFullFocus(); // This can fail and throw an exception, don't let that crash the whole experiment.
                    } catch (MMDeviceException e) {
                        status.newStatusMessage("Focus Lock: Error: Focus lock failed to recover focus.");
                    }
                    Thread.sleep(1000); //Without this we will sometime not actually re-enable pfs for some reason.
                    zStage.setAutoFocusEnabled(true);
                    Thread.sleep((long) (settings.delay * 1000.0)); //Does this actually serve any purpose?
                }
            }
            return status;
        };
    }

    @Override
    public SequencerFunction getStepFunction(List<SequencerFunction> callbacks) {
        SequencerFunction subStepFunction = super.getSubstepsFunction(callbacks);
        SequencerSettings.FocusLockSettings settings = this.getSettings();
        return (status) -> {
            //FocusLock A function that turns on the PFS, runs substep and then turns it off.
            TranslationStage1d zstage = Globals.getHardwareConfiguration().getActiveConfiguration().zStage();
            try {
                double startingZ = zstage.getPosUm(); //After finding focus lock we will move back to this z position.
                zstage.runFullFocus();
                Thread.sleep(1000); //Without this we will sometimes not actually re-enable pfs for some reason.
                zstage.setAutoFocusEnabled(true);
                zstage.setPosUm(startingZ); //Move back to our starting position, except now PFS should be locked.
                Thread.sleep((long) (settings.delay * 1000.0));
                
            } catch (MMDeviceException e) {
                status.newStatusMessage("Focus Lock: Error: Focus lock failed to find initial focus.");
                zstage.setAutoFocusEnabled(false); //If we failed then make sure to completely disable autofocus.
            }
            AcquisitionStatus newstatus = subStepFunction.apply(status);
            zstage.setAutoFocusEnabled(false);
            return newstatus;
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
    
    @Override
    public List<String> validate() {
        List<String> errs = super.validate();
        
        //Check that the focus lock doesn't contain any illegal steps such as another focus lock step
        Enumeration<Step> en = (Enumeration<Step>) (Enumeration<? extends TreeNode>) this.breadthFirstEnumeration();
        en.nextElement(); //This clears the first item which is just a reference to this very same step.
        while (en.hasMoreElements()) {
            Step step = en.nextElement();
            if (step.getType().equals(SequencerConsts.Type.PFS)) {
                errs.add(String.format("Optical Focus Lock may not contain a sub-step of type: %s", SequencerConsts.getFactory(SequencerConsts.Type.PFS).getName()));
            } else if (step.getType().equals(SequencerConsts.Type.AF)) { //The autofocus step makes calls that move z without using our custom zStage devices, this will break the focus lock.
                errs.add(String.format("Optical Focus Lock may not contain a sub-step of type: %s", SequencerConsts.getFactory(SequencerConsts.Type.AF).getName()));
            }
        }
        return errs;
    }
}

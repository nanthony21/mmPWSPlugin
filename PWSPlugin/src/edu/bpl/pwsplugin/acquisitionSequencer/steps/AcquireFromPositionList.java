/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import edu.bpl.pwsplugin.Globals;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.micromanager.AutofocusPlugin;
import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;

/**
 *
 * @author nick
 */
public class AcquireFromPositionList implements Step {
    //Executes `step` at each position in the positionlist and increments the cell number each time.
    Step step;
    PositionList list;
    
    public AcquireFromPositionList(Step step, PositionList list) {
        this.step = step;
        this.list = list;
    }
    
    @Override
    public Integer applyThrows(Integer startingCellNum) throws Exception{
        int numOfNewAcqs = 0;

        Globals.core().setTimeoutMs(30000); //TODO put somewhere else. set timeout to 30 seconds. Otherwise we get an error if a position move takes greater than 5 seconds. (default timeout)
        for (int posNum=0; posNum < list.getNumberOfPositions(); posNum++) {
            MultiStagePosition pos = list.getPosition(posNum);
            String label = pos.getLabel();
            Callable<Void> preMoveRoutine = ()->{return null;};
            Callable<Void> postMoveRoutine = ()->{return null;};

            if (label.contains("APFS")) { //Turn off pfs before moving. after moving run autofocus to get bakc i the right range. then enable pfs again.
                preMoveRoutine = ()->{ Globals.core().setProperty("TIPFSStatus", "State", "Off"); return null; };
                postMoveRoutine = ()->{ PFSFuncs.autoFocusThenPFS(); return null; };     
            } else if (label.contains("ZPFS")) { //Turn off pfs, move, reenable pfs. make sure to set a coordinate for z-nonpfs for this to work.
                preMoveRoutine = ()->{ Globals.core().setProperty("TIPFSStatus", "State", "Off"); return null; };     
                postMoveRoutine = ()->{ PFSFuncs.pauseThenPFS(); return null; };
            } else if (label.contains("PFS")) { //If the position name has PFS then turn on pfs for this acquisition and then turn off.
                postMoveRoutine = ()->{ PFSFuncs.alignPFS(); return null; };
            }
            preMoveRoutine.call();
            pos.goToPosition(pos, Globals.core());   //Yes, I know this is weird. It's a static method that needs a position and the core as input.
            postMoveRoutine.call();
            int save_num = startingCellNum + numOfNewAcqs;
            // Set the display message for the type of data being acquired
            String msg = String.format("Acquiring cell: %d at position: %s", save_num, label);           
            Globals.statusAlert().setText(msg);
            numOfNewAcqs += step.apply(save_num);
        }
        list.getPosition(0).goToPosition(list.getPosition(0), Globals.core());
        return numOfNewAcqs;
    }
    
}


class PFSFuncs {
    static void alignPFS() throws Exception {
        //ALIGNPFS Turns on the pfs for a few seconds and then turns it off. If it's
        //already on then just proceed.
        if (Globals.core().getProperty("TIPFSStatus", "State").equals("Off")) {
            Globals.core().setProperty("TIPFSStatus", "State", "On");
            Thread.sleep(3000);
            Globals.core().setProperty("TIPFSStatus", "State", "Off");
        }
    }
    
    static void autoFocusThenPFS() throws Exception {
        //AUTOFOCUSTHENPFS Summary of this function goes here
        //   Detailed explanation goes here
        AutofocusPlugin afPlugin = Globals.mm().getAutofocusManager().getAutofocusMethod();
        afPlugin.fullFocus(); //This blocks until the focus is done
        Thread.sleep(3000);
        Globals.core().setProperty("TIPFSStatus", "State", "On");
        Thread.sleep(5000);
    }
    
    static void pauseThenPFS() throws Exception {
        Thread.sleep(1000);
        Globals.core().setProperty("TIPFSStatus", "State", "On");
        Thread.sleep(3000);
    }
}
///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin
//
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nick Anthony, 2021
//
// COPYRIGHT:    Northwestern University, 2021
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//

package edu.bpl.pwsplugin.acquisitionsequencer;

import edu.bpl.pwsplugin.utils.JsonableParam;
import org.micromanager.PositionList;

/**
 * A collection of settings for the various types of steps.
 *
 * @author Nick Anthony
 */
public class SequencerSettings {

   public static class SoftwareAutoFocusSettings extends JsonableParam {

      //public String afPluginName = "OughtaFocus";
      public double exposureMs = 30;
   }

   public static class RootStepSettings extends JsonableParam {

      public String directory = "";
      public String author = "";
      public String project = "";
      public String cellLine = "";
      public String description = "";

   }

   public static class PauseStepSettings extends JsonableParam {

      public String message = "Paused";
   }

   public static class FocusLockSettings extends JsonableParam {

      public double delay = 1; //Seconds delay after focus
   }

   public static class EveryNTimesSettings extends JsonableParam {

      public Integer n = 2;
      public Integer offset = 0;
   }

   public static class ChangeConfigGroupSettings extends JsonableParam {

      public String configGroupName;
      public String configValue;
   }

   public static class AcquireTimeSeriesSettings extends JsonableParam {

      public int numFrames = 1;
      public double frameIntervalMinutes = 1;

   }

   public static class AcquirePositionsSettings extends JsonableParam {

      public PositionList posList = new PositionList();
   }

   public static class EnterSubfolderSettings extends JsonableParam {

      public String relativePath = "";
   }

   public static class ZStackSettings extends JsonableParam {

      public double intervalUm = 1.0;
      public int numStacks = 2;
      public boolean absolute = false;
      public double startingPosition = 0; //Only used if absolute is true
   }

   public static class AutoShutterSettings extends JsonableParam {

      public String configName = "";
      public Double warmupTimeMinutes = 0.;
   }
}
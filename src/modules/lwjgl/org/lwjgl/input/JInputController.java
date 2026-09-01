package org.lwjgl.input;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Rumbler;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;

class JInputController implements Controller {
   private net.java.games.input.Controller target;
   private int index;
   private ArrayList<Component> buttons = new ArrayList<>();
   private ArrayList<Component> axes = new ArrayList<>();
   private ArrayList<Component> pov = new ArrayList<>();
   private final Map<Component, Integer> buttonIndices = new IdentityHashMap<>();
   private final Map<Component, Integer> axisIndices = new IdentityHashMap<>();
   private final Map<Component, Integer> povIndices = new IdentityHashMap<>();
   private Rumbler[] rumblers;
   private boolean[] buttonState;
   private float[] povValues;
   private float[] axesValue;
   private float[] axesMax;
   private float[] deadZones;
   private int xaxis = -1;
   private int yaxis = -1;
   private int zaxis = -1;
   private int rxaxis = -1;
   private int ryaxis = -1;
   private int rzaxis = -1;

   JInputController(int index, net.java.games.input.Controller target) {
      this.target = target;
      this.index = index;
      Component[] sourceAxes = target.getComponents();

      for (Component sourceAxis : sourceAxes) {
         if (sourceAxis.getIdentifier() instanceof Button) {
            this.buttonIndices.put(sourceAxis, this.buttons.size());
            this.buttons.add(sourceAxis);
         } else if (sourceAxis.getIdentifier().equals(Axis.POV)) {
            this.povIndices.put(sourceAxis, this.pov.size());
            this.pov.add(sourceAxis);
         } else {
            int axisIndex = this.axes.size();
            this.axisIndices.put(sourceAxis, axisIndex);
            this.axes.add(sourceAxis);
            if (sourceAxis.getIdentifier().equals(Axis.X)) {
               this.xaxis = axisIndex;
            }

            if (sourceAxis.getIdentifier().equals(Axis.Y)) {
               this.yaxis = axisIndex;
            }

            if (sourceAxis.getIdentifier().equals(Axis.Z)) {
               this.zaxis = axisIndex;
            }

            if (sourceAxis.getIdentifier().equals(Axis.RX)) {
               this.rxaxis = axisIndex;
            }

            if (sourceAxis.getIdentifier().equals(Axis.RY)) {
               this.ryaxis = axisIndex;
            }

            if (sourceAxis.getIdentifier().equals(Axis.RZ)) {
               this.rzaxis = axisIndex;
            }
         }
      }

      this.buttonState = new boolean[this.buttons.size()];
      this.povValues = new float[this.pov.size()];
      this.axesValue = new float[this.axes.size()];

      for (int i = 0; i < this.buttons.size(); i++) {
         this.buttonState[i] = this.buttons.get(i).getPollData() != 0.0F;
      }

      for (int i = 0; i < this.axes.size(); i++) {
         this.axesValue[i] = this.axes.get(i).getPollData();
      }

      this.axesMax = new float[this.axes.size()];
      this.deadZones = new float[this.axes.size()];

      for (int i = 0; i < this.axesMax.length; i++) {
         this.axesMax[i] = 1.0F;
         this.deadZones[i] = 0.05F;
      }

      this.rumblers = target.getRumblers();
   }

   @Override
   public String getName() {
      return this.target.getName();
   }

   @Override
   public int getIndex() {
      return this.index;
   }

   @Override
   public int getButtonCount() {
      return this.buttons.size();
   }

   @Override
   public String getButtonName(int index) {
      return this.buttons.get(index).getName();
   }

   @Override
   public boolean isButtonPressed(int index) {
      return this.buttonState[index];
   }

   @Override
   public void poll() {
      this.target.poll();
      Event event = new Event();
      EventQueue queue = this.target.getEventQueue();

      while (queue.getNextEvent(event)) {
         Component component = event.getComponent();
         Integer buttonIndex = this.buttonIndices.get(component);
         if (buttonIndex != null) {
            int buttonPosition = buttonIndex;
            this.buttonState[buttonPosition] = event.getValue() != 0.0F;
            Controllers.addEvent(new ControllerEvent(this, event.getNanos(), 1, buttonPosition, this.buttonState[buttonPosition], false, false, 0.0F, 0.0F));
         }

         Integer povIndex = this.povIndices.get(component);
         if (povIndex != null) {
            int povPosition = povIndex;
            float prevX = this.getPovX();
            float prevY = this.getPovY();
            this.povValues[povPosition] = event.getValue();
            if (prevX != this.getPovX()) {
               Controllers.addEvent(new ControllerEvent(this, event.getNanos(), 3, 0, false, false));
            }

            if (prevY != this.getPovY()) {
               Controllers.addEvent(new ControllerEvent(this, event.getNanos(), 4, 0, false, false));
            }
         }

         Integer axisIndex = this.axisIndices.get(component);
         if (axisIndex != null) {
            int axisPosition = axisIndex;
            Component axis = component;
            float value = axis.getPollData();
            float xaxisValue = 0.0F;
            float yaxisValue = 0.0F;
            if (Math.abs(value) < this.deadZones[axisPosition]) {
               value = 0.0F;
            }

            if (Math.abs(value) < axis.getDeadZone()) {
               value = 0.0F;
            }

            if (Math.abs(value) > this.axesMax[axisPosition]) {
               this.axesMax[axisPosition] = Math.abs(value);
            }

            value /= this.axesMax[axisPosition];
            if (axisPosition == this.xaxis) {
               xaxisValue = value;
            }

            if (axisPosition == this.yaxis) {
               yaxisValue = value;
            }

            Controllers.addEvent(
               new ControllerEvent(
                  this, event.getNanos(), 2, axisPosition, false, axisPosition == this.xaxis, axisPosition == this.yaxis, xaxisValue, yaxisValue
               )
            );
            this.axesValue[axisPosition] = value;
         }
      }
   }

   @Override
   public int getAxisCount() {
      return this.axes.size();
   }

   @Override
   public String getAxisName(int index) {
      return this.axes.get(index).getName();
   }

   @Override
   public float getAxisValue(int index) {
      return this.axesValue[index];
   }

   @Override
   public float getXAxisValue() {
      return this.xaxis == -1 ? 0.0F : this.getAxisValue(this.xaxis);
   }

   @Override
   public float getYAxisValue() {
      return this.yaxis == -1 ? 0.0F : this.getAxisValue(this.yaxis);
   }

   @Override
   public float getXAxisDeadZone() {
      return this.xaxis == -1 ? 0.0F : this.getDeadZone(this.xaxis);
   }

   @Override
   public float getYAxisDeadZone() {
      return this.yaxis == -1 ? 0.0F : this.getDeadZone(this.yaxis);
   }

   @Override
   public void setXAxisDeadZone(float zone) {
      this.setDeadZone(this.xaxis, zone);
   }

   @Override
   public void setYAxisDeadZone(float zone) {
      this.setDeadZone(this.yaxis, zone);
   }

   @Override
   public float getDeadZone(int index) {
      return this.deadZones[index];
   }

   @Override
   public void setDeadZone(int index, float zone) {
      this.deadZones[index] = zone;
   }

   @Override
   public float getZAxisValue() {
      return this.zaxis == -1 ? 0.0F : this.getAxisValue(this.zaxis);
   }

   @Override
   public float getZAxisDeadZone() {
      return this.zaxis == -1 ? 0.0F : this.getDeadZone(this.zaxis);
   }

   @Override
   public void setZAxisDeadZone(float zone) {
      this.setDeadZone(this.zaxis, zone);
   }

   @Override
   public float getRXAxisValue() {
      return this.rxaxis == -1 ? 0.0F : this.getAxisValue(this.rxaxis);
   }

   @Override
   public float getRXAxisDeadZone() {
      return this.rxaxis == -1 ? 0.0F : this.getDeadZone(this.rxaxis);
   }

   @Override
   public void setRXAxisDeadZone(float zone) {
      this.setDeadZone(this.rxaxis, zone);
   }

   @Override
   public float getRYAxisValue() {
      return this.ryaxis == -1 ? 0.0F : this.getAxisValue(this.ryaxis);
   }

   @Override
   public float getRYAxisDeadZone() {
      return this.ryaxis == -1 ? 0.0F : this.getDeadZone(this.ryaxis);
   }

   @Override
   public void setRYAxisDeadZone(float zone) {
      this.setDeadZone(this.ryaxis, zone);
   }

   @Override
   public float getRZAxisValue() {
      return this.rzaxis == -1 ? 0.0F : this.getAxisValue(this.rzaxis);
   }

   @Override
   public float getRZAxisDeadZone() {
      return this.rzaxis == -1 ? 0.0F : this.getDeadZone(this.rzaxis);
   }

   @Override
   public void setRZAxisDeadZone(float zone) {
      this.setDeadZone(this.rzaxis, zone);
   }

   @Override
   public float getPovX() {
      if (this.pov.size() == 0) {
         return 0.0F;
      } else {
         float value = this.povValues[0];
         if (value == 0.875F || value == 0.125F || value == 1.0F) {
            return -1.0F;
         } else {
            return value != 0.625F && value != 0.375F && value != 0.5F ? 0.0F : 1.0F;
         }
      }
   }

   @Override
   public float getPovY() {
      if (this.pov.size() == 0) {
         return 0.0F;
      } else {
         float value = this.povValues[0];
         if (value == 0.875F || value == 0.625F || value == 0.75F) {
            return 1.0F;
         } else {
            return value != 0.125F && value != 0.375F && value != 0.25F ? 0.0F : -1.0F;
         }
      }
   }

   @Override
   public int getRumblerCount() {
      return this.rumblers.length;
   }

   @Override
   public String getRumblerName(int index) {
      return this.rumblers[index].getAxisName();
   }

   @Override
   public void setRumblerStrength(int index, float strength) {
      this.rumblers[index].rumble(strength);
   }
}

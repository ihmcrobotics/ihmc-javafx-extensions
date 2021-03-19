package javafx.scene.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.EnumConverter;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.layout.Region;

public class InvisibleNumberAxis extends Region
{

   // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

   private Orientation effectiveOrientation;
   private double oldLength = 0;
   /** True when the current range invalid and all dependent calculations need to be updated */
   boolean rangeValid = false;

   /** The side of the plot which this axis is being drawn on */
   private ObjectProperty<Side> side = new StyleableObjectProperty<Side>()
   {
      @Override
      protected void invalidated()
      {
         // cause refreshTickMarks
         Side edge = get();
         pseudoClassStateChanged(TOP_PSEUDOCLASS_STATE, edge == Side.TOP);
         pseudoClassStateChanged(RIGHT_PSEUDOCLASS_STATE, edge == Side.RIGHT);
         pseudoClassStateChanged(BOTTOM_PSEUDOCLASS_STATE, edge == Side.BOTTOM);
         pseudoClassStateChanged(LEFT_PSEUDOCLASS_STATE, edge == Side.LEFT);
         requestAxisLayout();
      }

      @Override
      public CssMetaData<InvisibleNumberAxis, Side> getCssMetaData()
      {
         return StyleableProperties.SIDE;
      }

      @Override
      public Object getBean()
      {
         return InvisibleNumberAxis.this;
      }

      @Override
      public String getName()
      {
         return "side";
      }
   };

   public final Side getSide()
   {
      return side.get();
   }

   public final void setSide(Side value)
   {
      side.set(value);
   }

   public final ObjectProperty<Side> sideProperty()
   {
      return side;
   }

   private final Side getEffectiveSide()
   {
      final Side side = getSide();
      if (side == null || side.isVertical() && effectiveOrientation == Orientation.HORIZONTAL
            || side.isHorizontal() && effectiveOrientation == Orientation.VERTICAL)
      {
         // Means side == null && effectiveOrientation == null produces Side.BOTTOM
         return effectiveOrientation == Orientation.VERTICAL ? Side.LEFT : Side.BOTTOM;
      }
      return side;
   }

   /** This is true when the axis determines its range from the data automatically */
   private BooleanProperty autoRanging = new BooleanPropertyBase(true)
   {
      @Override
      protected void invalidated()
      {
         if (get())
         {
            // auto range turned on, so need to auto range now
            //               autoRangeValid = false;
            requestAxisLayout();
         }
      }

      @Override
      public Object getBean()
      {
         return InvisibleNumberAxis.this;
      }

      @Override
      public String getName()
      {
         return "autoRanging";
      }
   };

   public final boolean isAutoRanging()
   {
      return autoRanging.get();
   }

   public final void setAutoRanging(boolean value)
   {
      autoRanging.set(value);
   }

   public final BooleanProperty autoRangingProperty()
   {
      return autoRanging;
   }

   // -------------- METHODS ------------------------------------------------------------------------------------------

   /**
    * See if the current range is valid, if it is not then any range dependent calulcations need to
    * redone on the next layout pass
    *
    * @return true if current range calculations are valid
    */
   private final boolean isRangeValid()
   {
      return rangeValid;
   }

   /**
    * Mark the current range invalid, this will cause anything that depends on the range to be
    * recalculated on the next layout.
    */
   private final void invalidateRange()
   {
      rangeValid = false;
   }

   /**
    * We suppress requestLayout() calls here by doing nothing as we don't want changes to our children
    * to cause layout. If you really need to request layout then call requestAxisLayout().
    */
   @Override
   public void requestLayout()
   {
   }

   /**
    * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it
    * has been overridden to do nothing so that changes to children's bounds etc do not cause a layout.
    * This was done as a optimization as the InvisibleNumberAxis knows the exact minimal set of changes
    * that really need layout to be updated. So we only want to request layout then, not on any child
    * change.
    */
   public void requestAxisLayout()
   {
      super.requestLayout();
   }

   /**
    * Computes the preferred height of this axis for the given width. If axis orientation is
    * horizontal, it takes into account the tick mark length, tick label gap and label height.
    *
    * @return the computed preferred width for this axis
    */
   @Override
   protected double computePrefHeight(double width)
   {
      final Side side = getEffectiveSide();
      if (side.isVertical())
      {
         // TODO for now we have no hard and fast answer here, I guess it should work
         // TODO out the minimum size needed to display min, max and zero tick mark labels.
         return 100;
      }
      else
      { // HORIZONTAL
        // we need to first auto range as this may/will effect tick marks
        // calculate the new tick marks
        // calculate tick mark length
        // calculate label height
         return 0;
      }
   }

   /**
    * Computes the preferred width of this axis for the given height. If axis orientation is vertical,
    * it takes into account the tick mark length, tick label gap and label height.
    *
    * @return the computed preferred width for this axis
    */
   @Override
   protected double computePrefWidth(double height)
   {
      final Side side = getEffectiveSide();
      if (side.isVertical())
      {
         // calculate label height
         return 0;
      }
      else
      { // HORIZONTAL
        // TODO for now we have no hard and fast answer here, I guess it should work
        // TODO out the minimum size needed to display min, max and zero tick mark labels.
         return 100;
      }
   }

   // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

   private double offset;
   /**
    * This is the minimum current data value and it is used while auto ranging. Package private solely
    * for test purposes
    */
   double dataMinValue;
   /**
    * This is the maximum current data value and it is used while auto ranging. Package private solely
    * for test purposes
    */
   double dataMaxValue;
   // -------------- PRIVATE PROPERTIES -------------------------------------------------------------------------------

   /**
    * The current value for the lowerBound of this axis (minimum value). This may be the same as
    * lowerBound or different. It is used by NumberAxis to animate the lowerBound from the old value to
    * the new value.
    */
   private final DoubleProperty currentLowerBound = new SimpleDoubleProperty(this, "currentLowerBound");

   // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

   /** The scale factor from data units to visual units */
   private ReadOnlyDoubleWrapper scale = new ReadOnlyDoubleWrapper(this, "scale", 0)
   {
      @Override
      protected void invalidated()
      {
         requestAxisLayout();
      }
   };

   public final double getScale()
   {
      return scale.get();
   }

   private final void setScale(double scale)
   {
      this.scale.set(scale);
   }

   public final ReadOnlyDoubleProperty scaleProperty()
   {
      return scale.getReadOnlyProperty();
   }

   /**
    * The value for the upper bound of this axis (maximum value). This is automatically set if auto
    * ranging is on.
    */
   private DoubleProperty upperBound = new DoublePropertyBase(100)
   {
      @Override
      protected void invalidated()
      {
         if (!isAutoRanging())
         {
            invalidateRange();
            requestAxisLayout();
         }
      }

      @Override
      public Object getBean()
      {
         return InvisibleNumberAxis.this;
      }

      @Override
      public String getName()
      {
         return "upperBound";
      }
   };

   public final double getUpperBound()
   {
      return upperBound.get();
   }

   public final void setUpperBound(double value)
   {
      upperBound.set(value);
   }

   public final DoubleProperty upperBoundProperty()
   {
      return upperBound;
   }

   /**
    * The value for the lower bound of this axis (minimum value). This is automatically set if auto
    * ranging is on.
    */
   private DoubleProperty lowerBound = new DoublePropertyBase(0)
   {
      @Override
      protected void invalidated()
      {
         if (!isAutoRanging())
         {
            invalidateRange();
            requestAxisLayout();
         }
      }

      @Override
      public Object getBean()
      {
         return InvisibleNumberAxis.this;
      }

      @Override
      public String getName()
      {
         return "lowerBound";
      }
   };

   public final double getLowerBound()
   {
      return lowerBound.get();
   }

   public final void setLowerBound(double value)
   {
      lowerBound.set(value);
   }

   public final DoubleProperty lowerBoundProperty()
   {
      return lowerBound;
   }

   // -------------- PROTECTED METHODS --------------------------------------------------------------------------------

   /**
    * This calculates the upper and lower bound based on the data provided to invalidateRange() method.
    * This must not affect the state of the axis. Any results of the auto-ranging should be returned in
    * the range object. This will we passed to setRange() if it has been decided to adopt this range
    * for this axis.
    *
    * @param length The length of the axis in screen coordinates
    * @return Range information, this is implementation dependent
    */
   private final double[] autoRange(double length)
   {
      // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
      if (isAutoRanging())
      {
         // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
         return autoRange(dataMinValue, dataMaxValue, length);
      }
      else
      {
         return getRange();
      }
   }

   /**
    * Calculates new scale for this axis. This should not affect any properties of this axis.
    *
    * @param length     The display length of the axis
    * @param lowerBound The lower bound value
    * @param upperBound The upper bound value
    * @return new scale to fit the range from lower bound to upper bound in the given display length
    */
   private final double calculateNewScale(double length, double lowerBound, double upperBound)
   {
      double newScale = 1;
      final Side side = getEffectiveSide();
      if (side.isVertical())
      {
         offset = length;
         newScale = upperBound - lowerBound == 0 ? -length : -(length / (upperBound - lowerBound));
      }
      else
      { // HORIZONTAL
         offset = 0;
         newScale = upperBound - lowerBound == 0 ? length : length / (upperBound - lowerBound);
      }
      return newScale;
   }

   /**
    * Invoked during the layout pass to layout this axis and all its content.
    */
   @Override
   protected void layoutChildren()
   {
      final Side side = getEffectiveSide();
      final double length = side.isVertical() ? getHeight() : getWidth();
      // if we are not auto ranging we need to calculate the new scale
      if (!isAutoRanging())
      {
         // calculate new scale
         setScale(calculateNewScale(length, getLowerBound(), getUpperBound()));
         // update current lower bound
         currentLowerBound.set(getLowerBound());
      }
      // we have done all auto calcs, let InvisibleNumberAxis position major tickmarks

      // auto range if it is not valid
      boolean rangeInvalid = !isRangeValid();
      boolean lengthDiffers = oldLength != length;
      if (lengthDiffers || rangeInvalid)
      {
         // get range
         double[] range;
         if (isAutoRanging())
         {
            // auto range
            range = autoRange(length);
            // set current range to new range
            setRange(range[0], range[1], range[2]);
         }
         else
         {
            range = getRange();
         }

         // mark all done
         oldLength = length;
         rangeValid = true;
      }
   }

   // -------------- METHODS ------------------------------------------------------------------------------------------

   /**
    * Called when the data has changed and the range may not be valid anymore. This is only called by
    * the chart if isAutoRanging() returns true. If we are auto ranging it will cause layout to be
    * requested and auto ranging to happen on next layout pass.
    *
    * @param data The current set of all data that needs to be plotted on this axis
    */
   public void invalidateRange(List<Number> data)
   {
      if (data.isEmpty())
      {
         dataMaxValue = getUpperBound();
         dataMinValue = getLowerBound();
      }
      else
      {
         dataMinValue = Double.MAX_VALUE;
         // We need to init to the lowest negative double (which is NOT Double.MIN_VALUE)
         // in order to find the maximum (positive or negative)
         dataMaxValue = -Double.MAX_VALUE;
      }
      for (Number dataValue : data)
      {
         dataMinValue = Math.min(dataMinValue, dataValue.doubleValue());
         dataMaxValue = Math.max(dataMaxValue, dataValue.doubleValue());
      }
      invalidateRange();
      requestAxisLayout();
   }

   /**
    * Gets the display position along this axis for a given value. If the value is not in the current
    * range, the returned value will be an extrapolation of the display position.
    *
    * @param value The data value to work out display position for
    * @return display position
    */
   public double getDisplayPosition(Number value)
   {
      return offset + (value.doubleValue() - currentLowerBound.get()) * getScale();
   }

   /**
    * Gets the data value for the given display position on this axis. If the axis is a CategoryAxis
    * this will be the nearest value.
    *
    * @param displayPosition A pixel position on this axis
    * @return the nearest data value to the given pixel position or null if not on axis;
    */
   public Number getValueForDisplay(double displayPosition)
   {
      return toRealValue((displayPosition - offset) / getScale() + currentLowerBound.get());
   }

   /**
    * Gets the display position of the zero line along this axis.
    *
    * @return display position or Double.NaN if zero is not in current range;
    */
   public double getZeroPosition()
   {
      if (0 < getLowerBound() || 0 > getUpperBound())
         return Double.NaN;
      //noinspection unchecked
      return getDisplayPosition(Double.valueOf(0));
   }

   /**
    * Checks if the given value is plottable on this axis
    *
    * @param value The value to check if its on axis
    * @return true if the given value is plottable on this axis
    */
   public boolean isValueOnAxis(Number value)
   {
      final double num = value.doubleValue();
      return num >= getLowerBound() && num <= getUpperBound();
   }

   /**
    * All axis values must be representable by some numeric value. This gets the numeric value for a
    * given data value.
    *
    * @param value The data value to convert
    * @return Numeric value for the given data value
    */
   public double toNumericValue(Number value)
   {
      return value == null ? Double.NaN : value.doubleValue();
   }

   /**
    * All axis values must be representable by some numeric value. This gets the data value for a given
    * numeric value.
    *
    * @param value The numeric value to convert
    * @return Data value for given numeric value
    */
   public Number toRealValue(double value)
   {
      //noinspection unchecked
      return Double.valueOf(value);
   }

   // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

   /**
    * When true zero is always included in the visible range. This only has effect if auto-ranging is
    * on.
    */
   private BooleanProperty forceZeroInRange = new BooleanPropertyBase(true)
   {
      @Override
      protected void invalidated()
      {
         // This will affect layout if we are auto ranging
         if (isAutoRanging())
         {
            requestAxisLayout();
            invalidateRange();
         }
      }

      @Override
      public Object getBean()
      {
         return InvisibleNumberAxis.this;
      }

      @Override
      public String getName()
      {
         return "forceZeroInRange";
      }
   };

   public final boolean isForceZeroInRange()
   {
      return forceZeroInRange.getValue();
   }

   public final void setForceZeroInRange(boolean value)
   {
      forceZeroInRange.setValue(value);
   }

   public final BooleanProperty forceZeroInRangeProperty()
   {
      return forceZeroInRange;
   }

   // -------------- CONSTRUCTOR --------------------------------------------------------------------------------------

   /**
    * Creates an auto-ranging InvisibleNumberAxis.
    */
   public InvisibleNumberAxis()
   {
      getStyleClass().setAll("axis");
   }

   /**
    * Creates a non-auto-ranging InvisibleNumberAxis with the given upper bound, lower bound and tick
    * unit.
    *
    * @param lowerBound The lower bound for this axis, i.e. min plottable value
    * @param upperBound The upper bound for this axis, i.e. max plottable value
    * @param tickUnit   The tick unit, i.e. space between tickmarks
    */
   public InvisibleNumberAxis(double lowerBound, double upperBound, double tickUnit)
   {
      this();
      setAutoRanging(false);
      setLowerBound(lowerBound);
      setUpperBound(upperBound);
   }

   // -------------- PROTECTED METHODS --------------------------------------------------------------------------------

   /**
    * Called to get the current axis range.
    *
    * @return A range object that can be passed to setRange() and calculateTickValues()
    */
   private double[] getRange()
   {
      return new double[] {getLowerBound(), getUpperBound(), getScale()};
   }

   /**
    * Called to set the current axis range to the given range. If isAnimating() is true then this
    * method should animate the range to the new range.
    *
    * @param range   A range object returned from autoRange()
    * @param animate If true animate the change in range
    */
   private void setRange(double lowerBound, double upperBound, double scale)
   {
      setLowerBound(lowerBound);
      setUpperBound(upperBound);
      currentLowerBound.set(lowerBound);
      setScale(scale);
   }

   /**
    * Called to set the upper and lower bound and anything else that needs to be auto-ranged.
    *
    * @param minValue  The min data value that needs to be plotted on this axis
    * @param maxValue  The max data value that needs to be plotted on this axis
    * @param length    The length of the axis in display coordinates
    * @param labelSize The approximate average size a label takes along the axis
    * @return The calculated range
    */
   private double[] autoRange(double minValue, double maxValue, double length)
   {
      // check if we need to force zero into range
      if (isForceZeroInRange())
      {
         if (maxValue < 0)
         {
            maxValue = 0;
         }
         else if (minValue > 0)
         {
            minValue = 0;
         }
      }

      double range = maxValue - minValue;

      if (range != 0 && range / 2 <= Math.ulp(minValue))
      {
         range = 0;
      }
      // pad min and max by 2%, checking if the range is zero
      final double paddedRange = range == 0 ? minValue == 0 ? 2 : Math.abs(minValue) * 0.02 : Math.abs(range) * 1.02;
      final double padding = (paddedRange - range) / 2;
      // if min and max are not zero then add padding to them
      double paddedMin = minValue - padding;
      double paddedMax = maxValue + padding;
      // calculate new scale
      final double newScale = calculateNewScale(length, paddedMin, paddedMax);
      // return new range
      return new double[] {paddedMin, paddedMax, newScale};
   }

   // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

   private static class StyleableProperties
   {
      private static final CssMetaData<InvisibleNumberAxis, Side> SIDE = new CssMetaData<InvisibleNumberAxis, Side>("-fx-side",
                                                                                                                    new EnumConverter<Side>(Side.class))
      {

         @Override
         public boolean isSettable(InvisibleNumberAxis n)
         {
            return n.side == null || !n.side.isBound();
         }

         @SuppressWarnings("unchecked") // sideProperty() is StyleableProperty<Side>
         @Override
         public StyleableProperty<Side> getStyleableProperty(InvisibleNumberAxis n)
         {
            return (StyleableProperty<Side>) n.sideProperty();
         }
      };

      private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
      static
      {
         final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
         styleables.add(SIDE);
         STYLEABLES = Collections.unmodifiableList(styleables);
      }
   }

   /**
    * @return The CssMetaData associated with this class, which may include the CssMetaData of its
    *         superclasses.
    * @since JavaFX 8.0
    */
   public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData()
   {
      return StyleableProperties.STYLEABLES;
   }

   /**
    * {@inheritDoc}
    *
    * @since JavaFX 8.0
    */
   @Override
   public List<CssMetaData<? extends Styleable, ?>> getCssMetaData()
   {
      return getClassCssMetaData();
   }

   /** pseudo-class indicating this is a vertical Top side InvisibleNumberAxis. */
   private static final PseudoClass TOP_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("top");
   /** pseudo-class indicating this is a vertical Bottom side InvisibleNumberAxis. */
   private static final PseudoClass BOTTOM_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("bottom");
   /** pseudo-class indicating this is a vertical Left side InvisibleNumberAxis. */
   private static final PseudoClass LEFT_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("left");
   /** pseudo-class indicating this is a vertical Right side InvisibleNumberAxis. */
   private static final PseudoClass RIGHT_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("right");
}

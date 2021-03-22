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
      }
      // we have done all auto calcs, let InvisibleNumberAxis position major tickmarks

      // auto range if it is not valid
      boolean rangeInvalid = !isRangeValid();
      boolean lengthDiffers = oldLength != length;
      if (lengthDiffers || rangeInvalid)
      {
         if (isAutoRanging())
            autoRange(dataMinValue, dataMaxValue, length);

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
    * Called when the data has changed and the range may not be valid anymore. This is only called by
    * the chart if isAutoRanging() returns true. If we are auto ranging it will cause layout to be
    * requested and auto ranging to happen on next layout pass.
    *
    * @param data The current set of all data that needs to be plotted on this axis
    */
   public void invalidateRange(double[] data)
   {
      if (data.length == 0)
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
      for (double dataValue : data)
      {
         dataMinValue = Math.min(dataMinValue, dataValue);
         dataMaxValue = Math.max(dataMaxValue, dataValue);
      }
      invalidateRange();
      requestAxisLayout();
   }

   /**
    * Called when the data has changed and the range may not be valid anymore. This is only called by
    * the chart if isAutoRanging() returns true. If we are auto ranging it will cause layout to be
    * requested and auto ranging to happen on next layout pass.
    *
    * @param data The current set of all data that needs to be plotted on this axis
    */
   public void invalidateRange(double minValue, double maxValue)
   {
      dataMinValue = minValue;
      dataMaxValue = maxValue;
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
   public double getDisplayPosition(double value)
   {
      return offset + (value - lowerBound.get()) * getScale();
   }

   /**
    * Gets the data value for the given display position on this axis. If the axis is a CategoryAxis
    * this will be the nearest value.
    *
    * @param displayPosition A pixel position on this axis
    * @return the nearest data value to the given pixel position or null if not on axis;
    */
   public double getValueForDisplay(double displayPosition)
   {
      return (displayPosition - offset) / getScale() + lowerBound.get();
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
   public boolean isValueOnAxis(double value)
   {
      return value >= getLowerBound() && value <= getUpperBound();
   }

   // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

   /** Padding expressed in percent of the actual data range. */
   private DoubleProperty rangePadding = new SimpleDoubleProperty(this, "rangePadding", 0.05)
   {
      @Override
      protected void invalidated()
      {
         invalidateRange();
         requestAxisLayout();
      };
   };

   public double getRangePadding()
   {
      return rangePadding.get();
   }

   public void setRangePadding(double rangePadding)
   {
      this.rangePadding.set(rangePadding);
   }

   public DoubleProperty rangePaddingProperty()
   {
      return rangePadding;
   }

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
    * Called to set the upper and lower bound and anything else that needs to be auto-ranged.
    *
    * @param minValue  The min data value that needs to be plotted on this axis
    * @param maxValue  The max data value that needs to be plotted on this axis
    * @param length    The length of the axis in display coordinates
    * @param labelSize The approximate average size a label takes along the axis
    */
   private void autoRange(double minValue, double maxValue, double length)
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
      final double paddedRange;
      if (range == 0)
         paddedRange = minValue == 0 ? getRangePadding() : Math.abs(minValue) * getRangePadding();
      else
         paddedRange = Math.abs(range) * (1.0 + getRangePadding());

      final double padding = (paddedRange - range) / 2;
      // if min and max are not zero then add padding to them
      double paddedMin = minValue - padding;
      double paddedMax = maxValue + padding;

      setLowerBound(paddedMin);
      setUpperBound(paddedMax);
      setScale(calculateNewScale(length, paddedMin, paddedMax));
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

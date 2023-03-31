package javafx.scene.chart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

public interface FastAxisBase
{
   Region asRegion();

   default double prefWidth(double height)
   {
      return asRegion().prefWidth(height);
   }

   default double prefHeight(double width)
   {
      return asRegion().prefHeight(width);
   }

   default double getWidth()
   {
      return asRegion().getWidth();
   }

   default double getHeight()
   {
      return asRegion().getHeight();
   }

   default Point2D screenToLocal(double screenX, double screenY)
   {
      return asRegion().screenToLocal(screenX, screenY);
   }

   default void resizeRelocate(double x, double y, double width, double height)
   {
      asRegion().resizeRelocate(x, y, width, height);
   }

   void requestAxisLayout();

   default void layout()
   {
      asRegion().layout();
   }

   default Side getSide()
   {
      return sideProperty().get();
   }

   default void setSide(Side value)
   {
      sideProperty().set(value);
   }

   ObjectProperty<Side> sideProperty();

   default boolean isAutoRanging()
   {
      return autoRangingProperty().get();
   }

   default void setAutoRanging(boolean value)
   {
      autoRangingProperty().set(value);
   }

   BooleanProperty autoRangingProperty();

   default void invalidateRange(double[] data)
   {
      List<Number> dataAsList = new ArrayList<>();
      for (double value : data)
      {
         dataAsList.add(value);
      }
      invalidateRange(dataAsList);
   }

   default void invalidateRange(double minValue, double maxValue)
   {
      invalidateRange(Arrays.asList(minValue, maxValue));
   }

   void invalidateRange(List<Number> data);

   double getDisplayPosition(double value);

   double getValueForDisplay(double displayPosition);

   void setForceZeroInRange(boolean value);

   default double getTickUnit()
   {
      if (tickUnitProperty() != null)
         return tickUnitProperty().get();
      return Double.NaN;
   }

   default void setTickUnit(double value)
   {
      if (tickUnitProperty() != null)
         tickUnitProperty().set(value);
   }

   DoubleProperty tickUnitProperty();

   default boolean isMinorTickVisible()
   {
      if (minorTickVisibleProperty() != null)
         return minorTickVisibleProperty().get();
      return false;
   }

   default void setMinorTickVisible(boolean value)
   {
      if (minorTickVisibleProperty() != null)
         minorTickVisibleProperty().set(value);
   }

   BooleanProperty minorTickVisibleProperty();

   default double getScale()
   {
      return scaleProperty().get();
   }

   ReadOnlyDoubleProperty scaleProperty();

   default double getLowerBound()
   {
      return lowerBoundProperty().get();
   }

   default void setLowerBound(double value)
   {
      lowerBoundProperty().set(value);
   }

   DoubleProperty lowerBoundProperty();

   default double getUpperBound()
   {
      return upperBoundProperty().get();
   }

   default void setUpperBound(double value)
   {
      upperBoundProperty().set(value);
   }

   DoubleProperty upperBoundProperty();

   default StringConverter<Number> getTickLabelFormatter()
   {
      if (tickLabelFormatterProperty() != null)
         return tickLabelFormatterProperty().getValue();
      return null;
   }

   default void setTickLabelFormatter(StringConverter<Number> value)
   {
      if (tickLabelFormatterProperty() != null)
         tickLabelFormatterProperty().setValue(value);
   }

   ObjectProperty<StringConverter<Number>> tickLabelFormatterProperty();

   default double getMinorTickLength()
   {
      if (minorTickLengthProperty() != null)
         return minorTickLengthProperty().get();
      return Double.NaN;
   }

   default void setMinorTickLength(double value)
   {
      if (minorTickLengthProperty() != null)
         minorTickLengthProperty().set(value);
   }

   DoubleProperty minorTickLengthProperty();

   default int getMinorTickCount()
   {
      return minorTickCountProperty().get();
   }

   default void setMinorTickCount(int value)
   {
      minorTickCountProperty().set(value);
   }

   IntegerProperty minorTickCountProperty();

   default String getLabel()
   {
      if (labelProperty() != null)
         return labelProperty().get();
      return null;
   }

   default void setLabel(String value)
   {
      if (labelProperty() != null)
         labelProperty().set(value);
   }

   ObjectProperty<String> labelProperty();

   default boolean isTickMarkVisible()
   {
      if (tickMarkVisibleProperty() != null)
         return tickMarkVisibleProperty().get();
      return false;
   }

   default void setTickMarkVisible(boolean value)
   {
      if (tickMarkVisibleProperty() != null)
         tickMarkVisibleProperty().set(value);
   }

   BooleanProperty tickMarkVisibleProperty();

   default boolean isTickLabelsVisible()
   {
      if (tickLabelsVisibleProperty() != null)
         return tickLabelsVisibleProperty().get();
      return false;
   }

   default void setTickLabelsVisible(boolean value)
   {
      if (tickLabelsVisibleProperty() != null)
         tickLabelsVisibleProperty().set(value);
   }

   default double getTickLength()
   {
      if (tickLengthProperty() != null)
         return tickLengthProperty().get();
      return Double.NaN;
   }

   default void setTickLength(double value)
   {
      if (tickLengthProperty() != null)
         tickLengthProperty().set(value);
   }

   DoubleProperty tickLengthProperty();

   BooleanProperty tickLabelsVisibleProperty();

   default Font getTickLabelFont()
   {
      if (tickLabelFontProperty() != null)
         return tickLabelFontProperty().get();
      return null;
   }

   default void setTickLabelFont(Font value)
   {
      if (tickLabelFontProperty() != null)
         tickLabelFontProperty().set(value);
   }

   ObjectProperty<Font> tickLabelFontProperty();

   default Paint getTickLabelFill()
   {
      if (tickLabelFillProperty() != null)
         return tickLabelFillProperty().get();
      return null;
   }

   default void setTickLabelFill(Paint value)
   {
      if (tickLabelFillProperty() != null)
         tickLabelFillProperty().set(value);
   }

   ObjectProperty<Paint> tickLabelFillProperty();

   default double getTickLabelGap()
   {
      if (tickLabelGapProperty() != null)
         return tickLabelGapProperty().get();
      return Double.NaN;
   }

   default void setTickLabelGap(double value)
   {
      if (tickLabelGapProperty() != null)
         tickLabelGapProperty().set(value);
   }

   DoubleProperty tickLabelGapProperty();

   default boolean getAnimated()
   {
      if (animatedProperty() != null)
         return animatedProperty().get();
      return false;
   }

   default void setAnimated(boolean value)
   {
      if (animatedProperty() != null)
         animatedProperty().set(value);
   }

   BooleanProperty animatedProperty();

   default double getTickLabelRotation()
   {
      if (tickLabelRotationProperty() != null)
         return tickLabelRotationProperty().getValue();
      return Double.NaN;
   }

   default void setTickLabelRotation(double value)
   {
      if (tickLabelRotationProperty() != null)
         tickLabelRotationProperty().setValue(value);
   }

   DoubleProperty tickLabelRotationProperty();

   void setEffectiveOrientation(Orientation orientation);

   public static FastAxisBase wrap(NumberAxis numberAxis)
   {
      return new FastAxisBase()
      {
         @Override
         public Region asRegion()
         {
            return numberAxis;
         }

         @Override
         public void requestAxisLayout()
         {
            numberAxis.requestAxisLayout();
         }

         @Override
         public ObjectProperty<Side> sideProperty()
         {
            return numberAxis.sideProperty();
         }

         @Override
         public BooleanProperty autoRangingProperty()
         {
            return numberAxis.autoRangingProperty();
         }

         @Override
         public void invalidateRange(List<Number> data)
         {
            numberAxis.invalidateRange(data);
         }

         @Override
         public DoubleProperty lowerBoundProperty()
         {
            return numberAxis.lowerBoundProperty();
         }

         @Override
         public DoubleProperty upperBoundProperty()
         {
            return numberAxis.upperBoundProperty();
         }

         @Override
         public double getDisplayPosition(double value)
         {
            return numberAxis.getDisplayPosition(value);
         }

         @Override
         public double getValueForDisplay(double displayPosition)
         {
            Number valueForDisplay = numberAxis.getValueForDisplay(displayPosition);
            return valueForDisplay.doubleValue();
         }

         @Override
         public void setForceZeroInRange(boolean value)
         {
            numberAxis.setForceZeroInRange(value);
         }

         @Override
         public DoubleProperty tickUnitProperty()
         {
            return numberAxis.tickUnitProperty();
         }

         @Override
         public BooleanProperty minorTickVisibleProperty()
         {
            return numberAxis.minorTickVisibleProperty();
         }

         @Override
         public ReadOnlyDoubleProperty scaleProperty()
         {
            return numberAxis.scaleProperty();
         }

         @Override
         public ObjectProperty<StringConverter<Number>> tickLabelFormatterProperty()
         {
            return numberAxis.tickLabelFormatterProperty();
         }

         @Override
         public DoubleProperty minorTickLengthProperty()
         {
            return numberAxis.minorTickLengthProperty();
         }

         @Override
         public IntegerProperty minorTickCountProperty()
         {
            return numberAxis.minorTickCountProperty();
         }

         @Override
         public ObjectProperty<String> labelProperty()
         {
            return numberAxis.labelProperty();
         }

         @Override
         public BooleanProperty tickMarkVisibleProperty()
         {
            return numberAxis.tickMarkVisibleProperty();
         }

         @Override
         public DoubleProperty tickLengthProperty()
         {
            return numberAxis.tickLengthProperty();
         }

         @Override
         public BooleanProperty tickLabelsVisibleProperty()
         {
            return numberAxis.tickLabelsVisibleProperty();
         }

         @Override
         public ObjectProperty<Font> tickLabelFontProperty()
         {
            return numberAxis.tickLabelFontProperty();
         }

         @Override
         public ObjectProperty<Paint> tickLabelFillProperty()
         {
            return numberAxis.tickLabelFillProperty();
         }

         @Override
         public DoubleProperty tickLabelGapProperty()
         {
            return numberAxis.tickLabelGapProperty();
         }

         @Override
         public BooleanProperty animatedProperty()
         {
            return numberAxis.animatedProperty();
         }

         @Override
         public DoubleProperty tickLabelRotationProperty()
         {
            return numberAxis.tickLabelRotationProperty();
         }

         @Override
         public void setEffectiveOrientation(Orientation orientation)
         {
            numberAxis.setEffectiveOrientation(orientation);
         }
      };
   }
}

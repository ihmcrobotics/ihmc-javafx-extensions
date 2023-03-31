package us.ihmc.javaFXExtensions.chart;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.chart.FastAxisBase;
import javafx.scene.shape.Rectangle;

public abstract class DynamicXYChart extends DynamicChart
{
   protected final ObjectProperty<FastAxisBase> xAxis = new SimpleObjectProperty<FastAxisBase>(this, "xAxis", null);
   protected final ObjectProperty<FastAxisBase> yAxis = new SimpleObjectProperty<FastAxisBase>(this, "xAxis", null);

   protected final Group plotContent = new Group()
   {
      @Override
      public void requestLayout()
      {
      } // suppress layout requests
   };
   private final Rectangle plotContentClip = new Rectangle();

   public DynamicXYChart(FastAxisBase xAxis, FastAxisBase yAxis)
   {
      this.xAxis.set(xAxis);
      this.yAxis.set(yAxis);

      // add initial content to chart content
      getChartChildren().addAll(plotContent, xAxis.asRegion(), yAxis.asRegion());
      // We don't want plotContent to autoSize or do layout
      plotContent.setAutoSizeChildren(false);
      // setup clipping on plot area
      plotContentClip.setSmooth(false);
      plotContent.setClip(plotContentClip);
      // setup css style classes
      plotContent.getStyleClass().setAll("plot-content");
      // mark plotContent as unmanaged as its preferred size changes do not effect our layout
      plotContent.setManaged(false);

      ChangeListener<? super FastAxisBase> axisChangeListener = (o, oldValue, newValue) ->
      {
         int index = getChartChildren().indexOf(oldValue.asRegion());
         getChartChildren().set(index, newValue.asRegion());
      };
      this.xAxis.addListener(axisChangeListener);
      this.yAxis.addListener(axisChangeListener);
   }

   @Override
   protected void layoutChartChildren(double top, double left, double width, double height)
   {
      updateAxisRange();

      // snap top and left to pixels
      top = snapPositionY(top);
      left = snapPositionX(left);

      FastAxisBase xAxis = xAxisProperty().get();
      FastAxisBase yAxis = yAxisProperty().get();

      // try and work out width and height of axises
      double xAxisWidth = 0;
      double xAxisHeight = 0; // guess x axis height to start with
      double yAxisWidth = 0;
      double yAxisHeight = 0;
      for (int count = 0; count < 5; count++)
      {
         yAxisHeight = Math.max(0, snapSizeY(height - xAxisHeight));
         yAxisWidth = yAxis.prefWidth(yAxisHeight);
         xAxisWidth = Math.max(0, snapSizeX(width - yAxisWidth));
         double newXAxisHeight = xAxis.prefHeight(xAxisWidth);
         if (newXAxisHeight == xAxisHeight)
            break;
         xAxisHeight = newXAxisHeight;
      }
      // round axis sizes up to whole integers to snap to pixel
      xAxisWidth = Math.ceil(xAxisWidth);
      xAxisHeight = Math.ceil(xAxisHeight);
      yAxisWidth = Math.ceil(yAxisWidth);
      yAxisHeight = Math.ceil(yAxisHeight);

      // resize axises
      xAxis.resizeRelocate(left + yAxisWidth, top + yAxisHeight, xAxisWidth, xAxisHeight);
      yAxis.resizeRelocate(left + 1, top, yAxisWidth, yAxisHeight);
      // When the chart is resized, need to specifically call out the axises
      // to lay out as they are unmanaged.
      xAxis.requestAxisLayout();
      xAxis.layout();
      yAxis.requestAxisLayout();
      yAxis.layout();
      // layout plot content
      layoutPlotChildren(top, left, xAxisWidth, yAxisHeight);
      // update clip
      plotContentClip.setX(left);
      plotContentClip.setY(top);
      plotContentClip.setWidth(xAxisWidth + 1);
      plotContentClip.setHeight(yAxisHeight + 1);
      // position plot group, its origin is the bottom left corner of the plot area
      plotContent.setLayoutX(left + yAxisWidth);
      plotContent.setLayoutY(top);
      plotContent.requestLayout(); // Note: not sure this is right, maybe plotContent should be resizeable
   }

   /**
    * Called to update the content of the chart, i.e. this is where the plots are actually drawn.
    *
    * @param top    The top offset from the origin to account for any padding on the chart content
    * @param left   The left offset from the origin to account for any padding on the chart content
    * @param width  The width of the area to layout the chart within
    * @param height The height of the area to layout the chart within
    */
   protected abstract void layoutPlotChildren(double top, double left, double width, double height);

   /**
    * Update the range of the x and y axes.
    */
   protected abstract void updateAxisRange();

   public ObjectProperty<FastAxisBase> xAxisProperty()
   {
      return xAxis;
   }

   public void setXAxis(FastAxisBase xAxis)
   {
      this.xAxis.set(xAxis);
   }

   public FastAxisBase getXAxis()
   {
      return xAxis.get();
   }

   public ObjectProperty<FastAxisBase> yAxisProperty()
   {
      return yAxis;
   }

   public void setYAxis(FastAxisBase yAxis)
   {
      this.yAxis.set(yAxis);
   }

   public FastAxisBase getYAxis()
   {
      return yAxis.get();
   }
}

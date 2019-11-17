package us.ihmc.javaFXExtensions.chart;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.chart.NumberAxis;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

public abstract class DynamicXYChart extends DynamicChart
{
   private final NumberAxis xAxis;
   private final NumberAxis yAxis;

   private final Region plotBackground = new Region();
   private final Group plotArea = new Group()
   {
      @Override
      public void requestLayout()
      {
      } // suppress layout requests
   };
   protected final Group plotContent = new Group();
   private final Rectangle plotAreaClip = new Rectangle();

   public DynamicXYChart(NumberAxis xAxis, NumberAxis yAxis)
   {
      this.xAxis = xAxis;
      this.yAxis = yAxis;

      // add initial content to chart content
      getChartChildren().addAll(plotBackground, plotArea, xAxis, yAxis);
      // We don't want plotArea or plotContent to autoSize or do layout
      plotArea.setAutoSizeChildren(false);
      plotContent.setAutoSizeChildren(false);
      // setup clipping on plot area
      plotAreaClip.setSmooth(false);
      plotArea.setClip(plotAreaClip);
      // add children to plot area
      plotArea.getChildren().addAll(plotContent);
      // setup css style classes
      plotContent.getStyleClass().setAll("plot-content");
      plotBackground.getStyleClass().setAll("chart-plot-background");
      plotBackground.paddingProperty().set(new Insets(0));
      // mark plotContent as unmanaged as its preferred size changes do not effect our layout
      plotContent.setManaged(false);
      plotArea.setManaged(false);
   }

   @Override
   protected void layoutChartChildren(double top, double left, double width, double height)
   {
      updateAxisRange();

      // snap top and left to pixels
      top = snapPosition(top);
      left = snapPosition(left);
      // try and work out width and height of axises
      double xAxisWidth = 0;
      double xAxisHeight = 30; // guess x axis height to start with
      double yAxisWidth = 0;
      double yAxisHeight = 0;
      for (int count = 0; count < 5; count++)
      {
         yAxisHeight = Math.max(0, snapSize(height - xAxisHeight));
         yAxisWidth = yAxis.prefWidth(yAxisHeight);
         xAxisWidth = Math.max(0, snapSize(width - yAxisWidth));
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
      // calc xAxis height
      double xAxisY = top + yAxisHeight;
      // calc yAxis width
      double yAxisX = left + 1;
      left += yAxisWidth;

      // resize axises
      xAxis.resizeRelocate(left, xAxisY, xAxisWidth, xAxisHeight);
      yAxis.resizeRelocate(yAxisX, top, yAxisWidth, yAxisHeight);
      // When the chart is resized, need to specifically call out the axises
      // to lay out as they are unmanaged.
      xAxis.requestAxisLayout();
      xAxis.layout();
      yAxis.requestAxisLayout();
      yAxis.layout();
      // layout plot content
      layoutPlotChildren(top, left, xAxisWidth, yAxisHeight);
      // layout plot background
      plotBackground.resizeRelocate(left, top, xAxisWidth, yAxisHeight);
      // update clip
      plotAreaClip.setX(left);
      plotAreaClip.setY(top);
      plotAreaClip.setWidth(xAxisWidth + 1);
      plotAreaClip.setHeight(yAxisHeight + 1);
      // position plot group, its origin is the bottom left corner of the plot area
      plotContent.setLayoutX(left);
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
}

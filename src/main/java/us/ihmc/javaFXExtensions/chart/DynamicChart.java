package us.ihmc.javaFXExtensions.chart;

import com.sun.javafx.scene.control.skin.Utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.Chart;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * Variation of the base class {@link Chart} that provides a simpler implementation with a tighter
 * layout destined to plot multiple charts in a single window.
 */
public abstract class DynamicChart extends Region
{
   private static final int MIN_WIDTH_TO_LEAVE_FOR_CHART_CONTENT = 100;
   private static final int MIN_HEIGHT_TO_LEAVE_FOR_CHART_CONTENT = 50;

   /**
    * This is the Pane that Chart subclasses use to contain the chart content, It is sized to be inside
    * the chart area leaving space for the title and legend.
    */
   private final Pane chartContent = new Pane()
   {
      @Override
      protected void layoutChildren()
      {
         final double top = snappedTopInset();
         final double left = snappedLeftInset();
         final double bottom = snappedBottomInset();
         final double right = snappedRightInset();
         final double width = getWidth();
         final double height = getHeight();
         final double contentWidth = snapSize(width - (left + right));
         final double contentHeight = snapSize(height - (top + bottom));
         layoutChartChildren(snapPosition(top), snapPosition(left), contentWidth, contentHeight);
      }
   };

   /**
    * The node to display as the Legend. Subclasses can set a node here to be displayed on a side as
    * the legend. If no legend is wanted then this can be set to null
    */
   private final ObjectProperty<Node> legend = new ObjectPropertyBase<Node>()
   {
      private Node old = null;

      @Override
      protected void invalidated()
      {
         Node newLegend = get();
         if (old != null)
            getChildren().remove(old);
         if (newLegend != null)
         {
            getChildren().add(newLegend);
            updateLegendSizeBinding(newLegend);
            newLegend.setVisible(true);
         }
         old = newLegend;
      }

      @Override
      public Object getBean()
      {
         return DynamicChart.this;
      }

      @Override
      public String getName()
      {
         return "legend";
      }
   };

   protected final Node getLegend()
   {
      return legend.getValue();
   }

   protected final void setLegend(Node value)
   {
      legend.setValue(value);
   }

   protected final ObjectProperty<Node> legendProperty()
   {
      return legend;
   }

   protected void updateLegendSizeBinding(Node legend)
   {
      if (legend instanceof FlowPane)
      {
         FlowPane legendFlowPane = (FlowPane) legend;
         legendFlowPane.prefWrapLengthProperty().bind(widthProperty());
      }
      else if (legend instanceof Region)
      {
         Region legendTilePane = (Region) legend;
         legendTilePane.prefWidthProperty().bind(widthProperty());
      }
   }

   /**
    * Modifiable and observable list of all content in the chart. This is where implementations of
    * Chart should add any nodes they use to draw their chart. This excludes the legend and title which
    * are looked after by this class.
    *
    * @return Observable list of plot children
    */
   protected ObservableList<Node> getChartChildren()
   {
      return chartContent.getChildren();
   }

   // -------------- CONSTRUCTOR --------------------------------------------------------------------------------------

   /**
    * Creates a new default Chart instance.
    */
   public DynamicChart()
   {
      getChildren().addAll(chartContent);
      getStyleClass().add("chart");
      chartContent.getStyleClass().add("chart-content");
      // mark chartContent as unmanaged because any changes to its preferred size shouldn't cause a relayout
      chartContent.setManaged(false);
      chartContent.setPadding(Insets.EMPTY);
      setPadding(Insets.EMPTY);
   }

   /** Call this when you know something has changed that needs the chart to be relayed out. */
   protected void requestChartLayout()
   {
      chartContent.requestLayout();
   }

   /**
    * Called to update and layout the chart children available from getChartChildren()
    *
    * @param top    The top offset from the origin to account for any padding on the chart content
    * @param left   The left offset from the origin to account for any padding on the chart content
    * @param width  The width of the area to layout the chart within
    * @param height The height of the area to layout the chart within
    */
   protected abstract void layoutChartChildren(double top, double left, double width, double height);

   /**
    * Invoked during the layout pass to layout this chart and all its content.
    */
   @Override
   protected void layoutChildren()
   {
      double top = snappedTopInset();
      double left = snappedLeftInset();
      double bottom = snappedBottomInset();
      double right = snappedRightInset();
      double width = getWidth();
      double height = getHeight();
      // layout legend
      Node legend = getLegend();
      if (legend != null)
      {
         boolean shouldShowLegend = true;
         double legendHeight = snapSize(legend.prefHeight(width - left - right));
         double legendWidth = Utils.boundedSize(snapSize(legend.prefWidth(legendHeight)), 0, width - left - right);
         double legendLeft = left + (width - left - right - legendWidth) / 2;
         double legendTop = height - bottom - legendHeight;
         legend.resizeRelocate(legendLeft, legendTop, legendWidth, legendHeight);
         if (height - bottom - top - legendHeight < MIN_HEIGHT_TO_LEAVE_FOR_CHART_CONTENT)
            shouldShowLegend = false;
         else
            bottom += legendHeight;
         legend.setVisible(shouldShowLegend);
      }
      // whats left is for the chart content
      chartContent.resizeRelocate(left, top, width - left - right, height - top - bottom);
   }

   /**
    * Charts are sized outside in, user tells chart how much space it has and chart draws inside that.
    * So minimum height is a constant 150.
    */
   @Override
   protected double computeMinHeight(double width)
   {
      return MIN_HEIGHT_TO_LEAVE_FOR_CHART_CONTENT;
   }

   /**
    * Charts are sized outside in, user tells chart how much space it has and chart draws inside that.
    * So minimum width is a constant 200.
    */
   @Override
   protected double computeMinWidth(double height)
   {
      return MIN_WIDTH_TO_LEAVE_FOR_CHART_CONTENT;
   }

   /**
    * Charts are sized outside in, user tells chart how much space it has and chart draws inside that.
    * So preferred width is a constant 500.
    */
   @Override
   protected double computePrefWidth(double height)
   {
      return 500.0;
   }

   /**
    * Charts are sized outside in, user tells chart how much space it has and chart draws inside that.
    * So preferred height is a constant 400.
    */
   @Override
   protected double computePrefHeight(double width)
   {
      return 400.0;
   }
}

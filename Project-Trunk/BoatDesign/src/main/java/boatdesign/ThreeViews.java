package boatdesign;

import bezier.Bezier;
import boatdesign.threeD.BoatBox3D;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsg.SwingUtils.Box3D;
import gsg.SwingUtils.WhiteBoardPanel;
import gsg.SwingUtils.fullui.ThreeDPanelWithWidgets;
import gsg.VectorUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Using default WhiteBoard Writer
 *
 * 2D Bezier example. (<- So so...)
 * With draggable control points (hence the MouseListener, MouseMotionListener).
 *
 * TODO More doc here, about point correlation, number of beziers, etc.
 */
public class ThreeViews {

    private final static String TITLE = "One 3D Bezier Drawing Board";

    private static JFrame frame;
    private ThreeDPanelWithWidgets threeDPanel;
    private final JMenuBar menuBar = new JMenuBar();
    private final JMenu menuFile = new JMenu();
    private final JMenuItem menuFileSpit = new JMenuItem();
    private final JMenuItem menuFileExit = new JMenuItem();
    private final JMenu menuHelp = new JMenu();
    private final JMenuItem menuHelpAbout = new JMenuItem();
    private JLabel topLabel;
    private final JButton refreshButton = new JButton("Refresh Boat Shape"); // Not really useful here.

    private final static int WIDTH = 1024;
    private final static int HEIGHT = 800;

    private static ObjectMapper mapper = new ObjectMapper();
    private static Map<String, Object> initConfig = null;

    /*
     * Some points are correlated.
     * First rail point and bow top
     * First keel point and bow bottom
     * Transom is defined by the last rail point and last keel point.
     * - Transom Control point(s) defined by them as well.
     */
    private List<Bezier.Point3D> railCtrlPoints = new ArrayList<>();
    private List<Bezier.Point3D> keelCtrlPoints = new ArrayList<>();

    // The WhiteBoard instantiations
    private WhiteBoardPanel whiteBoardXY = null; // from above
    private WhiteBoardPanel whiteBoardXZ = null; // side
    private WhiteBoardPanel whiteBoardYZ = null; // facing

    private Box3D box3D = null;

    private JTextPane dataTextArea = null;

    private static ThreeViews instance;

    private void fileSpit_ActionPerformed(ActionEvent ae) {
        System.out.println("Ctrl Points:"); // TODO Keel too
        this.railCtrlPoints.forEach(pt -> {
            System.out.println(String.format("%s", pt));
        });
    }
    private void fileExit_ActionPerformed(ActionEvent ae) {
        System.out.printf("Exit requested %s\n", ae);
        System.exit(0);
    }
    private void helpAbout_ActionPerformed(ActionEvent ae) {
        System.out.printf("Help requested %s\n", ae);
        JOptionPane.showMessageDialog(frame, TITLE, "GSG Help", JOptionPane.PLAIN_MESSAGE);
    }
    private void refreshBoatShape() {
        Thread refresher = new Thread(() -> {
            System.out.println("Starting refresh...");
            // TODO Synchronization
            ((BoatBox3D) this.box3D).refreshData();
            System.out.println("Refresh completed!");
            this.box3D.repaint();
        });
        refresher.start();
    }

    private void refreshData() {

        if (railCtrlPoints.size() > 0) { // TODO Consider keelCtrlPoints too

            // Tell the 3D box
            ((BoatBox3D)this.box3D).setRailCtrlPoints(railCtrlPoints); // The rail.
            // TODO Keel, and correlations

            // Display in textArea
            String content = railCtrlPoints.stream()
                    .map(pt -> String.format("%d: %s", railCtrlPoints.indexOf(pt), pt.toString()))
                    .collect(Collectors.joining("\n"));
            dataTextArea.setText("Control Points:\n" + content);

            /*
             * Prepare data for display
             */

            // Generate the data, the Bézier curve(s).

            // 1 - Rail Ctrl Points
            Bezier bezier = new Bezier(railCtrlPoints);
            List<VectorUtils.Vector3D> bezierPoints = new ArrayList<>(); // The points to display.
            if (railCtrlPoints.size() > 2) { // 3 points minimum.
                for (double t = 0; t <= 1.0; t += 1E-3) {
                    Bezier.Point3D tick = bezier.getBezierPoint(t);
                    // System.out.println(String.format("%.03f: %s", t, tick.toString()));
                    bezierPoints.add(new VectorUtils.Vector3D(tick.getX(), tick.getY(), tick.getZ()));
                }
            }
            double[] xCtrlPoints = railCtrlPoints.stream()
                    .mapToDouble(bp -> bp.getX())
                    .toArray();
            double[] yCtrlPoints = railCtrlPoints.stream()
                    .mapToDouble(bp -> bp.getY())
                    .toArray();
            double[] zCtrlPoints = railCtrlPoints.stream()
                    .mapToDouble(bp -> bp.getZ())
                    .toArray();
            List<VectorUtils.Vector2D> ctrlPtsXYVectors = new ArrayList<>();
            for (int i = 0; i < xCtrlPoints.length; i++) {
                ctrlPtsXYVectors.add(new VectorUtils.Vector2D(xCtrlPoints[i], yCtrlPoints[i]));
            }
            List<VectorUtils.Vector2D> ctrlPtsXZVectors = new ArrayList<>();
            for (int i = 0; i < xCtrlPoints.length; i++) {
                ctrlPtsXZVectors.add(new VectorUtils.Vector2D(xCtrlPoints[i], zCtrlPoints[i]));
            }
            List<VectorUtils.Vector2D> ctrlPtsYZVectors = new ArrayList<>();
            for (int i = 0; i < yCtrlPoints.length; i++) {
                ctrlPtsYZVectors.add(new VectorUtils.Vector2D(yCtrlPoints[i], zCtrlPoints[i]));
            }

            // Curve points
            double[] xData = bezierPoints.stream()
                    .mapToDouble(bp -> bp.getX())
                    .toArray();
            double[] yData = bezierPoints.stream()
                    .mapToDouble(bp -> bp.getY())
                    .toArray();
            double[] zData = bezierPoints.stream()
                    .mapToDouble(bp -> bp.getZ())
                    .toArray();
            List<VectorUtils.Vector2D> dataXYVectors = new ArrayList<>();
            for (int i = 0; i < xData.length; i++) {
                dataXYVectors.add(new VectorUtils.Vector2D(xData[i], yData[i]));
            }
            List<VectorUtils.Vector2D> dataXZVectors = new ArrayList<>();
            for (int i = 0; i < xData.length; i++) {
                dataXZVectors.add(new VectorUtils.Vector2D(xData[i], zData[i]));
            }
            List<VectorUtils.Vector2D> dataYZVectors = new ArrayList<>();
            for (int i = 0; i < yData.length; i++) {
                dataYZVectors.add(new VectorUtils.Vector2D(yData[i], zData[i]));
            }
            // TODO - Keel Ctrl Points

            // TODO Correlated points

            whiteBoardXY.resetAllData();
            whiteBoardXZ.resetAllData();
            whiteBoardYZ.resetAllData();

            // Bezier ctrl points series
            // XY
            WhiteBoardPanel.DataSerie ctrlXYSerie = new WhiteBoardPanel.DataSerie()
                    .data(ctrlPtsXYVectors)
                    .graphicType(WhiteBoardPanel.GraphicType.LINE_WITH_DOTS)
                    .lineThickness(1)
                    .color(Color.ORANGE);
            whiteBoardXY.addSerie(ctrlXYSerie);
            // TODO Keel & correlated points

            // XZ
            WhiteBoardPanel.DataSerie ctrlXZSerie = new WhiteBoardPanel.DataSerie()
                    .data(ctrlPtsXZVectors)
                    .graphicType(WhiteBoardPanel.GraphicType.LINE_WITH_DOTS)
                    .lineThickness(1)
                    .color(Color.ORANGE);
            whiteBoardXZ.addSerie(ctrlXZSerie);
            // TODO Keel & correlated points

            // YZ
            WhiteBoardPanel.DataSerie ctrlYZSerie = new WhiteBoardPanel.DataSerie()
                    .data(ctrlPtsYZVectors)
                    .graphicType(WhiteBoardPanel.GraphicType.LINE_WITH_DOTS)
                    .lineThickness(1)
                    .color(Color.ORANGE);
            whiteBoardYZ.addSerie(ctrlYZSerie);
            // TODO Keel & correlated points

            // Bezier points series
            // XY
            WhiteBoardPanel.DataSerie dataXYSerie = new WhiteBoardPanel.DataSerie()
                    .data(dataXYVectors)
                    .graphicType(WhiteBoardPanel.GraphicType.LINE)
                    .lineThickness(3)
                    .color(Color.BLUE);
            whiteBoardXY.addSerie(dataXYSerie);
            // TODO Keel & correlated points

            // XZ
            WhiteBoardPanel.DataSerie dataXZSerie = new WhiteBoardPanel.DataSerie()
                    .data(dataXZVectors)
                    .graphicType(WhiteBoardPanel.GraphicType.LINE)
                    .lineThickness(3)
                    .color(Color.BLUE);
            whiteBoardXZ.addSerie(dataXZSerie);
            // TODO Keel & correlated points

            // YZ
            WhiteBoardPanel.DataSerie dataYZSerie = new WhiteBoardPanel.DataSerie()
                    .data(dataYZVectors)
                    .graphicType(WhiteBoardPanel.GraphicType.LINE)
                    .lineThickness(3)
                    .color(Color.BLUE);
            whiteBoardYZ.addSerie(dataYZSerie);
            // TODO Keel & correlated points

            // Finally, display it.
            whiteBoardXY.repaint();  // This is for a pure Swing context
            whiteBoardXZ.repaint();  // This is for a pure Swing context
            whiteBoardYZ.repaint();  // This is for a pure Swing context

            this.box3D.repaint();
        } else {
            dataTextArea.setText(" - No points -");
        }
    }

    private void show() {
        this.frame.setVisible(true);
    }

    private void initComponents() {

        if (initConfig != null) {
            Map<String, List<Object>> defaultPoints = (Map)initConfig.get("default-points");
            List<List<Double>> railPoints = (List)defaultPoints.get("rail");
            List<List<Double>> bowPoints = (List)defaultPoints.get("bow");
            List<List<Double>> keelPoints = (List)defaultPoints.get("keel");
            // Rail
            railPoints.forEach(pt -> {
                railCtrlPoints.add(new Bezier.Point3D(pt.get(0), pt.get(1), pt.get(2)));
            });
            // Keel
            keelPoints.forEach(pt -> {
                keelCtrlPoints.add(new Bezier.Point3D(pt.get(0), pt.get(1), pt.get(2)));
            });

        } else {
            // TODO There is a problem when ctrlPoints is empty... Fix it.
            // Initialize [0, 10, 0], [550, 105, 0]
            railCtrlPoints.add(new Bezier.Point3D(0, 10, 0));
            railCtrlPoints.add(new Bezier.Point3D(550, 105, 0));

            keelCtrlPoints.add(new Bezier.Point3D(10d, 0d, -5d));
            keelCtrlPoints.add(new Bezier.Point3D(550d, 0d, 5d));
        }

        // Override defaults (not mandatory)

        // XY
        whiteBoardXY.setAxisColor(Color.BLACK);
        whiteBoardXY.setGridColor(Color.GRAY);
        whiteBoardXY.setForceTickIncrement(50);
        whiteBoardXY.setEnforceXAxisAt(0d);
        whiteBoardXY.setEnforceYAxisAt(0d);

        whiteBoardXY.setWithGrid(true);
        whiteBoardXY.setBgColor(new Color(250, 250, 250, 255));
        whiteBoardXY.setGraphicTitle(null); // "X not equals Y, Y ampl enforced [0, 100]");
        whiteBoardXY.setSize(new Dimension(800, 200));
        whiteBoardXY.setPreferredSize(new Dimension(800, 200));
        whiteBoardXY.setTextColor(Color.RED);
        whiteBoardXY.setTitleFont(new Font("Arial", Font.BOLD | Font.ITALIC, 32));
        whiteBoardXY.setGraphicMargins(30);
        whiteBoardXY.setXEqualsY(true); // false);
        // Enforce Y amplitude
        whiteBoardXY.setForcedMinY(0d);
        whiteBoardXY.setForcedMaxY(150d);

        // XZ
        whiteBoardXZ.setAxisColor(Color.BLACK);
        whiteBoardXZ.setGridColor(Color.GRAY);
        whiteBoardXZ.setForceTickIncrement(50);
        whiteBoardXZ.setEnforceXAxisAt(0d);
        whiteBoardXZ.setEnforceYAxisAt(0d);

        whiteBoardXZ.setWithGrid(true);
        whiteBoardXZ.setBgColor(new Color(250, 250, 250, 255));
        whiteBoardXZ.setGraphicTitle(null); // "X not equals Y, Y ampl enforced [0, 100]");
        whiteBoardXZ.setSize(new Dimension(800, 200));
        whiteBoardXZ.setPreferredSize(new Dimension(800, 200));
        whiteBoardXZ.setTextColor(Color.RED);
        whiteBoardXZ.setTitleFont(new Font("Arial", Font.BOLD | Font.ITALIC, 32));
        whiteBoardXZ.setGraphicMargins(30);
        whiteBoardXZ.setXEqualsY(true); // false);
        // Enforce Y amplitude
        whiteBoardXZ.setForcedMinY(-50d);
        whiteBoardXZ.setForcedMaxY(100d);

        // YZ
        whiteBoardYZ.setAxisColor(Color.BLACK);
        whiteBoardYZ.setGridColor(Color.GRAY);
        whiteBoardYZ.setForceTickIncrement(50);
        whiteBoardYZ.setEnforceXAxisAt(0d);
        whiteBoardYZ.setEnforceYAxisAt(0d);

        whiteBoardYZ.setWithGrid(true);
        whiteBoardYZ.setBgColor(new Color(250, 250, 250, 255));
        whiteBoardYZ.setGraphicTitle(null); // "X not equals Y, Y ampl enforced [0, 100]");
        whiteBoardYZ.setSize(new Dimension(400, 200));
        whiteBoardYZ.setPreferredSize(new Dimension(400, 200));
        whiteBoardYZ.setTextColor(Color.RED);
        whiteBoardYZ.setTitleFont(new Font("Arial", Font.BOLD | Font.ITALIC, 32));
        whiteBoardYZ.setGraphicMargins(30);
        whiteBoardYZ.setXEqualsY(true); // false);
        // Enforce Y amplitude
        whiteBoardYZ.setForcedMinY(-50d);
        whiteBoardYZ.setForcedMaxY(100d);

        // ThreeViewsV2 instance = this;

        whiteBoardXY.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
//                System.out.println("Click on whiteboard");
                if (SwingUtilities.isRightMouseButton(e)) { // e.isPopupTrigger()) { // Right-click
                    Bezier.Point3D closePoint = getClosePoint(e, whiteBoardXY, Orientation.XY);
                    if (closePoint != null) {
                        BezierPopup popup = new BezierPopup(instance, closePoint);
                        popup.show(whiteBoardXY, e.getX(), e.getY());
                    }
                } else {
                    // Regular click.
                    // Drop point here. Where is the list
                    String response = JOptionPane.showInputDialog(
                            frame,
                            String.format("Where to insert new point (index in the list [0..%d]) ?", railCtrlPoints.size()),
                            "Add Control Point",
                            JOptionPane.QUESTION_MESSAGE);
//                  System.out.println("Response:" + response);
                    if (response != null && !response.isEmpty()) {
                        try {
                            int newIndex = Integer.parseInt(response);
                            Function<Integer, Double> canvasToSpaceXTransformer = whiteBoardXY.getCanvasToSpaceXTransformer();
                            Function<Integer, Double> canvasToSpaceYTransformer = whiteBoardXY.getCanvasToSpaceYTransformer();
                            int height = whiteBoardXY.getHeight();
                            if (canvasToSpaceXTransformer != null && canvasToSpaceYTransformer != null) {
                                double newX = canvasToSpaceXTransformer.apply(e.getX());
                                double newY = canvasToSpaceYTransformer.apply(height - e.getY());
//                      System.out.printf("Point dragged to %f / %f\n", newX, newY);
                                Bezier.Point3D point3D = new Bezier.Point3D().x(newX).y(newY);
                                List<Bezier.Point3D> newList = new ArrayList<>();
                                for (int i=0; i<newIndex; i++) {
                                    newList.add(railCtrlPoints.get(i));
                                }
                                newList.add(point3D);
                                for (int i = newIndex; i< railCtrlPoints.size(); i++) {
                                    newList.add(railCtrlPoints.get(i));
                                }
                                railCtrlPoints = newList;
                                System.out.printf("List now has %d elements.\n", railCtrlPoints.size());
                                refreshData();
                            }
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
//        System.out.printf("Mouse clicked x: %d y: %d\n", e.getX(), e.getY());
                Bezier.Point3D closePoint = getClosePoint(e, whiteBoardXY, Orientation.XY);
                if (closePoint != null) {
//            System.out.println("Found it!");
                    whiteBoardXY.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        });
        whiteBoardXY.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
//                System.out.println("Dragged on whiteboard");
                if (closestPointIndex > -1) {
                    Function<Integer, Double> canvasToSpaceXTransformer = whiteBoardXY.getCanvasToSpaceXTransformer();
                    Function<Integer, Double> canvasToSpaceYTransformer = whiteBoardXY.getCanvasToSpaceYTransformer();
                    int height = whiteBoardXY.getHeight();
                    if (canvasToSpaceXTransformer != null && canvasToSpaceYTransformer != null) {
                        double newX = canvasToSpaceXTransformer.apply(e.getX());
                        double newY = canvasToSpaceYTransformer.apply(height - e.getY());
//                System.out.printf("Point dragged to %f / %f\n", newX, newY);
                        Bezier.Point3D point3D = railCtrlPoints.get(closestPointIndex);
                        point3D.x(newX).y(newY);
                        refreshData();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
//                System.out.println("Moved on whiteboard (MotionListener)");
                Bezier.Point3D closePoint = getClosePoint(e, whiteBoardXY, Orientation.XY);
                if (closePoint != null) {
                    whiteBoardXY.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    closestPointIndex = railCtrlPoints.indexOf(closePoint);
                } else {
                    whiteBoardXY.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    closestPointIndex = -1;
                }

            }
        });

        whiteBoardXZ.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
//                System.out.println("Click on whiteboard");
                if (SwingUtilities.isRightMouseButton(e)) { // e.isPopupTrigger()) { // Right-click
                    Bezier.Point3D closePoint = getClosePoint(e, whiteBoardXZ, Orientation.XZ);
                    if (closePoint != null) {
                        BezierPopup popup = new BezierPopup(instance, closePoint);
                        popup.show(whiteBoardXZ, e.getX(), e.getY());
                    }
                } else {
                    // Regular click.
                    // Drop point here. Where is the list
                    String response = JOptionPane.showInputDialog(
                            frame,
                            String.format("Where to insert new point (index in the list [0..%d]) ?", railCtrlPoints.size()),
                            "Add Control Point",
                            JOptionPane.QUESTION_MESSAGE);
    //              System.out.println("Response:" + response);
                    if (response != null && !response.isEmpty()) {
                        try {
                            int newIndex = Integer.parseInt(response);
                            Function<Integer, Double> canvasToSpaceXTransformer = whiteBoardXZ.getCanvasToSpaceXTransformer();
                            Function<Integer, Double> canvasToSpaceYTransformer = whiteBoardXZ.getCanvasToSpaceYTransformer();
                            int height = whiteBoardXZ.getHeight();
                            if (canvasToSpaceXTransformer != null && canvasToSpaceYTransformer != null) {
                                double newX = canvasToSpaceXTransformer.apply(e.getX());
                                double newZ = canvasToSpaceYTransformer.apply(height - e.getY());
//                      System.out.printf("Point dragged to %f / %f\n", newX, newY);
                                Bezier.Point3D point3D = new Bezier.Point3D().x(newX).z(newZ);
                                List<Bezier.Point3D> newList = new ArrayList<>();
                                for (int i=0; i<newIndex; i++) {
                                    newList.add(railCtrlPoints.get(i));
                                }
                                newList.add(point3D);
                                for (int i = newIndex; i< railCtrlPoints.size(); i++) {
                                    newList.add(railCtrlPoints.get(i));
                                }
                                railCtrlPoints = newList;
                                System.out.printf("List now has %d elements.\n", railCtrlPoints.size());
                                refreshData();
                            }
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
//        System.out.printf("Mouse clicked x: %d y: %d\n", e.getX(), e.getY());
                Bezier.Point3D closePoint = getClosePoint(e, whiteBoardXZ, Orientation.XZ);
                if (closePoint != null) {
//            System.out.println("Found it!");
                    whiteBoardXY.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        });
        whiteBoardXZ.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
//                System.out.println("Dragged on whiteboard");
                if (closestPointIndex > -1) {
                    Function<Integer, Double> canvasToSpaceXTransformer = whiteBoardXZ.getCanvasToSpaceXTransformer();
                    Function<Integer, Double> canvasToSpaceYTransformer = whiteBoardXZ.getCanvasToSpaceYTransformer();
                    int height = whiteBoardXZ.getHeight();
                    if (canvasToSpaceXTransformer != null && canvasToSpaceYTransformer != null) {
                        double newX = canvasToSpaceXTransformer.apply(e.getX());
                        double newZ = canvasToSpaceYTransformer.apply(height - e.getY());
//                System.out.printf("Point dragged to %f / %f\n", newX, newY);
                        Bezier.Point3D point3D = railCtrlPoints.get(closestPointIndex);
                        point3D.x(newX).z(newZ);
                        refreshData();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
//                System.out.println("Moved on whiteboard (MotionListener)");
                Bezier.Point3D closePoint = getClosePoint(e, whiteBoardXZ, Orientation.XZ);
                if (closePoint != null) {
                    whiteBoardXZ.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    closestPointIndex = railCtrlPoints.indexOf(closePoint);
                } else {
                    whiteBoardXZ.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    closestPointIndex = -1;
                }

            }
        });

        whiteBoardYZ.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
//                System.out.println("Click on whiteboard");
                if (SwingUtilities.isRightMouseButton(e)) { // e.isPopupTrigger()) { // Right-click
                    Bezier.Point3D closePoint = getClosePoint(e, whiteBoardYZ, Orientation.YZ);
                    if (closePoint != null) {
                        BezierPopup popup = new BezierPopup(instance, closePoint);
                        popup.show(whiteBoardYZ, e.getX(), e.getY());
                    }
                } else {
                    // Regular click.
                    // Drop point here. Where is the list
                    String response = JOptionPane.showInputDialog(
                            frame,
                            String.format("Where to insert new point (index in the list [0..%d]) ?", railCtrlPoints.size()),
                            "Add Control Point",
                            JOptionPane.QUESTION_MESSAGE);
//                  System.out.println("Response:" + response);
                    if (response != null && !response.isEmpty()) {
                        try {
                            int newIndex = Integer.parseInt(response);
                            Function<Integer, Double> canvasToSpaceXTransformer = whiteBoardYZ.getCanvasToSpaceXTransformer();
                            Function<Integer, Double> canvasToSpaceYTransformer = whiteBoardYZ.getCanvasToSpaceYTransformer();
                            int height = whiteBoardYZ.getHeight();
                            if (canvasToSpaceXTransformer != null && canvasToSpaceYTransformer != null) {
                                double newY = canvasToSpaceXTransformer.apply(e.getX());
                                double newZ = canvasToSpaceYTransformer.apply(height - e.getY());
//                      System.out.printf("Point dragged to %f / %f\n", newX, newY);
                                Bezier.Point3D point3D = new Bezier.Point3D().y(newY).z(newZ);
                                List<Bezier.Point3D> newList = new ArrayList<>();
                                for (int i=0; i<newIndex; i++) {
                                    newList.add(railCtrlPoints.get(i));
                                }
                                newList.add(point3D);
                                for (int i = newIndex; i< railCtrlPoints.size(); i++) {
                                    newList.add(railCtrlPoints.get(i));
                                }
                                railCtrlPoints = newList;
                                System.out.printf("List now has %d elements.\n", railCtrlPoints.size());
                                refreshData();
                            }
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
//        System.out.printf("Mouse clicked x: %d y: %d\n", e.getX(), e.getY());
                Bezier.Point3D closePoint = getClosePoint(e, whiteBoardYZ, Orientation.YZ);
                if (closePoint != null) {
//            System.out.println("Found it!");
                    whiteBoardXY.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        });
        whiteBoardYZ.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
//                System.out.println("Dragged on whiteboard");
                if (closestPointIndex > -1) {
                    Function<Integer, Double> canvasToSpaceXTransformer = whiteBoardYZ.getCanvasToSpaceXTransformer();
                    Function<Integer, Double> canvasToSpaceYTransformer = whiteBoardYZ.getCanvasToSpaceYTransformer();
                    int height = whiteBoardYZ.getHeight();
                    if (canvasToSpaceXTransformer != null && canvasToSpaceYTransformer != null) {
                        double newY = canvasToSpaceXTransformer.apply(e.getX());
                        double newZ = canvasToSpaceYTransformer.apply(height - e.getY());
//                System.out.printf("Point dragged to %f / %f\n", newX, newY);
                        Bezier.Point3D point3D = railCtrlPoints.get(closestPointIndex);
                        point3D.y(newY).z(newZ);
                        refreshData();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
//                System.out.println("Moved on whiteboard (MotionListener)");
                Bezier.Point3D closePoint = getClosePoint(e, whiteBoardYZ, Orientation.YZ);
                if (closePoint != null) {
                    whiteBoardYZ.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    closestPointIndex = railCtrlPoints.indexOf(closePoint);
                } else {
                    whiteBoardYZ.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    closestPointIndex = -1;
                }

            }
        });

        // The JFrame
        frame = new JFrame(TITLE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
//        System.out.printf("Default frame width %d height %d %n", frameSize.width, frameSize.height);
        frameSize.height = Math.min(frameSize.height, screenSize.height);
        frameSize.width  = Math.min(frameSize.width, screenSize.width);
        if (frameSize.width == 0 || frameSize.height == 0) {
            frameSize = new Dimension(WIDTH, HEIGHT + 50 + 10); // 50: ... menu, title bar, etc. 10: button
            frame.setSize(frameSize);
        }
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        refreshButton.addActionListener(e -> refreshBoatShape());

        frame.setJMenuBar(menuBar);
        frame.getContentPane().setLayout(new BorderLayout());
        menuFile.setText("File");
        menuFileSpit.setText("Spit out points");
        menuFileSpit.addActionListener(ae -> fileSpit_ActionPerformed(ae));
        menuFileExit.setText("Exit");
        menuFileExit.addActionListener(ae -> fileExit_ActionPerformed(ae));
        menuHelp.setText("Help");
        menuHelpAbout.setText("About");
        menuHelpAbout.addActionListener(ae -> helpAbout_ActionPerformed(ae));
        menuFile.add(menuFileSpit);
        menuFile.add(menuFileExit);
        menuBar.add(menuFile);
        menuHelp.add(menuHelpAbout);
        menuBar.add(menuHelp);

        topLabel = new JLabel(" " + TITLE); // Ugly left padding!
        topLabel.setFont(new Font("Courier New", Font.ITALIC | Font.BOLD, 16));
        frame.getContentPane().add(topLabel, BorderLayout.NORTH);

        // >> HERE: Add the WitheBoard to the JFrame
        JPanel whiteBoardsPanel = new JPanel(new GridBagLayout());

//        JLabel label1 = new JLabel("Label 1");
//        JLabel label2 = new JLabel("Label 2");
//        JLabel label3 = new JLabel("Label 3");

        JPanel ctrlPointsPanel = new JPanel(new BorderLayout());
        ctrlPointsPanel.setBorder(BorderFactory.createTitledBorder("Data Placeholder"));
        dataTextArea = new JTextPane();
        dataTextArea.setFont(new Font("Courier New", Font.PLAIN, 14));
        dataTextArea.setPreferredSize(new Dimension(300, 600));
        JScrollPane dataScrollPane = new JScrollPane(dataTextArea);

        ctrlPointsPanel.add(dataScrollPane, BorderLayout.NORTH);

        whiteBoardsPanel.add(whiteBoardXZ,         // Side
                new GridBagConstraints(0,
                        0,
                        1,
                        1,
                        0.0,
                        0.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        whiteBoardsPanel.add(whiteBoardYZ,       // Face
                new GridBagConstraints(0,
                        1,
                        1,
                        1,
                        0.0,
                        0.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.NONE,
                        new Insets(50, 0, 0, 0), 0, 10));
        whiteBoardsPanel.add(whiteBoardXY,       // From above
                new GridBagConstraints(0,
                        2,
                        1,
                        1,
                        0.0,
                        0.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));

        whiteBoardsPanel.add(threeDPanel, // ctrlPointsPanel,
                new GridBagConstraints(1,
                        0,
                        1,
                        3,
                        1.0,
                        0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

        JScrollPane jScrollPane = new JScrollPane(whiteBoardsPanel);
        frame.getContentPane().add(jScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.add(refreshButton, new GridBagConstraints(0,
                0,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 10), 0, 0));

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
//        frame.pack();
    }

    public ThreeViews() {
        instance = this;

        this.whiteBoardXY = new WhiteBoardPanel(); // from above
        this.whiteBoardXZ = new WhiteBoardPanel(); // side
        this.whiteBoardYZ = new WhiteBoardPanel(); // facing

        /*BoatBox3D*/ this.box3D = new BoatBox3D();
        threeDPanel = new ThreeDPanelWithWidgets(box3D);
    }

    enum Orientation {
        XY, XZ, YZ
    }

    // Find the control point close to the mouse pointer.
    private Bezier.Point3D getClosePoint(MouseEvent me, WhiteBoardPanel wbp, Orientation orientation) {
        Bezier.Point3D closePoint = null;
        Function<Double, Integer> spaceToCanvasXTransformer = wbp.getSpaceToCanvasXTransformer();
        Function<Double, Integer> spaceToCanvasYTransformer = wbp.getSpaceToCanvasYTransformer();
        int height = wbp.getHeight();
        if (spaceToCanvasXTransformer != null && spaceToCanvasYTransformer != null) {
            for (Bezier.Point3D ctrlPt : railCtrlPoints) {
                double ptX = ctrlPt.getX();
                double ptY = ctrlPt.getY();
                if (orientation == Orientation.XZ) {
                    ptX = ctrlPt.getX();
                    ptY = ctrlPt.getZ();
                } else if (orientation == Orientation.YZ) {
                    ptX = ctrlPt.getY();
                    ptY = ctrlPt.getZ();
                }
                Integer canvasX = spaceToCanvasXTransformer.apply(ptX);
                Integer canvasY = spaceToCanvasYTransformer.apply(ptY);
                if (Math.abs(me.getX() - canvasX) < 5 && Math.abs(me.getY() - (height - canvasY)) < 5) {
//                    System.out.printf("DeltaX: %d, DeltaY: %d\n", Math.abs(e.getX() - canvasX), Math.abs(e.getY() - (height - canvasY)));
//                    System.out.printf("Close to %s\n", ctrlPt);
                    closePoint = ctrlPt;
                    break;
                }
            }
        }
        return closePoint;
    }

    private int closestPointIndex = -1;

    static class BezierPopup extends JPopupMenu
            implements ActionListener,
            PopupMenuListener {
        private JMenuItem deleteMenuItem;
        private JMenuItem editMenuItem;

        private ThreeViews parent;
        private Bezier.Point3D closePoint;

        private final static String DELETE_CTRL_POINT = "Delete Ctrl Point";
        private final static String EDIT_CTRL_POINT = "Edit Ctrl Point";

        public BezierPopup(ThreeViews parent, Bezier.Point3D closePoint) {
            super();
            this.parent = parent;
            this.closePoint = closePoint;

            deleteMenuItem = new JMenuItem(DELETE_CTRL_POINT);
            this.add(deleteMenuItem);
            deleteMenuItem.addActionListener(this);
            editMenuItem = new JMenuItem(EDIT_CTRL_POINT);
            this.add(editMenuItem);
            editMenuItem.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getActionCommand().equals(DELETE_CTRL_POINT)) {
                if (this.closePoint != null) {
                    this.parent.railCtrlPoints.remove(this.closePoint);
                    this.parent.refreshData();
                }
            } else if (event.getActionCommand().equals(EDIT_CTRL_POINT)) {
                if (this.closePoint != null) {
                    CtrlPointEditor cpEditor = new CtrlPointEditor(this.closePoint.getX(), this.closePoint.getY(), this.closePoint.getZ());
                    int response = JOptionPane.showConfirmDialog(frame,
                            cpEditor,
                            "Edit Control Point",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
                    if (response == JOptionPane.OK_OPTION) {
                        // Update Control Point in the list
                        double x = cpEditor.getXValue();
                        double y = cpEditor.getYValue();
                        double z = cpEditor.getZValue();
                        int idx = this.parent.railCtrlPoints.indexOf(this.closePoint);
                        this.parent.railCtrlPoints.get(idx).x(x).y(y).z(z);
                    }
                    this.parent.refreshData();
                }
            }
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }
    }

    static class CtrlPointEditor extends JPanel {
        private transient Border border = BorderFactory.createEtchedBorder();
        private GridBagLayout layoutMain = new GridBagLayout();
        private JLabel xLabel = new JLabel("X");
        private final JFormattedTextField xValue = new JFormattedTextField(new DecimalFormat("#0.0000"));
        private JLabel yLabel = new JLabel("Y");
        private final JFormattedTextField yValue = new JFormattedTextField(new DecimalFormat("#0.0000"));
        private JLabel zLabel = new JLabel("Z");
        private final JFormattedTextField zValue = new JFormattedTextField(new DecimalFormat("#0.0000"));

        private double x, y, z;

        public CtrlPointEditor(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            try {
                this.jbInit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public double getXValue() {
            return Double.parseDouble(this.xValue.getText());
        }

        public double getYValue() {
            return Double.parseDouble(this.yValue.getText());
        }

        public double getZValue() {
            return Double.parseDouble(this.zValue.getText());
        }

        private void jbInit() {
            this.setLayout(layoutMain);
            this.setBorder(border);

            xValue.setHorizontalAlignment(SwingConstants.RIGHT);
            xValue.setPreferredSize(new Dimension(80, 20));
            this.add(xLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 15, 0, 15), 0, 0));
            this.add(xValue, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 15, 0, 15), 0, 0));
            yValue.setHorizontalAlignment(SwingConstants.RIGHT);
            yValue.setPreferredSize(new Dimension(80, 20));
            this.add(yLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 15, 0, 15), 0, 0));
            this.add(yValue, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 15, 0, 15), 0, 0));
            zValue.setHorizontalAlignment(SwingConstants.RIGHT);
            zValue.setPreferredSize(new Dimension(80, 20));
            this.add(zLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 15, 0, 15), 0, 0));
            this.add(zValue, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 15, 0, 15), 0, 0));

            this.xValue.setValue(this.x);
            this.yValue.setValue(this.y);
            this.zValue.setValue(this.z);
        }
    }

    public static void main(String... args) {

        try {
            if (System.getProperty("swing.defaultlaf") == null) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("----------------------------------------------");
        System.out.printf("Running from folder %s\n", System.getProperty("user.dir"));
        System.out.printf("Java Version %s\n", System.getProperty("java.version"));
        System.out.println("----------------------------------------------");

        File config = new File("init.json");
        if (config.exists()) {
            try {
                URL configResource = config.toURI().toURL();
                initConfig = mapper.readValue(configResource.openStream(), Map.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Warning: no init.json was found.");
        }

        ThreeViews thisThing = new ThreeViews();// This one has instantiated the white boards

        thisThing.initComponents();

        thisThing.refreshData(); // Display data the first time.
        thisThing.show();
    }
}

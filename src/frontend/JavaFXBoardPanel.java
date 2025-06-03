package frontend;

import backend.model.Board;
import backend.model.BoardShape;
import backend.model.Piece;
import backend.model.Position;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import java.util.List;

public class JavaFXBoardPanel extends AbstractBoardPanel<Point2D> {
    private Pane pane;
    private Canvas canvas;
    
    public JavaFXBoardPanel(Board board) {
        super(board);
        
        pane = new Pane();
        pane.setPrefSize(650, 650);
        pane.setStyle("-fx-background-color: white;");
        
        canvas = new Canvas(650, 650);
        pane.getChildren().add(canvas);
        
        // 크기 변경 시 다시 그리기
        pane.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            initializeCoords();
            refresh();
        });
        pane.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            initializeCoords();
            refresh();
        });
        
        initializeCoords();
        refresh();
    }
    
    public Pane getPane() {
        return pane;
    }
    
    @Override
    public void refresh() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawBoard();
        drawPieces();
    }
    
    @Override
    protected void initializeTraditionalCoords() {
        calculateTraditionalLayout();
    }
    
    @Override
    protected void initializePentagonCoords() {
        calculatePentagonLayout();
    }
    
    @Override
    protected void initializeHexagonCoords() {
        calculateHexagonLayout();
    }
    
    @Override
    protected Point2D createPoint(double x, double y) {
        return new Point2D(x, y);
    }
    
    @Override
    protected Point2D midpoint(Point2D a, Point2D b) {
        return lerp(a, b, 0.5);
    }
    
    @Override
    protected Point2D lerp(Point2D a, Point2D b, double t) {
        return new Point2D(
            a.getX() + (b.getX() - a.getX()) * t,
            a.getY() + (b.getY() - a.getY()) * t
        );
    }
    
    @Override
    protected double getComponentWidth() {
        return pane.getWidth();
    }
    
    @Override
    protected double getComponentHeight() {
        return pane.getHeight();
    }
    
    private void drawBoard() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2.0);
        
        switch (boardShape) {
            case TRADITIONAL:
                drawTraditionalBoard(gc);
                break;
            case PENTAGON:
                drawPentagonBoard(gc);
                break;
            case HEXAGON:
                drawHexagonBoard(gc);
                break;
        }
        
        drawPaths(gc);
        drawNodes(gc);
        drawDirectionArrows(gc);
    }
    
    private void drawPieces() {
        if (board == null) return;
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        for (Position pos : coords.keySet()) {
            List<Piece> pieces = board.getPiecesAt(pos);
            if (pieces != null) {
                for (int i = 0; i < pieces.size(); i++) {
                    Piece piece = pieces.get(i);
                    if (piece != null) {
                        Point2D point = coords.get(pos);
                        if (point != null) {
                            gc.setFill(Color.valueOf(getPlayerColor(piece.getOwner().getName())));
                            gc.fillOval(point.getX() - 15 + i * 12, point.getY() - 15, 30, 30);
                            gc.setStroke(Color.BLACK);
                            gc.strokeOval(point.getX() - 15 + i * 12, point.getY() - 15, 30, 30);
                        }
                    }
                }
            }
        }
    }
    
    private void drawTraditionalBoard(GraphicsContext gc) {
        // 사각형 윤곽선
        if (coords.containsKey(Position.POS_0) && coords.containsKey(Position.POS_10)) {
            Point2D p0 = coords.get(Position.POS_0);
            Point2D p5 = coords.get(Position.POS_5);
            Point2D p10 = coords.get(Position.POS_10);
            Point2D p15 = coords.get(Position.POS_15);
            
            gc.setFill(Color.rgb(240, 240, 240, 0.5));
            double[] xPoints = {p0.getX(), p5.getX(), p10.getX(), p15.getX()};
            double[] yPoints = {p0.getY(), p5.getY(), p10.getY(), p15.getY()};
            
            gc.fillPolygon(xPoints, yPoints, 4);
        }
    }
    
    private void drawPentagonBoard(GraphicsContext gc) {
        // 오각형 윤곽선
        gc.setFill(Color.rgb(240, 240, 240, 0.5));
        double[] xPointsPent = new double[5];
        double[] yPointsPent = new double[5];
        
        for (int i = 0; i < 5; i++) {
            Position pos = Position.valueOf("POS_" + (i * 5)); // 각 꼭지점 (0, 5, 10, 15, 20)
            Point2D p = coords.get(pos);
            
            if (p != null) {
                xPointsPent[i] = p.getX();
                yPointsPent[i] = p.getY();
            }
        }
        gc.fillPolygon(xPointsPent, yPointsPent, 5);
    }
    
    private void drawHexagonBoard(GraphicsContext gc) {
        // 육각형 윤곽선
        gc.setFill(Color.rgb(240, 240, 240, 0.5));
        double[] xPointsHex = new double[6];
        double[] yPointsHex = new double[6];
        
        for (int i = 0; i < 6; i++) {
            Position pos = Position.valueOf("POS_" + (i * 5)); // 각 꼭지점 (0, 5, 10, 15, 20, 25)
            Point2D p = coords.get(pos);
            
            if (p != null) {
                xPointsHex[i] = p.getX();
                yPointsHex[i] = p.getY();
            }
        }
        gc.fillPolygon(xPointsHex, yPointsHex, 6);
    }
    
    private void drawPaths(GraphicsContext gc) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1.5);
        
        // 외곽 경로 그리기
        switch (boardShape) {
            case TRADITIONAL:
                drawOuterPathTraditional(gc);
                break;
            case PENTAGON:
                drawOuterPathPentagon(gc);
                break;
            case HEXAGON:
                drawOuterPathHexagon(gc);
                break;
        }
        
        // 대각선 경로 그리기
        drawDiagonalPaths(gc);
    }
    
    private void drawOuterPathTraditional(GraphicsContext gc) {
        // 외곽 경로
        Position[] outerPath = new Position[20];
        for (int i = 0; i < 20; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(gc, outerPath);
        
        // 마지막과 첫 번째 연결
        Point2D first = coords.get(Position.POS_0);
        Point2D last = coords.get(Position.POS_19);
        if (first != null && last != null) {
            gc.strokeLine(last.getX(), last.getY(), first.getX(), first.getY());
        }
    }
    
    private void drawOuterPathPentagon(GraphicsContext gc) {
        // 외곽 경로
        Position[] outerPath = new Position[25];
        for (int i = 0; i < 25; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(gc, outerPath);
        
        // 첫 번째 노드와 마지막 노드 연결
        Point2D first = coords.get(Position.POS_0);
        Point2D last = coords.get(Position.POS_24);
        if (first != null && last != null) {
            gc.strokeLine(last.getX(), last.getY(), first.getX(), first.getY());
        }
    }
    
    private void drawOuterPathHexagon(GraphicsContext gc) {
        // 외곽 경로
        Position[] outerPath = new Position[30];
        for (int i = 0; i < 30; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(gc, outerPath);
        
        // 첫 번째 노드와 마지막 노드 연결
        Point2D first = coords.get(Position.POS_0);
        Point2D last = coords.get(Position.POS_29);
        if (first != null && last != null) {
            gc.strokeLine(last.getX(), last.getY(), first.getX(), first.getY());
        }
    }
    
    private void drawDiagonalPaths(GraphicsContext gc) {
        switch (boardShape) {
            case TRADITIONAL:
                // 대각선 경로 A
                drawPath(gc,
                        Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4, Position.POS_15
                );
                
                // 대각선 경로 B
                drawPath(gc,
                        Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4, Position.POS_0
                );
                break;
                
            case PENTAGON:
                // 대각선 A: POS_5 -> CENTER -> POS_20
                drawPath(gc,
                        Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4, Position.POS_20
                );
                
                // 대각선 B: POS_10 -> CENTER -> END
                drawPath(gc,
                        Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4, Position.POS_0
                );
                
                // 대각선 C: POS_15 -> CENTER -> POS_0
                drawPath(gc,
                        Position.POS_15, Position.DIA_C1, Position.DIA_C2, Position.CENTER,
                        Position.DIA_C3, Position.DIA_C4, Position.POS_0
                );
                break;
                
            case HEXAGON:
                // 대각선 A: POS_5 -> CENTER -> POS_20
                drawPath(gc,
                        Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4, Position.POS_20
                );
                
                // 대각선 B: POS_10 -> CENTER -> POS_25
                drawPath(gc,
                        Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4, Position.POS_25
                );
                
                // 대각선 C: POS_15 -> CENTER -> END
                drawPath(gc,
                        Position.POS_15, Position.DIA_C1, Position.DIA_C2, Position.CENTER,
                        Position.DIA_C3, Position.DIA_C4, Position.POS_0
                );
                break;
        }
    }
    
    private void drawPath(GraphicsContext gc, Position... pts) {
        for (int i = 0; i < pts.length - 1; i++) {
            Point2D a = coords.get(pts[i]);
            Point2D b = coords.get(pts[i+1]);
            if (a != null && b != null) {
                gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
        }
    }
    
    private void drawNodes(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        for (Point2D p : coords.values()) {
            gc.fillOval(p.getX() - 12, p.getY() - 12, 24, 24);
            gc.strokeOval(p.getX() - 12, p.getY() - 12, 24, 24);
        }
    }
    
    private void drawDirectionArrows(GraphicsContext gc) {
        gc.setStroke(Color.DARKGREEN);
        gc.setFill(Color.DARKGREEN);
        gc.setLineWidth(2.0);
        
        Point2D start = coords.get(Position.POS_0);
        Point2D next = coords.get(Position.POS_1);
        
        if (start != null && next != null) {
            drawArrow(gc, start, next);
        }
    }
    
    private void drawArrow(GraphicsContext gc, Point2D from, Point2D to) {
        if (from == null || to == null) return;
        
        // 화살표 길이와 방향 계산
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        
        // 화살표 축소 (시작과 끝 노드 사이에 딱 맞게)
        double scaleFactor = 0.7; // 화살표 길이 비율
        double arrowX = from.getX() + dx * scaleFactor;
        double arrowY = from.getY() + dy * scaleFactor;
        
        // 화살표 머리 크기
        double arrowSize = 10;
        
        // 화살표 방향 각도
        double angle = Math.atan2(dy, dx);
        
        // 화살표 몸통 그리기
        gc.strokeLine(from.getX(), from.getY(), arrowX, arrowY);
        
        // 화살표 머리 그리기
        gc.save();
        gc.translate(arrowX, arrowY);
        gc.rotate(Math.toDegrees(angle));
        
        // 삼각형 화살표 머리
        double[] xPoints = {0, -arrowSize, -arrowSize};
        double[] yPoints = {0, -arrowSize/2, arrowSize/2};
        gc.fillPolygon(xPoints, yPoints, 3);
        
        gc.restore();
    }
}
package frontend;

import backend.model.Board;
import backend.model.BoardShape;
import backend.model.Piece;
import backend.model.Position;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX 버전의 윷놀이 보드 패널
 * Swing BoardPanel과 동일한 기능을 제공
 */
public class JavaFXBoardPanel extends Pane {
    private final Board board;
    private final Map<Position, Point2D> coords = new HashMap<>();
    private BoardShape boardShape = BoardShape.TRADITIONAL;
    private Canvas canvas;

    public JavaFXBoardPanel(Board board) {
        this.board = board;
        setPrefSize(650, 650);
        setStyle("-fx-background-color: white;");
        
        canvas = new Canvas(650, 650);
        getChildren().add(canvas);
        
        // 크기 변경 시 다시 그리기
        widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            initializeCoords();
            drawBoard();
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            initializeCoords();
            drawBoard();
        });
        
        initializeCoords();
        drawBoard();
    }
    
    public void setBoardShape(BoardShape shape) {
        this.boardShape = shape;
        initializeCoords();
        drawBoard();
    }
    
    public BoardShape getBoardShape() {
        return this.boardShape;
    }
    
    private void initializeCoords() {
        coords.clear();
        
        switch (boardShape) {
            case TRADITIONAL:
                initializeTraditionalCoords();
                break;
            case PENTAGON:
                initializePentagonCoords();
                break;
            case HEXAGON:
                initializeHexagonCoords();
                break;
        }
    }

    private void initializeTraditionalCoords() {
        // 보드의 크기를 패널 크기에 맞게 조절
        double width = getWidth() > 0 ? getWidth() : 650;
        double height = getHeight() > 0 ? getHeight() : 650;
        
        // 여백을 화면 크기의 10%로 설정
        double margin = Math.min(width, height) / 10;
        
        // 격자 간격을 화면 크기에 맞게 조절
        double gridSize = Math.min(width - 2 * margin, height - 2 * margin) / 5;
        
        // 6×6 격자
        Point2D[][] grid = new Point2D[6][6];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                grid[r][c] = new Point2D(margin + c * gridSize, margin + r * gridSize);
            }
        }

        // 1) 외곽 20칸 (POS_0 ~ POS_19, 시계 반대 방향)
        coords.put(Position.POS_0,  grid[5][5]);
        coords.put(Position.POS_1,  grid[4][5]);
        coords.put(Position.POS_2,  grid[3][5]);
        coords.put(Position.POS_3,  grid[2][5]);
        coords.put(Position.POS_4,  grid[1][5]);
        coords.put(Position.POS_5,  grid[0][5]);
        coords.put(Position.POS_6,  grid[0][4]);
        coords.put(Position.POS_7,  grid[0][3]);
        coords.put(Position.POS_8,  grid[0][2]);
        coords.put(Position.POS_9,  grid[0][1]);
        coords.put(Position.POS_10, grid[0][0]);
        coords.put(Position.POS_11, grid[1][0]);
        coords.put(Position.POS_12, grid[2][0]);
        coords.put(Position.POS_13, grid[3][0]);
        coords.put(Position.POS_14, grid[4][0]);
        coords.put(Position.POS_15, grid[5][0]);
        coords.put(Position.POS_16, grid[5][1]);
        coords.put(Position.POS_17, grid[5][2]);
        coords.put(Position.POS_18, grid[5][3]);
        coords.put(Position.POS_19, grid[5][4]);

        // 2) 중앙
        Point2D center = midpoint(grid[2][2], grid[3][3]);
        coords.put(Position.CENTER, center);

        // 3) 지름길 A (POS_5 → 2단계 → CENTER → 2단계 → POS_15)
        Point2D p5  = coords.get(Position.POS_5);
        Point2D p15 = coords.get(Position.POS_15);
        coords.put(Position.DIA_A1, lerp(p5,  center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5,  center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p15, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p15, 2.0/3));

        // 4) 지름길 B (POS_10 → 2단계 → CENTER → 2단계 → POS_0)
        Point2D p10 = coords.get(Position.POS_10);
        Point2D p0  = coords.get(Position.POS_0);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p0,  1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p0,  2.0/3));
    }

    private void initializePentagonCoords() {
        // 보드의 크기를 패널 크기에 맞게 조절
        double width = getWidth() > 0 ? getWidth() : 650;
        double height = getHeight() > 0 ? getHeight() : 650;
        double centerX = width / 2;
        double centerY = height / 2;
        
        // 여백을 화면 크기의 10%로 설정
        double margin = Math.min(width, height) / 10;
        
        // 오각형의 반지름 (중심에서 꼭지점까지의 거리)
        double radius = Math.min(width, height) / 2 - margin;
        
        // 각 지점 간의 각도
        double angleStep = 2 * Math.PI / 5;
        
        // 외곽 경로 노드 총 수 (5개 변 * 5개 노드)
        int totalNodes = 25;
        int nodesPerSide = 5;
        
        // 외곽 25개 노드의 좌표 계산
        for (int i = 0; i < totalNodes; i++) {
            int sideIndex = i / nodesPerSide;
            double ratio = (i % nodesPerSide) / (double)nodesPerSide;
            
            double angle1 = sideIndex * angleStep;
            double angle2 = (sideIndex + 1) * angleStep;
            
            // 현재 변의 시작점과 끝점
            double x1 = centerX + radius * Math.sin(angle1);
            double y1 = centerY - radius * Math.cos(angle1);
            double x2 = centerX + radius * Math.sin(angle2);
            double y2 = centerY - radius * Math.cos(angle2);
            
            // 변 위의 점 계산
            double x = x1 + ratio * (x2 - x1);
            double y = y1 + ratio * (y2 - y1);
            
            Position pos = Position.valueOf("POS_" + i);
            coords.put(pos, new Point2D(x, y));
        }
        
        // 중앙
        Point2D center = new Point2D(centerX, centerY);
        coords.put(Position.CENTER, center);
        
        // 지름길 경로 (BoardShape에 정의된 대로)
        
        // 대각선 A: POS_5 -> CENTER -> POS_20
        Point2D p5 = coords.get(Position.POS_5);
        Point2D p20 = coords.get(Position.POS_20);
        coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p20, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p20, 2.0/3));
        
        // 대각선 B: POS_10 -> CENTER -> END
        Point2D p10 = coords.get(Position.POS_10);
        // END 포지션은 화면 밖에 있으므로, POS_0 방향으로 확장
        Point2D p0 = coords.get(Position.POS_0);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p0, 2.0/3));
        
        // 대각선 C: POS_15 -> CENTER -> END
        Point2D p15 = coords.get(Position.POS_15);
        coords.put(Position.DIA_C1, lerp(p15, center, 1.0/3));
        coords.put(Position.DIA_C2, lerp(p15, center, 2.0/3));
        coords.put(Position.DIA_C3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_C4, lerp(center, p0, 2.0/3));
    }

    private void initializeHexagonCoords() {
        // 보드의 크기를 패널 크기에 맞게 조절
        double width = getWidth() > 0 ? getWidth() : 650;
        double height = getHeight() > 0 ? getHeight() : 650;
        double centerX = width / 2;
        double centerY = height / 2;

        // 여백을 화면 크기의 10%로 설정
        double margin = Math.min(width, height) / 10;

        // 육각형의 반지름 (중심에서 꼭짓점까지의 거리)
        double radius = Math.min(width, height) / 2 - margin;

        // 각 지점 간의 각도 (6변이니까 60도씩)
        double angleStep = 2 * Math.PI / 6;

        // 외곽 경로 노드 총 수 (6변 * 5개 노드)
        int totalNodes = 30;
        int nodesPerSide = 5;

        // 외곽 30개 노드의 좌표 계산
        for (int i = 0; i < totalNodes; i++) {
            int sideIndex = i / nodesPerSide;
            double ratio = (i % nodesPerSide) / (double)nodesPerSide;

            double angle1 = sideIndex * angleStep;
            double angle2 = (sideIndex + 1) * angleStep;

            // 현재 변의 시작점과 끝점
            double x1 = centerX + radius * Math.sin(angle1);
            double y1 = centerY - radius * Math.cos(angle1);
            double x2 = centerX + radius * Math.sin(angle2);
            double y2 = centerY - radius * Math.cos(angle2);

            // 변 위의 점 계산
            double x = x1 + ratio * (x2 - x1);
            double y = y1 + ratio * (y2 - y1);

            Position pos = Position.valueOf("POS_" + i);
            coords.put(pos, new Point2D(x, y));
        }

        // 중앙
        Point2D center = new Point2D(centerX, centerY);
        coords.put(Position.CENTER, center);

        // 지름길 경로 (BoardShape에 정의된 대로)
        
        // 대각선 A: POS_5 -> CENTER -> POS_20
        Point2D p5 = coords.get(Position.POS_5);
        Point2D p20 = coords.get(Position.POS_20);
        coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p20, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p20, 2.0/3));
        
        // 대각선 B: POS_10 -> CENTER -> POS_25
        Point2D p10 = coords.get(Position.POS_10);
        Point2D p25 = coords.get(Position.POS_25);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p25, 1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p25, 2.0/3));
        
        // 대각선 C: POS_15 -> CENTER -> END
        Point2D p15 = coords.get(Position.POS_15);
        // END 포지션은 화면 밖에 있으므로, POS_0 방향으로 확장
        Point2D p0 = coords.get(Position.POS_0);
        coords.put(Position.DIA_C1, lerp(p15, center, 1.0/3));
        coords.put(Position.DIA_C2, lerp(p15, center, 2.0/3));
        coords.put(Position.DIA_C3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_C4, lerp(center, p0, 2.0/3));
    }

    private Point2D midpoint(Point2D a, Point2D b) {
        return lerp(a, b, 0.5);
    }

    private Point2D lerp(Point2D a, Point2D b, double t) {
        return new Point2D(
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t
        );
    }

    public void drawBoard() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // 보드 윤곽선 그리기
        drawBoardOutline(gc);

        // 1) 경로선
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1.5);
        
        // 외곽 경로
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

        // 2) 노드
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        for (Point2D p : coords.values()) {
            gc.fillOval(p.getX() - 12, p.getY() - 12, 24, 24);
            gc.strokeOval(p.getX() - 12, p.getY() - 12, 24, 24);
        }

        // 3) 말
        if (board != null) {
            for (Map.Entry<Position, Point2D> e : coords.entrySet()) {
                Position pos = e.getKey();
                Point2D p = e.getValue();
                List<Piece> list = board.getPiecesAt(pos);
                for (int i = 0; i < list.size(); i++) {
                    Piece piece = list.get(i);
                    gc.setFill(getPlayerColor(piece.getOwner().getName()));
                    gc.fillOval(p.getX() - 6 + i * 12, p.getY() - 6, 12, 12);
                }
            }
        }
        
        // 방향 화살표 그리기
        drawDirectionArrows(gc);
    }
    
    private void drawDirectionArrows(GraphicsContext gc) {
        gc.setStroke(Color.DARKGREEN); // 진한 녹색 화살표
        gc.setFill(Color.DARKGREEN);
        gc.setLineWidth(2.0);
        
        // 시작 위치와 다음 위치를 가져옴
        Point2D start = coords.get(Position.POS_0);
        Point2D next = coords.get(Position.POS_1);
        
        if (start != null && next != null) {
            // 화살표 그리기
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
        
        // 첫 번째 노드와 마지막 노드 연결 (닫힌 경로)
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
        
        // 첫 번째 노드와 마지막 노드 연결 (닫힌 경로)
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
    
    private void drawBoardOutline(GraphicsContext gc) {
        gc.setFill(Color.rgb(240, 240, 240)); // 연한 회색 배경
        
        switch (boardShape) {
            case TRADITIONAL:
                // 사각형 윤곽선
                if (coords.containsKey(Position.POS_0) && coords.containsKey(Position.POS_10)) {
                    Point2D p0 = coords.get(Position.POS_0);
                    Point2D p5 = coords.get(Position.POS_5);
                    Point2D p10 = coords.get(Position.POS_10);
                    Point2D p15 = coords.get(Position.POS_15);
                    
                    double[] xPoints = {p0.getX(), p5.getX(), p10.getX(), p15.getX()};
                    double[] yPoints = {p0.getY(), p5.getY(), p10.getY(), p15.getY()};
                    
                    gc.fillPolygon(xPoints, yPoints, 4);
                }
                break;
                
            case PENTAGON:
                // 오각형 윤곽선
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
                break;
                
            case HEXAGON:
                // 육각형 윤곽선
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

    private Color getPlayerColor(String name) {
        switch (name) {
            case "Player 1": return Color.RED;
            case "Player 2": return Color.BLUE;
            case "Player 3": return Color.GREEN;
            case "Player 4": return Color.MAGENTA;
            default:         return Color.GRAY;
        }
    }
}
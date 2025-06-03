package frontend;

import backend.model.Board;
import backend.model.BoardShape;
import backend.model.Position;

import java.util.HashMap;
import java.util.Map;

/**
 * 보드 패널의 공통 로직을 담은 추상 클래스
 * @param <PointType> 좌표 타입 (Point for Swing, Point2D for JavaFX)
 */
public abstract class AbstractBoardPanel<PointType> implements BoardPanelInterface {
    protected final Board board;
    protected final Map<Position, PointType> coords = new HashMap<>();
    protected BoardShape boardShape = BoardShape.TRADITIONAL;
    
    public AbstractBoardPanel(Board board) {
        this.board = board;
    }
    
    @Override
    public void setBoardShape(BoardShape shape) {
        this.boardShape = shape;
        initializeCoords();
        refresh();
    }
    
    @Override
    public BoardShape getBoardShape() {
        return this.boardShape;
    }
    
    @Override
    public void initializeCoords() {
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
    
    // 추상 메서드들 - 각 구현체에서 정의
    protected abstract void initializeTraditionalCoords();
    protected abstract void initializePentagonCoords();
    protected abstract void initializeHexagonCoords();
    protected abstract PointType createPoint(double x, double y);
    protected abstract PointType midpoint(PointType a, PointType b);
    protected abstract PointType lerp(PointType a, PointType b, double t);
    protected abstract double getComponentWidth();
    protected abstract double getComponentHeight();
    
    // 공통 유틸리티 메서드들
    protected void calculateTraditionalLayout() {
        double width = getComponentWidth() > 0 ? getComponentWidth() : 650;
        double height = getComponentHeight() > 0 ? getComponentHeight() : 650;
        
        double margin = Math.min(width, height) / 10;
        double gridSize = Math.min(width - 2 * margin, height - 2 * margin) / 5;
        
        // 6×6 격자 생성
        PointType[][] grid = createGrid(6, 6, margin, gridSize);
        
        // 외곽 20칸 배치
        setOuterPositions(grid);
        
        // 중앙 위치 설정
        PointType center = midpoint(grid[2][2], grid[3][3]);
        coords.put(Position.CENTER, center);
        
        // 대각선 경로 설정
        setDiagonalPaths(center);
    }
    
    protected void calculatePentagonLayout() {
        double width = getComponentWidth() > 0 ? getComponentWidth() : 650;
        double height = getComponentHeight() > 0 ? getComponentHeight() : 650;
        double centerX = width / 2;
        double centerY = height / 2;
        
        double margin = Math.min(width, height) / 10;
        double radius = Math.min(width, height) / 2 - margin;
        double angleStep = 2 * Math.PI / 5;
        
        // 외곽 25개 노드 배치
        setNodesOnPolygon(25, 5, centerX, centerY, radius, angleStep);
        
        // 중앙과 대각선 경로 설정
        PointType center = createPoint(centerX, centerY);
        coords.put(Position.CENTER, center);
        setPentagonDiagonalPaths(center);
    }
    
    protected void calculateHexagonLayout() {
        double width = getComponentWidth() > 0 ? getComponentWidth() : 650;
        double height = getComponentHeight() > 0 ? getComponentHeight() : 650;
        double centerX = width / 2;
        double centerY = height / 2;
        
        double margin = Math.min(width, height) / 10;
        double radius = Math.min(width, height) / 2 - margin;
        double angleStep = 2 * Math.PI / 6;
        
        // 외곽 30개 노드 배치
        setNodesOnPolygon(30, 6, centerX, centerY, radius, angleStep);
        
        // 중앙과 대각선 경로 설정
        PointType center = createPoint(centerX, centerY);
        coords.put(Position.CENTER, center);
        setHexagonDiagonalPaths(center);
    }
    
    @SuppressWarnings("unchecked")
    private PointType[][] createGrid(int rows, int cols, double margin, double gridSize) {
        PointType[][] grid = (PointType[][]) new Object[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = createPoint(margin + c * gridSize, margin + r * gridSize);
            }
        }
        return grid;
    }
    
    private void setOuterPositions(PointType[][] grid) {
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
    }
    
    private void setDiagonalPaths(PointType center) {
        PointType p5 = coords.get(Position.POS_5);
        PointType p15 = coords.get(Position.POS_15);
        coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p15, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p15, 2.0/3));

        PointType p10 = coords.get(Position.POS_10);
        PointType p0 = coords.get(Position.POS_0);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p0, 2.0/3));
    }
    
    private void setNodesOnPolygon(int totalNodes, int sides, 
                                   double centerX, double centerY, 
                                   double radius, double angleStep) {
        int nodesPerSide = totalNodes / sides;
        for (int i = 0; i < totalNodes; i++) {
            int sideIndex = i / nodesPerSide;
            double ratio = (i % nodesPerSide) / (double)nodesPerSide;
            
            double angle1 = sideIndex * angleStep;
            double angle2 = (sideIndex + 1) * angleStep;
            
            double x1 = centerX + radius * Math.sin(angle1);
            double y1 = centerY - radius * Math.cos(angle1);
            double x2 = centerX + radius * Math.sin(angle2);
            double y2 = centerY - radius * Math.cos(angle2);
            
            double x = x1 + ratio * (x2 - x1);
            double y = y1 + ratio * (y2 - y1);
            
            Position pos = Position.valueOf("POS_" + i);
            coords.put(pos, createPoint(x, y));
        }
    }
    
    private void setPentagonDiagonalPaths(PointType center) {
        // 대각선 A: POS_5 -> CENTER -> POS_20
        PointType p5 = coords.get(Position.POS_5);
        PointType p20 = coords.get(Position.POS_20);
        if (p5 != null && p20 != null) {
            coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
            coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
            coords.put(Position.DIA_A3, lerp(center, p20, 1.0/3));
            coords.put(Position.DIA_A4, lerp(center, p20, 2.0/3));
        }
        
        // 대각선 B: POS_10 -> CENTER -> POS_0
        PointType p10 = coords.get(Position.POS_10);
        PointType p0 = coords.get(Position.POS_0);
        if (p10 != null && p0 != null) {
            coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
            coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
            coords.put(Position.DIA_B3, lerp(center, p0, 1.0/3));
            coords.put(Position.DIA_B4, lerp(center, p0, 2.0/3));
        }
        
        // 대각선 C: POS_15 -> CENTER -> POS_0
        PointType p15 = coords.get(Position.POS_15);
        if (p15 != null && p0 != null) {
            coords.put(Position.DIA_C1, lerp(p15, center, 1.0/3));
            coords.put(Position.DIA_C2, lerp(p15, center, 2.0/3));
            coords.put(Position.DIA_C3, lerp(center, p0, 1.0/3));
            coords.put(Position.DIA_C4, lerp(center, p0, 2.0/3));
        }
    }
    
    private void setHexagonDiagonalPaths(PointType center) {
        // 대각선 A: POS_5 -> CENTER -> POS_20
        PointType p5 = coords.get(Position.POS_5);
        PointType p20 = coords.get(Position.POS_20);
        if (p5 != null && p20 != null) {
            coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
            coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
            coords.put(Position.DIA_A3, lerp(center, p20, 1.0/3));
            coords.put(Position.DIA_A4, lerp(center, p20, 2.0/3));
        }
        
        // 대각선 B: POS_10 -> CENTER -> POS_25
        PointType p10 = coords.get(Position.POS_10);
        PointType p25 = coords.get(Position.POS_25);
        if (p10 != null && p25 != null) {
            coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
            coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
            coords.put(Position.DIA_B3, lerp(center, p25, 1.0/3));
            coords.put(Position.DIA_B4, lerp(center, p25, 2.0/3));
        }
        
        // 대각선 C: POS_15 -> CENTER -> POS_0
        PointType p15 = coords.get(Position.POS_15);
        PointType p0 = coords.get(Position.POS_0);
        if (p15 != null && p0 != null) {
            coords.put(Position.DIA_C1, lerp(p15, center, 1.0/3));
            coords.put(Position.DIA_C2, lerp(p15, center, 2.0/3));
            coords.put(Position.DIA_C3, lerp(center, p0, 1.0/3));
            coords.put(Position.DIA_C4, lerp(center, p0, 2.0/3));
        }
    }
    
    // 플레이어 색상 유틸리티
    public static class PlayerColors {
        public static final String PLAYER_1_COLOR = "#FF0000"; // RED
        public static final String PLAYER_2_COLOR = "#0000FF"; // BLUE
        public static final String PLAYER_3_COLOR = "#00FF00"; // GREEN
        public static final String PLAYER_4_COLOR = "#FF00FF"; // MAGENTA
        public static final String DEFAULT_COLOR = "#808080";  // GRAY
        
        public static String getColorHex(String playerName) {
            switch (playerName) {
                case "Player 1": return PLAYER_1_COLOR;
                case "Player 2": return PLAYER_2_COLOR;
                case "Player 3": return PLAYER_3_COLOR;
                case "Player 4": return PLAYER_4_COLOR;
                default:         return DEFAULT_COLOR;
            }
        }
    }
    
    // 자식 클래스에서 사용할 수 있는 protected 메서드
    protected String getPlayerColor(String playerName) {
        return PlayerColors.getColorHex(playerName);
    }
}
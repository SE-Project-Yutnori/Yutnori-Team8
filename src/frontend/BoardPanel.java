// File: frontend/BoardPanel.java
package frontend;

import backend.model.Board;
import backend.model.BoardShape;
import backend.model.Piece;
import backend.model.Position;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 전통 윷놀이판만 표시 (OFFBOARD, END 제외)
 */
public class BoardPanel extends JPanel {
    private final Board board;
    private final Map<Position, Point> coords = new HashMap<>();
    private BoardShape boardShape = BoardShape.TRADITIONAL; // 기본값은 전통 보드

    public BoardPanel(Board board) {
        this.board = board;
        setPreferredSize(new Dimension(650, 650));
        setBackground(Color.WHITE);
        initializeCoords();
    }
    
    public void setBoardShape(BoardShape shape) {
        this.boardShape = shape;
        initializeCoords();
        repaint();
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
        int width = getWidth() > 0 ? getWidth() : 650;
        int height = getHeight() > 0 ? getHeight() : 650;
        
        // 여백을 화면 크기의 10%로 설정
        int margin = Math.min(width, height) / 10;
        
        // 격자 간격을 화면 크기에 맞게 조절
        int gridSize = Math.min(width - 2 * margin, height - 2 * margin) / 5;
        
        // 6×6 격자
        Point[][] grid = new Point[6][6];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                grid[r][c] = new Point(margin + c * gridSize, margin + r * gridSize);
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
        Point center = midpoint(grid[2][2], grid[3][3]);
        coords.put(Position.CENTER, center);

        // 3) 지름길 A (POS_5 → 2단계 → CENTER → 2단계 → POS_15)
        Point p5  = coords.get(Position.POS_5);
        Point p15 = coords.get(Position.POS_15);
        coords.put(Position.DIA_A1, lerp(p5,  center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5,  center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p15, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p15, 2.0/3));

        // 4) 지름길 B (POS_10 → 2단계 → CENTER → 2단계 → POS_0)
        Point p10 = coords.get(Position.POS_10);
        Point p0  = coords.get(Position.POS_0);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p0,  1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p0,  2.0/3));
    }

    private void initializePentagonCoords() {
        // 보드의 크기를 패널 크기에 맞게 조절
        int width = getWidth() > 0 ? getWidth() : 650;
        int height = getHeight() > 0 ? getHeight() : 650;
        int centerX = width / 2;
        int centerY = height / 2;
        
        // 여백을 화면 크기의 10%로 설정
        int margin = Math.min(width, height) / 10;
        
        // 오각형의 반지름 (중심에서 꼭지점까지의 거리)
        int radius = Math.min(width, height) / 2 - margin;
        
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
            coords.put(pos, new Point((int)x, (int)y));
        }
        
        // 중앙
        coords.put(Position.CENTER, new Point(centerX, centerY));
        
        // 지름길 경로 (BoardShape에 정의된 대로)
        
        // 대각선 A: POS_5 -> CENTER -> POS_20
        Point p5 = coords.get(Position.POS_5);
        Point center = coords.get(Position.CENTER);
        Point p20 = coords.get(Position.POS_20);
        coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p20, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p20, 2.0/3));
        
        // 대각선 B: POS_10 -> CENTER -> END
        Point p10 = coords.get(Position.POS_10);
        // END 포지션은 화면 밖에 있으므로, POS_0 방향으로 확장
        Point p0 = coords.get(Position.POS_0);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p0, 2.0/3));
        
        // 대각선 C: POS_15 -> CENTER -> END
        Point p15 = coords.get(Position.POS_15);
        coords.put(Position.DIA_C1, lerp(p15, center, 1.0/3));
        coords.put(Position.DIA_C2, lerp(p15, center, 2.0/3));
        coords.put(Position.DIA_C3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_C4, lerp(center, p0, 2.0/3));
    }

    private void initializeHexagonCoords() {
        // 보드의 크기를 패널 크기에 맞게 조절
        int width = getWidth() > 0 ? getWidth() : 650;
        int height = getHeight() > 0 ? getHeight() : 650;
        int centerX = width / 2;
        int centerY = height / 2;

        // 여백을 화면 크기의 10%로 설정
        int margin = Math.min(width, height) / 10;

        // 육각형의 반지름 (중심에서 꼭짓점까지의 거리)
        int radius = Math.min(width, height) / 2 - margin;

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
            coords.put(pos, new Point((int)x, (int)y));
        }

        // 중앙
        Point center = new Point(centerX, centerY);
        coords.put(Position.CENTER, center);

        // 지름길 경로 (BoardShape에 정의된 대로)
        
        // 대각선 A: POS_5 -> CENTER -> POS_20
        Point p5 = coords.get(Position.POS_5);
        Point p20 = coords.get(Position.POS_20);
        coords.put(Position.DIA_A1, lerp(p5, center, 1.0/3));
        coords.put(Position.DIA_A2, lerp(p5, center, 2.0/3));
        coords.put(Position.DIA_A3, lerp(center, p20, 1.0/3));
        coords.put(Position.DIA_A4, lerp(center, p20, 2.0/3));
        
        // 대각선 B: POS_10 -> CENTER -> POS_25
        Point p10 = coords.get(Position.POS_10);
        Point p25 = coords.get(Position.POS_25);
        coords.put(Position.DIA_B1, lerp(p10, center, 1.0/3));
        coords.put(Position.DIA_B2, lerp(p10, center, 2.0/3));
        coords.put(Position.DIA_B3, lerp(center, p25, 1.0/3));
        coords.put(Position.DIA_B4, lerp(center, p25, 2.0/3));
        
        // 대각선 C: POS_15 -> CENTER -> END
        Point p15 = coords.get(Position.POS_15);
        // END 포지션은 화면 밖에 있으므로, POS_0 방향으로 확장
        Point p0 = coords.get(Position.POS_0);
        coords.put(Position.DIA_C1, lerp(p15, center, 1.0/3));
        coords.put(Position.DIA_C2, lerp(p15, center, 2.0/3));
        coords.put(Position.DIA_C3, lerp(center, p0, 1.0/3));
        coords.put(Position.DIA_C4, lerp(center, p0, 2.0/3));
    }


    private Point midpoint(Point a, Point b) {
        return lerp(a, b, 0.5);
    }

    private Point lerp(Point a, Point b, double t) {
        return new Point(
                (int)(a.x + (b.x - a.x) * t),
                (int)(a.y + (b.y - a.y) * t)
        );
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        initializeCoords(); // 크기가 변경될 때마다 좌표 재계산
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 새 크기에 맞게 좌표 재계산
        if (getWidth() > 0 && getHeight() > 0) {
            initializeCoords();
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 보드 윤곽선 그리기
        drawBoardOutline(g2d);

        // 1) 경로선
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1.5f));
        
        // 외곽 경로
        switch (boardShape) {
	        case TRADITIONAL:
	            drawOuterPathTraditional(g2d);
	            break;
	        case PENTAGON:
	            drawOuterPathPentagon(g2d);
	            break;
	        case HEXAGON:
	            drawOuterPathHexagon(g2d);
	            break;
        }
        
        // 대각선 경로 그리기
        drawDiagonalPaths(g2d);

        // 2) 노드
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        for (Point p : coords.values()) {
            g2d.drawOval(p.x - 12, p.y - 12, 24, 24);
        }

        // 3) 말
        if (board != null) {
            for (Map.Entry<Position, Point> e : coords.entrySet()) {
                Position pos = e.getKey();
                Point p = e.getValue();
                List<Piece> list = board.getPiecesAt(pos);
                for (int i = 0; i < list.size(); i++) {
                    Piece piece = list.get(i);
                    g2d.setColor(getPlayerColor(piece.getOwner().getName()));
                    g2d.fillOval(p.x - 6 + i * 12, p.y - 6, 12, 12);
                }
            }
        }
    }
    
    private void drawOuterPathTraditional(Graphics2D g2d) {
        // 외곽 경로
        Position[] outerPath = new Position[20];
        for (int i = 0; i < 20; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(g2d, outerPath);
        
        Point first = coords.get(Position.POS_0);
        Point last = coords.get(Position.POS_24);
        if (first != null && last != null) {
            g2d.drawLine(last.x, last.y, first.x, first.y);
        }
        
    }
    
    private void drawOuterPathPentagon(Graphics2D g2d) {
        // 외곽 경로
        Position[] outerPath = new Position[25];
        for (int i = 0; i < 25; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(g2d, outerPath);
        
        // 첫 번째 노드와 마지막 노드 연결 (닫힌 경로)
        Point first = coords.get(Position.POS_0);
        Point last = coords.get(Position.POS_24);
        if (first != null && last != null) {
            g2d.drawLine(last.x, last.y, first.x, first.y);
        }
    }
    
    private void drawOuterPathHexagon(Graphics2D g2d) {
        // 외곽 경로
        Position[] outerPath = new Position[30];
        for (int i = 0; i < 30; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(g2d, outerPath);
        
        // 첫 번째 노드와 마지막 노드 연결 (닫힌 경로)
        Point first = coords.get(Position.POS_0);
        Point last = coords.get(Position.POS_29);
        if (first != null && last != null) {
            g2d.drawLine(last.x, last.y, first.x, first.y);
        }
    }
    
    private void drawDiagonalPaths(Graphics2D g2d) {
        switch (boardShape) {
            case TRADITIONAL:
            	// 대각선 경로 A
                drawPath(g2d,
                        Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4, Position.POS_15
                );
                
                // 대각선 경로 B
                drawPath(g2d,
                        Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4, Position.POS_0
                );
                break;
                
            case PENTAGON:
                
                // 대각선 A: POS_5 -> CENTER -> POS_20
                drawPath(g2d,
                        Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4, Position.POS_20
                );
                
                // 대각선 B: POS_10 -> CENTER -> END
                // END는 화면 밖으로 가는 경로이므로 DIA_B4만 표시
                drawPath(g2d,
                        Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4, Position.POS_0
                );
                
                // 대각선 C: POS_15 -> CENTER -> POS_0
                drawPath(g2d,
                        Position.POS_15, Position.DIA_C1, Position.DIA_C2, Position.CENTER,
                        Position.DIA_C3, Position.DIA_C4, Position.POS_0
                );
                break;
                
            case HEXAGON:
                // BoardShape에 정의된 대로 대각선 경로 그리기
                
                // 대각선 A: POS_5 -> CENTER -> POS_20
                drawPath(g2d,
                        Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4, Position.POS_20
                );
                
                // 대각선 B: POS_10 -> CENTER -> POS_25
                drawPath(g2d,
                        Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4, Position.POS_25
                );
                
                // 대각선 C: POS_15 -> CENTER -> END
                // END는 화면 밖으로 가는 경로이므로 DIA_C4만 표시
                drawPath(g2d,
                        Position.POS_15, Position.DIA_C1, Position.DIA_C2, Position.CENTER,
                        Position.DIA_C3, Position.DIA_C4, Position.POS_0
                );
                break;
        }
    }
    
    private void drawBoardOutline(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240)); // 연한 회색 배경
        
        Path2D path = new Path2D.Double();
        
        switch (boardShape) {
            case TRADITIONAL:
                // 사각형 윤곽선
                if (coords.containsKey(Position.POS_0) && coords.containsKey(Position.POS_10)) {
                    Point p0 = coords.get(Position.POS_0);
                    Point p5 = coords.get(Position.POS_5);
                    Point p10 = coords.get(Position.POS_10);
                    Point p15 = coords.get(Position.POS_15);
                    
                    path.moveTo(p0.x, p0.y);
                    path.lineTo(p5.x, p5.y);
                    path.lineTo(p10.x, p10.y);
                    path.lineTo(p15.x, p15.y);
                    path.closePath();
                    
                    g2d.fill(path);
                }
                break;
                
            case PENTAGON:
                // 오각형 윤곽선
                for (int i = 0; i < 5; i++) {
                    Position pos = Position.valueOf("POS_" + (i * 5)); // 각 꼭지점 (0, 5, 10, 15, 20)
                    Point p = coords.get(pos);
                    
                    if (p != null) {
                        if (i == 0) {
                            path.moveTo(p.x, p.y);
                        } else {
                            path.lineTo(p.x, p.y);
                        }
                    }
                }
                path.closePath();
                g2d.fill(path);
                break;
                
            case HEXAGON:
                // 육각형 윤곽선
                for (int i = 0; i < 6; i++) {
                    Position pos = Position.valueOf("POS_" + (i * 5)); // 각 꼭지점 (0, 5, 10, 15, 20, 25)
                    Point p = coords.get(pos);
                    
                    if (p != null) {
                        if (i == 0) {
                            path.moveTo(p.x, p.y);
                        } else {
                            path.lineTo(p.x, p.y);
                        }
                    }
                }
                path.closePath();
                g2d.fill(path);
                break;
        }
    }

    private void drawPath(Graphics2D g, Position... pts) {
        for (int i = 0; i < pts.length - 1; i++) {
            Point a = coords.get(pts[i]);
            Point b = coords.get(pts[i+1]);
            if (a != null && b != null) {
                g.drawLine(a.x, a.y, b.x, b.y);
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
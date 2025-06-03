package frontend;

import backend.model.Board;
import backend.model.Piece;
import backend.model.Position;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

public class SwingBoardPanel extends AbstractBoardPanel<Point> {
    private JPanel panel;
    
    public SwingBoardPanel(Board board) {
        super(board);
        this.panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                SwingBoardPanel.this.paintBoard(g);
            }
            
            @Override
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x, y, width, height);
                SwingBoardPanel.this.initializeCoords();
            }
        };
        
        panel.setPreferredSize(new Dimension(650, 650));
        panel.setBackground(Color.WHITE);
        initializeCoords();
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    @Override
    public void refresh() {
        panel.repaint();
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
    protected Point createPoint(double x, double y) {
        return new Point((int)x, (int)y);
    }
    
    @Override
    protected Point midpoint(Point a, Point b) {
        return lerp(a, b, 0.5);
    }
    
    @Override
    protected Point lerp(Point a, Point b, double t) {
        return new Point(
            (int)(a.x + (b.x - a.x) * t),
            (int)(a.y + (b.y - a.y) * t)
        );
    }
    
    @Override
    protected double getComponentWidth() {
        return panel.getWidth();
    }
    
    @Override
    protected double getComponentHeight() {
        return panel.getHeight();
    }
    
    private void paintBoard(Graphics g) {
        if (getComponentWidth() > 0 && getComponentHeight() > 0) {
            initializeCoords();
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawBoardOutline(g2d);
        drawPaths(g2d);
        drawNodes(g2d);
        drawPieces(g2d);
        drawDirectionArrows(g2d);
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
    
    private void drawPaths(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1.5f));
        
        // 외곽 경로 그리기
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
    }
    
    private void drawOuterPathTraditional(Graphics2D g2d) {
        // 외곽 경로
        Position[] outerPath = new Position[20];
        for (int i = 0; i < 20; i++) {
            outerPath[i] = Position.valueOf("POS_" + i);
        }
        drawPath(g2d, outerPath);
        
        // 마지막과 첫 번째 연결
        Point first = coords.get(Position.POS_0);
        Point last = coords.get(Position.POS_19);
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
        
        // 첫 번째 노드와 마지막 노드 연결
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
        
        // 첫 번째 노드와 마지막 노드 연결
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
                drawPath(g2d,
                        Position.POS_15, Position.DIA_C1, Position.DIA_C2, Position.CENTER,
                        Position.DIA_C3, Position.DIA_C4, Position.POS_0
                );
                break;
        }
    }
    
    private void drawPath(Graphics2D g2d, Position... pts) {
        for (int i = 0; i < pts.length - 1; i++) {
            Point a = coords.get(pts[i]);
            Point b = coords.get(pts[i+1]);
            if (a != null && b != null) {
                g2d.drawLine(a.x, a.y, b.x, b.y);
            }
        }
    }
    
    private void drawNodes(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        for (Point p : coords.values()) {
            g2d.drawOval(p.x - 12, p.y - 12, 24, 24);
        }
    }
    
    private void drawPieces(Graphics2D g2d) {
        if (board != null) {
            for (Map.Entry<Position, Point> e : coords.entrySet()) {
                Position pos = e.getKey();
                Point p = e.getValue();
                List<Piece> list = board.getPiecesAt(pos);
                for (int i = 0; i < list.size(); i++) {
                    Piece piece = list.get(i);
                    String colorHex = PlayerColors.getColorHex(piece.getOwner().getName());
                    g2d.setColor(Color.decode(colorHex));
                    g2d.fillOval(p.x - 6 + i * 12, p.y - 6, 12, 12);
                }
            }
        }
    }
    
    private void drawDirectionArrows(Graphics2D g2d) {
        g2d.setColor(new Color(0, 100, 0));
        g2d.setStroke(new BasicStroke(2.0f));
        
        Point start = coords.get(Position.POS_0);
        Point next = coords.get(Position.POS_1);
        
        if (start != null && next != null) {
            drawArrow(g2d, start, next);
        }
    }
    
    private void drawArrow(Graphics2D g2d, Point from, Point to) {
    	if (from == null || to == null) return;
        
        // 화살표 길이와 방향 계산
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        // 화살표 축소 (시작과 끝 노드 사이에 딱 맞게)
        double scaleFactor = 0.7; // 화살표 길이 비율
        int arrowX = from.x + (int)(dx * scaleFactor);
        int arrowY = from.y + (int)(dy * scaleFactor);
        
        // 화살표 머리 크기
        int arrowSize = 10;
        
        // 화살표 방향 각도
        double angle = Math.atan2(dy, dx);
        
        // 화살표 몸통 그리기
        g2d.drawLine(from.x, from.y, arrowX, arrowY);
        
        // 화살표 머리 그리기
        AffineTransform tx = g2d.getTransform();
        g2d.translate(arrowX, arrowY);
        g2d.rotate(angle);
        
        // 삼각형 화살표 머리
        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(0, 0);
        arrowHead.lineTo(-arrowSize, -arrowSize/2);
        arrowHead.lineTo(-arrowSize, arrowSize/2);
        arrowHead.closePath();
        
        g2d.fill(arrowHead);
        g2d.setTransform(tx);
    }
}
// File: frontend/BoardPanel.java
package frontend;

import backend.model.Board;
import backend.model.Piece;
import backend.model.Position;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Point;
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

    public BoardPanel(Board board) {
        this.board = board;
        setPreferredSize(new Dimension(650, 650));
        setBackground(Color.WHITE);
        initializeCoords();
    }

    private void initializeCoords() {
        int margin = 50;
        int gap    = 100;

        // 6×6 격자
        Point[][] grid = new Point[6][6];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                grid[r][c] = new Point(margin + c * gap, margin + r * gap);
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1) 경로선
        g.setColor(Color.LIGHT_GRAY);
        drawPath(g,
                Position.POS_0, Position.POS_1, Position.POS_2, Position.POS_3, Position.POS_4,
                Position.POS_5, Position.POS_6, Position.POS_7, Position.POS_8, Position.POS_9,
                Position.POS_10,Position.POS_11,Position.POS_12,Position.POS_13,Position.POS_14,
                Position.POS_15,Position.POS_16,Position.POS_17,Position.POS_18,Position.POS_19
        );
        drawPath(g,
                Position.POS_5, Position.DIA_A1, Position.DIA_A2, Position.CENTER,
                Position.DIA_A3, Position.DIA_A4, Position.POS_15
        );
        drawPath(g,
                Position.POS_10,Position.DIA_B1, Position.DIA_B2, Position.CENTER,
                Position.DIA_B3, Position.DIA_B4, Position.POS_0
        );

        // 2) 노드
        g.setColor(Color.BLACK);
        for (Point p : coords.values()) {
            g.drawOval(p.x - 12, p.y - 12, 24, 24);
        }

        // 3) 말
        for (Map.Entry<Position, Point> e : coords.entrySet()) {
            Position pos = e.getKey();
            Point p     = e.getValue();
            List<Piece> list = board.getPiecesAt(pos);
            for (int i = 0; i < list.size(); i++) {
                Piece piece = list.get(i);
                g.setColor(getPlayerColor(piece.getOwner().getName()));
                g.fillOval(p.x - 6 + i * 12, p.y - 6, 12, 12);
            }
        }
    }

    private void drawPath(Graphics g, Position... pts) {
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

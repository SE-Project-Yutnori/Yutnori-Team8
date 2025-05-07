package frontend;

import backend.game.Game;
import backend.game.YutThrowResult;
import backend.game.YutThrower;
import backend.model.PathManager;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class YutGameUI extends JFrame {
    private Game game;
    private JLabel statusLabel;
    private JTextArea logArea, indicatorArea;
    private BoardPanel boardPanel;

    public static void launch() {
        SwingUtilities.invokeLater(() -> new YutGameUI().setVisible(true));
    }

    public YutGameUI() {
        super("전통 윷놀이");
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusLabel = new JLabel();
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new BoardPanel(null);
        add(boardPanel, BorderLayout.CENTER);

        indicatorArea = new JTextArea(10, 20);
        indicatorArea.setEditable(false);
        indicatorArea.setBorder(BorderFactory.createTitledBorder("미시작 말"));
        add(new JScrollPane(indicatorArea), BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout());
        logArea = new JTextArea(5, 40);
        logArea.setEditable(false);
        bottom.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton randomBtn = new JButton("랜덤 윷 던지기");
        randomBtn.addActionListener(e -> handleThrow());
        btnPanel.add(randomBtn);

        JButton designatedBtn = new JButton("지정 윷 던지기");
        designatedBtn.addActionListener(e -> handleDesignatedThrow());
        btnPanel.add(designatedBtn);

        bottom.add(btnPanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        initGame();
    }

    private void initGame() {
        int pc = prompt("참가자 수 (2~4명)", 2, 4);
        int mc = prompt("말 수 (2~5개)", 2, 5);
        game = new Game(pc, mc);

        remove(boardPanel);
        boardPanel = new BoardPanel(game.getBoard());
        add(boardPanel, BorderLayout.CENTER);

        statusLabel.setText(game.getCurrentPlayer().getName() + " 차례입니다.");
        logArea.setText("게임을 시작합니다.\n");
        updateIndicator();

        revalidate();
        repaint();
    }

    private int prompt(String msg, int min, int max) {
        while (true) {
            String s = JOptionPane.showInputDialog(this, msg);
            if (s == null) System.exit(0);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {}
        }
    }

    private void handleThrow() {
        processThrow(YutThrower.throwRandom());
    }

    private void handleDesignatedThrow() {
        YutThrowResult[] opts = YutThrowResult.values();
        String[] labels = Arrays.stream(opts).map(Enum::name).toArray(String[]::new);
        String sel = (String) JOptionPane.showInputDialog(
                this, "지정할 윷 결과를 선택하세요", "지정 윷 던지기",
                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]
        );
        if (sel == null) return;
        processThrow(YutThrowResult.valueOf(sel));
    }

    private void processThrow(YutThrowResult res) {
        Player player = game.getCurrentPlayer();
        logArea.append(player.getName() + " → " + res.name() + "\n");

        // 이동 가능한 말 선택
        List<Piece> movable = new ArrayList<>();
        for (Piece p : player.getPieces()) {
            if (!p.isFinished()) movable.add(p);
        }
        if (movable.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이동할 말이 없습니다.");
            return;
        }

        Piece sel = (movable.size() == 1) ? movable.get(0) : choosePiece(movable);
        if (sel == null) return;

        Position cur = sel.getPosition();
        // 이제 무조건 하나의 메서드로 경로 계산
        List<Position> path = PathManager.getNextPositions(cur, res.getMove());

        // 업기 그룹 설정
        List<Piece> group = new ArrayList<>();
        if (cur == Position.OFFBOARD) {
            group.add(sel);
        } else {
            for (Piece p : game.getBoard().getPiecesAt(cur)) {
                if (p.getOwner() == player) group.add(p);
            }
        }

        // 이동 & 잡기
        boolean captured = false;
        if (!path.isEmpty()) {
            Position dest = path.get(path.size() - 1);
            for (Piece p : group) {
                if (game.getBoard().placePiece(p, dest)) {
                    captured = true;
                }
            }
            logArea.append(" → " + dest.name() +
                    " (" + group.size() + "개)\n");
        }

        boardPanel.repaint();
        updateIndicator();

        // 승리 체크
        if (game.checkWin(player)) {
            JOptionPane.showMessageDialog(this, player.getName() + " 승리!");
            System.exit(0);
        }

        // 추가 턴: 윷·모·잡기 시
        if (res == YutThrowResult.YUT ||
                res == YutThrowResult.MO  ||
                captured) {
            logArea.append("추가 던지기 기회!\n");
        } else {
            game.nextTurn();
            statusLabel.setText(
                    game.getCurrentPlayer().getName() + " 차례입니다."
            );
        }
    }

    private Piece choosePiece(List<Piece> mv) {
        String[] opts = new String[mv.size()];
        for (int i = 0; i < mv.size(); i++) {
            opts[i] = String.format("%d번 말 (%s)", i+1, mv.get(i).getPosition());
        }
        String sel = (String) JOptionPane.showInputDialog(
                this, "이동할 말을 선택하세요", "말 선택",
                JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]
        );
        if (sel == null) return null;
        return mv.get(Integer.parseInt(sel.split("번")[0]) - 1);
    }

    private void updateIndicator() {
        StringBuilder sb = new StringBuilder();
        for (Player p : game.getPlayers()) {
            long off = p.getPieces().stream()
                    .filter(x -> x.getPosition() == Position.OFFBOARD)
                    .count();
            sb.append(p.getName()).append(": ").append(off).append("개\n");
        }
        indicatorArea.setText(sb.toString());
    }
}

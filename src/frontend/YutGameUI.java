// File: src/frontend/YutGameUI.java
package frontend;

import backend.controller.GameController;
import backend.game.Game; // 타입 힌트 등에 필요할 수 있음
import backend.game.YutThrowResult;
// import backend.game.YutThrower; // UI는 더 이상 YutThrower를 직접 호출하지 않음
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position; // 필요한 경우 말 위치 표시에 사용

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList; // For promptForThrowSelection
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class YutGameUI extends JFrame {
    private GameController controller; // 컨트롤러를 보유
    private Game gameModel; // 선택 사항: 읽기 전용 표시 요구에 대한 직접 참조 (BoardPanel 등에 전달)

    private JLabel statusLabel;
    private JTextArea logArea, indicatorArea;
    private BoardPanel boardPanel;
    private JButton randomThrowButton, designatedThrowButton;

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            YutGameUI ui = new YutGameUI();
            GameController controller = new GameController(ui); // UI 참조를 컨트롤러에 전달
            ui.setController(controller); // 컨트롤러 참조를 UI에 전달
            ui.setVisible(true);
            ui.promptForGameSetup(); // 게임 설정 시작
        });
    }

    public YutGameUI() {
        super("전통 윷놀이 (MVC 리팩터링)");
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("게임 설정을 시작하세요.", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        add(statusLabel, BorderLayout.NORTH);

        // BoardPanel은 게임 설정 후 초기화됨
        boardPanel = new BoardPanel(null); // null 또는 더미 보드로 초기화
        add(boardPanel, BorderLayout.CENTER);

        indicatorArea = new JTextArea(10, 20);
        indicatorArea.setEditable(false);
        indicatorArea.setBorder(BorderFactory.createTitledBorder("미시작/완료 말"));
        add(new JScrollPane(indicatorArea), BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        logArea.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel buttonControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        randomThrowButton = new JButton("랜덤 윷 던지기");
        randomThrowButton.setEnabled(false); // 게임 시작 전까지 비활성화
        randomThrowButton.addActionListener(e -> {
            if (controller != null) controller.handleThrowRequest(true);
        });
        buttonControlPanel.add(randomThrowButton);

        designatedThrowButton = new JButton("지정 윷 던지기");
        designatedThrowButton.setEnabled(false); // 게임 시작 전까지 비활성화
        designatedThrowButton.addActionListener(e -> {
            if (controller != null) controller.handleThrowRequest(false);
        });
        buttonControlPanel.add(designatedThrowButton);

        bottomPanel.add(buttonControlPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void setGameModel(Game gameModel) {
        this.gameModel = gameModel; // gameModel 참조 저장
        if (this.boardPanel != null) {
            remove(this.boardPanel); // 이전 것이 있으면 제거
        }
        if (gameModel != null && gameModel.getBoard() != null) {
            this.boardPanel = new BoardPanel(gameModel.getBoard()); // 게임 모델로 새 보드 패널 생성
            add(this.boardPanel, BorderLayout.CENTER);
        } else {
            // gameModel 또는 gameModel.getBoard()가 null인 경우 처리 (예: 빈 패널 표시)
            this.boardPanel = new BoardPanel(null); // 혹은 다른 기본값
            add(this.boardPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public void promptForGameSetup() {
        int playerCount = promptForInt("참가자 수 (2~4명):", 2, 4);
        if (playerCount == -1) { // 사용자가 취소함
            System.exit(0);
            return;
        }
        int pieceCount = promptForInt("말 수 (2~5개):", 2, 5);
        if (pieceCount == -1) { // 사용자가 취소함
            System.exit(0);
            return;
        }

        if (controller != null) {
            controller.initializeGame(playerCount, pieceCount);
            randomThrowButton.setEnabled(true); // 게임 시작 시 버튼 활성화
            designatedThrowButton.setEnabled(true);
        }
    }

    private int promptForInt(String message, int min, int max) {
        String input;
        int value;
        while (true) {
            input = JOptionPane.showInputDialog(this, message, min + "~" + max);
            if (input == null) return -1; // 사용자가 취소함
            try {
                value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    JOptionPane.showMessageDialog(this, "입력 범위는 " + min + "에서 " + max + " 사이입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "숫자를 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public YutThrowResult promptForDesignatedThrow() {
        YutThrowResult[] options = YutThrowResult.values();
        String[] labels = Arrays.stream(options).map(Enum::name).toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(
                this, "지정할 윷 결과를 선택하세요:", "지정 윷 던지기",
                JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
        if (selected == null) return null; // 사용자가 취소함
        try {
            return YutThrowResult.fromString(selected);
        } catch (IllegalArgumentException e) {
            showError("잘못된 윷 결과입니다: " + selected);
            return null;
        }
    }

    public Piece promptForPieceSelection(List<Piece> movablePieces, String message) {
        if (movablePieces == null || movablePieces.isEmpty()) return null;

        String[] options = new String[movablePieces.size()];
        for (int i = 0; i < movablePieces.size(); i++) {
            Piece p = movablePieces.get(i);
            String positionInfo = (p.getPosition() == Position.OFFBOARD) ? "출발하지 않은 말" : p.getPosition().name();
            options[i] = String.format("%d번 말 (현재 위치: %s, %s 소유)", i + 1, positionInfo, p.getOwner().getName());
        }

        String selectedOption = (String) JOptionPane.showInputDialog(
                this, message, "말 선택",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (selectedOption == null) return null; // 사용자가 취소함

        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selectedOption)) {
                return movablePieces.get(i);
            }
        }
        return null; // 이론상 발생 안 함
    }

    public YutThrowResult promptForThrowSelection(List<YutThrowResult> availableThrows) {
        if (availableThrows == null || availableThrows.isEmpty()) return null;

        String[] options = availableThrows.stream().map(Enum::name).toArray(String[]::new);
        String message = "어떤 윷 결과를 사용하시겠습니까?\n사용 가능한 결과: " +
                availableThrows.stream().map(Enum::name).collect(Collectors.joining(", "));

        String selectedOption = (String) JOptionPane.showInputDialog(
                this, message, "윷 결과 선택",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (selectedOption == null) return null; // 사용자가 취소함
        try {
            return YutThrowResult.fromString(selectedOption);
        } catch (IllegalArgumentException e) {
            showError("잘못된 윷 결과입니다: " + selectedOption);
            return null;
        }
    }


    public void logMessage(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // 자동 스크롤
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    public void updateStatusLabel(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }
    }

    public void refreshBoard() {
        if (SwingUtilities.isEventDispatchThread()) {
            if (boardPanel != null) {
                boardPanel.repaint();
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                if (boardPanel != null) {
                    boardPanel.repaint();
                }
            });
        }
    }

    public void updateIndicators() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateIndicatorsLogic();
        } else {
            SwingUtilities.invokeLater(this::updateIndicatorsLogic);
        }
    }

    private void updateIndicatorsLogic() {
        if (gameModel == null || gameModel.getPlayers() == null) return;
        StringBuilder sb = new StringBuilder();
        for (Player p : gameModel.getPlayers()) {
            if (p != null && p.getPieces() != null) {
                long offBoardCount = p.getPieces().stream().filter(pc -> pc != null && pc.getPosition() == Position.OFFBOARD).count();
                long finishedCount = p.getPieces().stream().filter(pc -> pc != null && pc.isFinished()).count();
                sb.append(String.format("%s: 대기 %d, 완료 %d\n", p.getName(), offBoardCount, finishedCount));
            }
        }
        indicatorArea.setText(sb.toString());
    }

    public void showInfo(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.INFORMATION_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    public void showError(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE));
        }
    }

    public void showWinMessage(String winnerName) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(this, winnerName + "님이 승리했습니다! 축하합니다!", "게임 종료", JOptionPane.INFORMATION_MESSAGE);
            randomThrowButton.setEnabled(false);
            designatedThrowButton.setEnabled(false);
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, winnerName + "님이 승리했습니다! 축하합니다!", "게임 종료", JOptionPane.INFORMATION_MESSAGE);
                randomThrowButton.setEnabled(false);
                designatedThrowButton.setEnabled(false);
            });
        }
    }
}
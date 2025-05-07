// File: src/frontend/YutGameUI.java
package frontend;

import backend.controller.GameController;
import backend.game.Game;
import backend.game.YutThrowResult;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class YutGameUI extends JFrame {
    private GameController controller;
    private Game gameModel;

    private JLabel statusLabel;
    private JTextArea logArea, indicatorArea;
    private BoardPanel boardPanel;

    private JButton randomThrowButton, designatedThrowButton;
    private JComboBox<String> yutResultChoiceDropdown;
    private JComboBox<String> pieceChoiceDropdown;
    private JButton applyMoveButton;
    private JButton endTurnButton;
    private JPanel actionPanel; // 윷 선택, 말 선택, 이동 버튼을 담을 패널

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            YutGameUI ui = new YutGameUI();
            GameController controller = new GameController(ui);
            ui.setController(controller);
            ui.setVisible(true);
            ui.promptForGameSetup();
        });
    }

    public YutGameUI() {
        super("전통 윷놀이 (MVC + 전략적 선택)");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("게임 설정을 시작하세요.", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new BoardPanel(null);
        add(boardPanel, BorderLayout.CENTER);

        indicatorArea = new JTextArea(10, 15); // 너비 조정
        indicatorArea.setEditable(false);
        indicatorArea.setBorder(BorderFactory.createTitledBorder("게임 현황"));
        add(new JScrollPane(indicatorArea), BorderLayout.WEST);

        // 오른쪽 컨트롤 패널 (던지기 버튼, 액션 패널, 턴 종료 버튼)
        JPanel eastControlPanel = new JPanel();
        eastControlPanel.setLayout(new BoxLayout(eastControlPanel, BoxLayout.Y_AXIS));
        eastControlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 던지기 버튼 패널
        JPanel throwButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        randomThrowButton = new JButton("랜덤 윷 던지기");
        designatedThrowButton = new JButton("지정 윷 던지기");
        throwButtonsPanel.add(randomThrowButton);
        throwButtonsPanel.add(designatedThrowButton);
        eastControlPanel.add(throwButtonsPanel);

        // 액션 패널 (윷 선택, 말 선택, 이동 실행) - 처음에는 숨김
        actionPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // 세로 배치, 컴포넌트간 간격
        actionPanel.setBorder(BorderFactory.createTitledBorder("말 이동 선택"));

        yutResultChoiceDropdown = new JComboBox<>();
        actionPanel.add(new JLabel("사용할 윷 결과:"));
        actionPanel.add(yutResultChoiceDropdown);

        pieceChoiceDropdown = new JComboBox<>();
        actionPanel.add(new JLabel("움직일 말:"));
        actionPanel.add(pieceChoiceDropdown);

        applyMoveButton = new JButton("선택한 대로 이동");
        actionPanel.add(applyMoveButton);
        actionPanel.setVisible(false); // 초기에는 숨김
        eastControlPanel.add(actionPanel);

        // 턴 종료 버튼
        endTurnButton = new JButton("턴 마치기");
        endTurnButton.setEnabled(false); // 초기에는 비활성화
        JPanel endTurnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        endTurnPanel.add(endTurnButton);
        eastControlPanel.add(endTurnPanel);

        add(eastControlPanel, BorderLayout.EAST);


        // 하단 로그 패널
        logArea = new JTextArea(10, 40); // 높이 증가
        logArea.setEditable(false);
        logArea.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane logScrollPane = new JScrollPane(logArea);
        add(logScrollPane, BorderLayout.SOUTH);

        // 초기 버튼 상태 설정
        setGameInteractionEnabled(false); // 게임 시작 전에는 대부분 비활성화

        // 버튼 리스너 연결
        randomThrowButton.addActionListener(e -> {
            if (controller != null) controller.handleThrowRequest(true);
        });
        designatedThrowButton.addActionListener(e -> {
            if (controller != null) controller.handleThrowRequest(false);
        });
        applyMoveButton.addActionListener(e -> applySelectedMoveAction());
        endTurnButton.addActionListener(e -> {
            if (controller != null) controller.decideNextStepAfterTurnActions();
        });
    }

    private void applySelectedMoveAction() {
        if (controller == null) return;

        String selectedYutResultStr = (String) yutResultChoiceDropdown.getSelectedItem();
        String selectedPieceStr = (String) pieceChoiceDropdown.getSelectedItem();

        if (selectedYutResultStr == null || selectedYutResultStr.isEmpty() ||
                selectedPieceStr == null || selectedPieceStr.isEmpty()) {
            showError("윷 결과와 움직일 말을 모두 선택해야 합니다.");
            return;
        }

        YutThrowResult yutResultToApply = null;
        try {
            yutResultToApply = YutThrowResult.fromString(selectedYutResultStr.split(" ")[0]); // "DO (1)" -> "DO"
        } catch (IllegalArgumentException ex) {
            showError("선택된 윷 결과가 유효하지 않습니다.");
            return;
        }

        // "1번 말 (위치: OFFBOARD)" 형식에서 말 객체를 찾아야 함
        // Piece 객체를 직접 JComboBox에 저장하거나, 문자열에서 ID를 파싱해야 함
        // 여기서는 컨트롤러에게 문자열을 전달하고 컨트롤러가 해석하도록 할 수도 있음
        // 임시: 컨트롤러가 말 목록을 주면, 선택된 문자열과 비교하여 Piece 객체 찾기
        List<Piece> movablePieces = controller.getMovablePiecesForCurrentPlayer();
        Piece pieceToMove = null;
        for (Piece p : movablePieces) {
            String pieceDisplay = String.format("%s (위치: %s)",
                    p.getOwner().getName().replace("Player ", "") + "번 말", // "1번 말" 형식으로
                    (p.getPosition() == Position.OFFBOARD ? "출발안함" : p.getPosition().name()));
            // JComboBox에 저장된 형식과 비교해야 함 (아래 updateActionPanelControls 참고)
            // 현재 pieceChoiceDropdown은 인덱스 기반으로 생성되므로, 선택된 인덱스를 사용할 수 있음.
        }
        int selectedPieceIndex = pieceChoiceDropdown.getSelectedIndex();
        if (selectedPieceIndex >= 0 && selectedPieceIndex < movablePieces.size()) {
            pieceToMove = movablePieces.get(selectedPieceIndex);
        }


        if (pieceToMove == null) {
            showError("선택된 말이 유효하지 않습니다.");
            return;
        }
        controller.applySelectedYutAndPiece(yutResultToApply, pieceToMove);
    }


    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void setGameModel(Game gameModel) {
        this.gameModel = gameModel;
        if (this.boardPanel != null) {
            remove(this.boardPanel);
        }
        if (gameModel != null && gameModel.getBoard() != null) {
            this.boardPanel = new BoardPanel(gameModel.getBoard());
            add(this.boardPanel, BorderLayout.CENTER);
        } else {
            this.boardPanel = new BoardPanel(null);
            add(this.boardPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public void promptForGameSetup() {
        int playerCount = promptForInt("참가자 수 (2~4명):", 2, 4);
        if (playerCount == -1) { System.exit(0); return; }
        int pieceCount = promptForInt("말 수 (2~5개):", 2, 5);
        if (pieceCount == -1) { System.exit(0); return; }

        if (controller != null) {
            controller.initializeGame(playerCount, pieceCount);
            setGameInteractionEnabled(true); // 게임 시작 시 버튼 활성화
            actionPanel.setVisible(false); // 말 이동 선택은 아직 숨김
            endTurnButton.setEnabled(false);
        }
    }

    private int promptForInt(String message, int min, int max) {
        // ... (이전과 동일) ...
        String input;
        int value;
        while (true) {
            input = JOptionPane.showInputDialog(this, message, min + "~" + max);
            if (input == null) return -1;
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
        // ... (이전과 동일) ...
        YutThrowResult[] options = YutThrowResult.values();
        String[] labels = Arrays.stream(options).map(Enum::name).toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(
                this, "지정할 윷 결과를 선택하세요:", "지정 윷 던지기",
                JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
        if (selected == null) return null;
        try {
            return YutThrowResult.fromString(selected);
        } catch (IllegalArgumentException e) {
            showError("잘못된 윷 결과입니다: " + selected);
            return null;
        }
    }

    // 이 메소드는 이제 GameController에서 직접 호출되지 않고, UI 업데이트를 통해 간접적으로 사용됨
    // public Piece promptForPieceSelection(List<Piece> movablePieces, String message) { ... }
    // public YutThrowResult promptForThrowSelection(List<YutThrowResult> availableThrows) { ... }


    public void logMessage(String message) {
        // ... (EDT 처리 포함, 이전과 동일) ...
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    public void updateStatusLabel(String text) {
        // ... (EDT 처리 포함, 이전과 동일) ...
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }
    }

    public void refreshBoard() {
        // ... (EDT 처리 포함, 이전과 동일) ...
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
        // ... (EDT 처리 포함, 이전과 동일) ...
        if (SwingUtilities.isEventDispatchThread()) {
            updateIndicatorsLogic();
        } else {
            SwingUtilities.invokeLater(this::updateIndicatorsLogic);
        }
    }

    private void updateIndicatorsLogic() {
        // ... (Null 체크 강화, 이전과 동일) ...
        if (gameModel == null || gameModel.getPlayers() == null) return;
        StringBuilder sb = new StringBuilder();
        for (Player p : gameModel.getPlayers()) {
            if (p != null && p.getPieces() != null) {
                long offBoardCount = p.getPieces().stream().filter(pc -> pc != null && pc.getPosition() == Position.OFFBOARD).count();
                long finishedCount = p.getPieces().stream().filter(pc -> pc != null && pc.isFinished()).count();
                sb.append(String.format("%s: 출발전 %d, 완료 %d\n", p.getName(), offBoardCount, finishedCount));
            }
        }
        indicatorArea.setText(sb.toString());
    }

    public void showInfo(String message) { /* ... (EDT 처리) ... */ SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.INFORMATION_MESSAGE));}
    public void showError(String message) { /* ... (EDT 처리) ... */ SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE));}

    public void showWinMessage(String winnerName) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, winnerName + "님이 승리했습니다! 축하합니다!", "게임 종료", JOptionPane.INFORMATION_MESSAGE);
            setGameInteractionEnabled(false);
            actionPanel.setVisible(false);
            endTurnButton.setEnabled(false);
        });
    }

    // GameController가 UI 상태를 업데이트하도록 호출할 메소드들
    public void enableThrowButtons(boolean enable) {
        SwingUtilities.invokeLater(() -> {
            randomThrowButton.setEnabled(enable);
            designatedThrowButton.setEnabled(enable);
        });
    }

    public void showActionPanel(boolean show, List<YutThrowResult> availableThrows, List<Piece> movablePieces) {
        SwingUtilities.invokeLater(() -> {
            actionPanel.setVisible(show);
            endTurnButton.setEnabled(show); // 액션 패널 보일 때 턴 종료 버튼도 활성화/비활성화

            if (show) {
                // 윷 결과 드롭다운 업데이트
                Vector<String> yutResultsVector = new Vector<>();
                if (availableThrows != null) {
                    for (YutThrowResult yr : availableThrows) {
                        yutResultsVector.add(yr.name() + " (" + yr.getMove() + "칸)");
                    }
                }
                yutResultChoiceDropdown.setModel(new DefaultComboBoxModel<>(yutResultsVector));

                // 말 선택 드롭다운 업데이트
                Vector<String> piecesVector = new Vector<>();
                if (movablePieces != null) {
                    for (int i = 0; i < movablePieces.size(); i++) {
                        Piece p = movablePieces.get(i);
                        // 간단하게 "플레이어ID-말번호 (위치)" 형식으로 표시
                        // 또는 Player 객체에서 말의 고유 ID를 가져와 사용할 수도 있음
                        String pieceDisplayName = String.format("%s-%d번말 (%s)",
                                p.getOwner().getName().replaceAll("[^0-9]", ""), // "Player 1" -> "1"
                                i + 1, // 단순 인덱스 기반 번호
                                p.getPosition() == Position.OFFBOARD ? "출발안함" : p.getPosition().name());
                        piecesVector.add(pieceDisplayName);
                    }
                }
                pieceChoiceDropdown.setModel(new DefaultComboBoxModel<>(piecesVector));

                applyMoveButton.setEnabled(!yutResultsVector.isEmpty() && !piecesVector.isEmpty());
            } else {
                yutResultChoiceDropdown.removeAllItems();
                pieceChoiceDropdown.removeAllItems();
                applyMoveButton.setEnabled(false);
            }
        });
    }

    private void setGameInteractionEnabled(boolean enabled) {
        randomThrowButton.setEnabled(enabled);
        designatedThrowButton.setEnabled(enabled);
        // applyMoveButton, endTurnButton 등은 상황에 따라 controller가 직접 제어
    }
}
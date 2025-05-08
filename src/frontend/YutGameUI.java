// File: src/frontend/YutGameUI.java
package frontend;

import backend.controller.GameController;
import backend.game.Game;
import backend.game.YutThrowResult;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;
import backend.model.BoardShape;

import javax.swing.*;
import java.awt.*;
// import java.awt.event.ActionEvent; // 현재 직접 사용 안함
// import java.awt.event.ActionListener; // 현재 직접 사용 안함
import java.util.ArrayList;
import java.util.Arrays; // promptForDesignatedThrow에서 사용
import java.util.List;
import java.util.Vector; // JComboBox 모델용
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
    private JButton endTurnButton; // "턴 마치기" 버튼
    private JPanel actionPanel; // 윷 선택, 말 선택, 이동 버튼을 담을 패널
    private BoardShape selectedBoardShape = BoardShape.TRADITIONAL;

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            YutGameUI ui = new YutGameUI();
            ui.promptForGameSetup(); // 게임 설정 시작
        });
    }

    public YutGameUI() {
        super("전통 윷놀이 (MVC + 전략적 선택)");
        setSize(1200, 800); // 너비 약간 늘림
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // 컴포넌트 간 간격 추가

        statusLabel = new JLabel("게임 설정을 시작하세요.", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new BoardPanel(null); // 초기에는 null 보드
        add(boardPanel, BorderLayout.CENTER);

        indicatorArea = new JTextArea(10, 15); // 너비 조정
        indicatorArea.setEditable(false);
        indicatorArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // 가독성 위한 폰트
        indicatorArea.setBorder(BorderFactory.createTitledBorder("게임 현황"));
        add(new JScrollPane(indicatorArea), BorderLayout.WEST); // 위치 변경 (왼쪽)

        // 오른쪽 컨트롤 패널 (던지기 버튼, 액션 패널, 턴 종료 버튼)
        JPanel eastControlPanel = new JPanel();
        eastControlPanel.setLayout(new BoxLayout(eastControlPanel, BoxLayout.Y_AXIS)); // 세로 정렬
        eastControlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 던지기 버튼 패널
        JPanel throwButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        randomThrowButton = new JButton("랜덤 윷 던지기");
        designatedThrowButton = new JButton("지정 윷 던지기");
        throwButtonsPanel.add(randomThrowButton);
        throwButtonsPanel.add(designatedThrowButton);
        eastControlPanel.add(throwButtonsPanel);

        // 액션 패널 (윷 선택, 말 선택, 이동 실행)
        actionPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // 세로 배치, 컴포넌트간 간격
        actionPanel.setBorder(BorderFactory.createTitledBorder("말 이동 선택"));

        yutResultChoiceDropdown = new JComboBox<>();
        actionPanel.add(new JLabel("사용할 윷 결과:"));
        actionPanel.add(yutResultChoiceDropdown);

        pieceChoiceDropdown = new JComboBox<>();
        actionPanel.add(new JLabel("움직일 말:"));
        actionPanel.add(pieceChoiceDropdown);

        applyMoveButton = new JButton("선택한 대로 이동 실행");
        actionPanel.add(applyMoveButton);
        actionPanel.setVisible(false); // 초기에는 숨김
        eastControlPanel.add(actionPanel);

        // 턴 종료 버튼
        endTurnButton = new JButton("턴 마치기 / 윷 사용 포기");
        endTurnButton.setEnabled(false); // 초기에는 비활성화
        JPanel endTurnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        endTurnPanel.add(endTurnButton);
        eastControlPanel.add(endTurnPanel);

        add(eastControlPanel, BorderLayout.EAST);


        // 하단 로그 패널
        logArea = new JTextArea(10, 40); // 높이 증가
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
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
        endTurnButton.addActionListener(e -> { // 수정된 리스너 연결
            if (controller != null) controller.playerEndsTurnActions();
        });
    }

    private void applySelectedMoveAction() {
        if (controller == null) return;

        String selectedYutResultStr = (String) yutResultChoiceDropdown.getSelectedItem();
        // String selectedPieceStr = (String) pieceChoiceDropdown.getSelectedItem(); // 이 방식 대신 인덱스 사용

        if (selectedYutResultStr == null || selectedYutResultStr.isEmpty() ||
                pieceChoiceDropdown.getSelectedIndex() == -1 ) { // 선택된 말이 없는 경우
            showError("윷 결과와 움직일 말을 모두 선택해야 합니다.");
            return;
        }

        YutThrowResult yutResultToApply = null;
        try {
            // "DO (1칸)" 같은 형식에서 "DO"만 추출
            yutResultToApply = YutThrowResult.fromString(selectedYutResultStr.split(" ")[0]);
        } catch (IllegalArgumentException ex) {
            showError("선택된 윷 결과가 유효하지 않습니다: " + selectedYutResultStr);
            return;
        }

        List<Piece> movablePieces = controller.getMovablePiecesForCurrentPlayer();
        int selectedPieceIndex = pieceChoiceDropdown.getSelectedIndex();
        Piece pieceToMove = null;

        if (selectedPieceIndex >= 0 && selectedPieceIndex < movablePieces.size()) {
            pieceToMove = movablePieces.get(selectedPieceIndex);
        }

        if (pieceToMove == null) {
            showError("선택된 말이 유효하지 않습니다. (리스트 인덱스 문제)");
            return;
        }
        controller.applySelectedYutAndPiece(yutResultToApply, pieceToMove);
    }


    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void setGameModel(Game gameModel) {
    	this.gameModel = gameModel;
        remove(boardPanel);
        boardPanel = new BoardPanel(gameModel != null ? gameModel.getBoard() : null);
        boardPanel.setBoardShape(selectedBoardShape);
        add(boardPanel, BorderLayout.CENTER);
        revalidate(); repaint();
    }
    
    private void setMainUIVisible(boolean visible) {
        statusLabel.setVisible(visible);
        boardPanel.setVisible(visible);
    }

    public void promptForGameSetup() {
    	setVisible(true);
        promptForBoardShape();
        int players = promptForInt("참가자 수(2~4):", 2, 4);
        int pieces  = promptForInt("말 수(2~5):", 2, 5);
        if (players < 0 || pieces < 0) System.exit(0);
        controller = new GameController(this, selectedBoardShape);
        controller.initializeGame(players, pieces);
        setGameInteractionEnabled(true);
    }
    
    private void promptForBoardShape() {
        Object[] options = {"사각형", "오각형", "육각형"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "보드 모양을 선택하세요:",
                "보드 모양 선택",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        switch (choice) {
            case 1 -> selectedBoardShape = BoardShape.PENTAGON;
            case 2 -> selectedBoardShape = BoardShape.HEXAGON;
            default -> selectedBoardShape = BoardShape.TRADITIONAL;
        }
    }

    private int promptForInt(String message, int min, int max) {
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

    public void logMessage(String message) {
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
        if (gameModel == null || gameModel.getPlayers() == null) {
            indicatorArea.setText(""); // 데이터 없으면 비움
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== 현재 턴 ===").append("\n");
        if (gameModel.getCurrentPlayer() != null) {
            sb.append(gameModel.getCurrentPlayer().getName()).append("\n\n");
        }
        sb.append("=== 말 현황 ===").append("\n");
        for (Player p : gameModel.getPlayers()) {
            if (p != null && p.getPieces() != null) {
                long offBoardCount = p.getPieces().stream().filter(pc -> pc != null && pc.getPosition() == Position.OFFBOARD).count();
                long finishedCount = p.getPieces().stream().filter(pc -> pc != null && pc.isFinished()).count();
                sb.append(String.format("%s: 출발전 %d, 완료 %d\n", p.getName(), offBoardCount, finishedCount));
            }
        }
        indicatorArea.setText(sb.toString());
    }

    public void showInfo(String message) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.INFORMATION_MESSAGE));}
    public void showError(String message) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE));}

    public void showWinMessage(String winnerName) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, winnerName + "님이 승리했습니다! 축하합니다!", "게임 종료", JOptionPane.INFORMATION_MESSAGE);
            setGameInteractionEnabled(false);
            actionPanel.setVisible(false);
            endTurnButton.setEnabled(false);
        });
    }

    public void enableThrowButtons(boolean enable) {
        SwingUtilities.invokeLater(() -> {
            randomThrowButton.setEnabled(enable);
            designatedThrowButton.setEnabled(enable);
        });
    }

    public void showActionPanel(boolean show, List<YutThrowResult> availableThrows, List<Piece> movablePieces) {
        SwingUtilities.invokeLater(() -> {
            actionPanel.setVisible(show);
            endTurnButton.setEnabled(show); // 액션 패널이 보일 때(즉, 행동 선택이 필요할 때) 턴 종료 버튼도 함께 제어

            if (show) {
                Vector<String> yutResultsVector = new Vector<>();
                if (availableThrows != null) {
                    for (YutThrowResult yr : availableThrows) {
                        yutResultsVector.add(yr.name() + " (" + yr.getMove() + "칸)");
                    }
                }
                yutResultChoiceDropdown.setModel(new DefaultComboBoxModel<>(yutResultsVector));

                Vector<String> piecesVector = new Vector<>();
                if (movablePieces != null) {
                    // 각 Piece 객체에 고유 ID가 있다면 그것을 사용하는 것이 더 안정적일 수 있음
                    // 여기서는 임시로 플레이어 번호와 리스트 인덱스를 조합하여 표시
                    for (int i = 0; i < movablePieces.size(); i++) {
                        Piece p = movablePieces.get(i);
                        String playerNumber = p.getOwner().getName().replaceAll("[^0-9]", ""); // "Player 1" -> "1"
                        String pieceId = playerNumber + "-" + (i + 1); // 예: "1-1번말", "2-1번말"
                        String positionName = (p.getPosition() == Position.OFFBOARD) ? "출발안함" : p.getPosition().name();
                        piecesVector.add(String.format("%s번 말 (위치: %s)", pieceId, positionName));
                    }
                }
                pieceChoiceDropdown.setModel(new DefaultComboBoxModel<>(piecesVector));

                // 이동 실행 버튼은 선택할 윷과 말이 있을 때만 활성화
                applyMoveButton.setEnabled(!yutResultsVector.isEmpty() && !piecesVector.isEmpty());
            } else {
                yutResultChoiceDropdown.removeAllItems();
                pieceChoiceDropdown.removeAllItems();
                applyMoveButton.setEnabled(false);
            }
        });
    }

    // 게임 시작/종료 시 전체 인터랙션 버튼 상태 제어
    private void setGameInteractionEnabled(boolean enabled) {
        randomThrowButton.setEnabled(enabled);
        designatedThrowButton.setEnabled(enabled);
        // applyMoveButton과 endTurnButton은 showActionPanel에서 상황에 맞게 제어됨
    }
}
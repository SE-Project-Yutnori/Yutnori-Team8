package frontend;

import backend.controller.GameController;
import backend.game.Game;
import backend.game.YutThrowResult;
import backend.model.BoardShape;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

public class YutGameJavaFXUI extends Application implements YutGameUIInterface {
    private GameController controller;
    private Game gameModel;
    private Stage primaryStage;
    
    private Label statusLabel;
    private TextArea logArea;
    private TextArea indicatorArea;
    private BorderPane mainLayout;
    private VBox actionPanel;
    private ComboBox<String> yutResultChoiceDropdown;
    private ComboBox<String> pieceChoiceDropdown;
    private Button randomThrowButton;
    private Button designatedThrowButton;
    private Button applyMoveButton;
    private Button endTurnButton;
    private BoardShape selectedBoardShape = BoardShape.TRADITIONAL;
    private JavaFXBoardPanel boardPanel;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("전통 윷놀이 (JavaFX)");
        
        // 메인 레이아웃 설정
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));
        
        // 상단 상태 레이블
        statusLabel = new Label("게임 설정을 시작하세요.");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setAlignment(Pos.CENTER);
        mainLayout.setTop(statusLabel);
        
        // 중앙 보드 패널
        boardPanel = new JavaFXBoardPanel(null);
        mainLayout.setCenter(boardPanel);
        
        // 왼쪽 인디케이터 영역
        indicatorArea = new TextArea();
        indicatorArea.setEditable(false);
        indicatorArea.setPrefRowCount(10);
        indicatorArea.setPrefColumnCount(15);
        indicatorArea.setFont(Font.font("Monospaced", 12));
        ScrollPane indicatorScroll = new ScrollPane(indicatorArea);
        indicatorScroll.setFitToWidth(true);
        mainLayout.setLeft(indicatorScroll);
        
        // 오른쪽 컨트롤 패널
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        
        // 던지기 버튼
        HBox throwButtons = new HBox(10);
        randomThrowButton = new Button("랜덤 윷 던지기");
        designatedThrowButton = new Button("지정 윷 던지기");
        throwButtons.getChildren().addAll(randomThrowButton, designatedThrowButton);
        controlPanel.getChildren().add(throwButtons);
        
        // 액션 패널
        actionPanel = new VBox(5);
        actionPanel.setPadding(new Insets(10));
        actionPanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.GRAY, 
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        
        yutResultChoiceDropdown = new ComboBox<>();
        pieceChoiceDropdown = new ComboBox<>();
        applyMoveButton = new Button("선택한 대로 이동 실행");
        endTurnButton = new Button("턴 마치기 / 윷 사용 포기");
        
        actionPanel.getChildren().addAll(
            new Label("사용할 윷 결과:"),
            yutResultChoiceDropdown,
            new Label("움직일 말:"),
            pieceChoiceDropdown,
            applyMoveButton,
            endTurnButton
        );
        actionPanel.setVisible(false);
        controlPanel.getChildren().add(actionPanel);
        
        mainLayout.setRight(controlPanel);
        
        // 하단 로그 영역
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(5);
        logArea.setFont(Font.font("Monospaced", 12));
        ScrollPane logScroll = new ScrollPane(logArea);
        logScroll.setFitToWidth(true);
        mainLayout.setBottom(logScroll);
        
        // 버튼 이벤트 핸들러 설정
        randomThrowButton.setOnAction(e -> {
            if (controller != null) controller.handleThrowRequest(true);
        });
        
        designatedThrowButton.setOnAction(e -> {
            if (controller != null) controller.handleThrowRequest(false);
        });
        
        applyMoveButton.setOnAction(e -> applySelectedMoveAction());
        
        endTurnButton.setOnAction(e -> {
            if (controller != null) controller.playerEndsTurnActions();
        });
        
        // 씬 설정
        Scene scene = new Scene(mainLayout, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 게임 설정 시작
        promptForGameSetup();
    }

    @Override
    public void setController(GameController controller) {
        this.controller = controller;
    }

    @Override
    public void setGameModel(Game gameModel) {
        this.gameModel = gameModel;
        boardPanel = new JavaFXBoardPanel(gameModel != null ? gameModel.getBoard() : null);
        boardPanel.setBoardShape(selectedBoardShape);
        mainLayout.setCenter(boardPanel);
    }

    @Override
    public void promptForGameSetup() {
        // 보드 모양 선택
        List<String> options = List.of("사각형", "오각형", "육각형");
        ChoiceDialog<String> boardDialog = new ChoiceDialog<>(options.get(0), options);
        boardDialog.setTitle("보드 모양 선택");
        boardDialog.setHeaderText(null);
        boardDialog.setContentText("보드 모양을 선택하세요:");
        
        Optional<String> boardResult = boardDialog.showAndWait();
        if (boardResult.isPresent()) {
            switch (boardResult.get()) {
                case "오각형" -> selectedBoardShape = BoardShape.PENTAGON;
                case "육각형" -> selectedBoardShape = BoardShape.HEXAGON;
                default -> selectedBoardShape = BoardShape.TRADITIONAL;
            }
        }
        
        // 플레이어 수 입력
        TextInputDialog playerDialog = new TextInputDialog("2");
        playerDialog.setTitle("플레이어 수");
        playerDialog.setHeaderText(null);
        playerDialog.setContentText("참가자 수(2~4):");
        
        int players = 2;
        Optional<String> playerResult = playerDialog.showAndWait();
        if (playerResult.isPresent()) {
            try {
                players = Integer.parseInt(playerResult.get());
                if (players < 2 || players > 4) players = 2;
            } catch (NumberFormatException e) {
                players = 2;
            }
        }
        
        // 말 수 입력
        TextInputDialog pieceDialog = new TextInputDialog("2");
        pieceDialog.setTitle("말 수");
        pieceDialog.setHeaderText(null);
        pieceDialog.setContentText("말 수(2~5):");
        
        int pieces = 2;
        Optional<String> pieceResult = pieceDialog.showAndWait();
        if (pieceResult.isPresent()) {
            try {
                pieces = Integer.parseInt(pieceResult.get());
                if (pieces < 2 || pieces > 5) pieces = 2;
            } catch (NumberFormatException e) {
                pieces = 2;
            }
        }
        
        controller = new GameController(this, selectedBoardShape);
        controller.initializeGame(players, pieces);
    }

    @Override
    public void updateStatusLabel(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    @Override
    public void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    @Override
    public void refreshBoard() {
        Platform.runLater(() -> {
            if (boardPanel != null) {
                boardPanel.drawBoard();
            }
        });
    }

    @Override
    public void updateIndicators() {
        Platform.runLater(() -> {
            if (gameModel == null || gameModel.getPlayers() == null) {
                indicatorArea.setText("");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== 현재 턴 ===\n");
            if (gameModel.getCurrentPlayer() != null) {
                sb.append(gameModel.getCurrentPlayer().getName()).append("\n\n");
            }
            sb.append("=== 말 현황 ===\n");
            for (Player p : gameModel.getPlayers()) {
                if (p != null && p.getPieces() != null) {
                    long offBoardCount = p.getPieces().stream()
                        .filter(pc -> pc != null && pc.getPosition() == Position.OFFBOARD)
                        .count();
                    long finishedCount = p.getPieces().stream()
                        .filter(pc -> pc != null && pc.isFinished())
                        .count();
                    sb.append(String.format("%s: 출발전 %d, 완료 %d\n", 
                        p.getName(), offBoardCount, finishedCount));
                }
            }
            indicatorArea.setText(sb.toString());
        });
    }

    @Override
    public void enableThrowButtons(boolean enable) {
        Platform.runLater(() -> {
            randomThrowButton.setDisable(!enable);
            designatedThrowButton.setDisable(!enable);
        });
    }

    @Override
    public void showActionPanel(boolean show, List<YutThrowResult> availableThrows, List<Piece> movablePieces) {
        Platform.runLater(() -> {
            actionPanel.setVisible(show);
            endTurnButton.setDisable(!show);
            
            if (show) {
                ObservableList<String> yutResults = FXCollections.observableArrayList();
                if (availableThrows != null) {
                    for (YutThrowResult yr : availableThrows) {
                        yutResults.add(yr.name() + " (" + yr.getMove() + "칸)");
                    }
                }
                yutResultChoiceDropdown.setItems(yutResults);
                
                ObservableList<String> pieces = FXCollections.observableArrayList();
                if (movablePieces != null) {
                    for (int i = 0; i < movablePieces.size(); i++) {
                        Piece p = movablePieces.get(i);
                        String playerNumber = p.getOwner().getName().replaceAll("[^0-9]", "");
                        String pieceId = playerNumber + "-" + (i + 1);
                        String positionName = (p.getPosition() == Position.OFFBOARD) ? 
                            "출발안함" : p.getPosition().name();
                        pieces.add(String.format("%s번 말 (위치: %s)", pieceId, positionName));
                    }
                }
                pieceChoiceDropdown.setItems(pieces);
                
                applyMoveButton.setDisable(yutResults.isEmpty() || pieces.isEmpty());
            } else {
                yutResultChoiceDropdown.getItems().clear();
                pieceChoiceDropdown.getItems().clear();
                applyMoveButton.setDisable(true);
            }
        });
    }

    @Override
    public YutThrowResult promptForDesignatedThrow() {
        List<String> options = Arrays.stream(YutThrowResult.values())
            .map(Enum::name)
            .collect(Collectors.toList());
            
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("지정 윷 던지기");
        dialog.setHeaderText(null);
        dialog.setContentText("지정할 윷 결과를 선택하세요:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                return YutThrowResult.fromString(result.get());
            } catch (IllegalArgumentException e) {
                showError("잘못된 윷 결과입니다: " + result.get());
                return null;
            }
        }
        return null;
    }

    @Override
    public void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("알림");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("오류");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void showWinMessage(String winnerName) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("게임 종료");
            alert.setHeaderText(null);
            alert.setContentText(winnerName + "님이 승리했습니다!\n새 게임을 시작하시겠습니까?");
            
            ButtonType newGameButton = new ButtonType("새 게임 시작");
            ButtonType exitButton = new ButtonType("프로그램 종료");
            alert.getButtonTypes().setAll(newGameButton, exitButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == newGameButton) {
                    // 로그와 인디케이터 초기화
                    logArea.setText("");
                    indicatorArea.setText("");
                    // TODO: 보드 패널 초기화
                    promptForGameSetup();
                } else {
                    Platform.exit();
                }
            }
        });
    }

    private void applySelectedMoveAction() {
        if (controller == null) return;

        String selectedYutResultStr = yutResultChoiceDropdown.getValue();
        if (selectedYutResultStr == null || selectedYutResultStr.isEmpty() ||
                pieceChoiceDropdown.getSelectionModel().getSelectedIndex() == -1) {
            showError("윷 결과와 움직일 말을 모두 선택해야 합니다.");
            return;
        }

        YutThrowResult yutResultToApply = null;
        try {
            yutResultToApply = YutThrowResult.fromString(selectedYutResultStr.split(" ")[0]);
        } catch (IllegalArgumentException ex) {
            showError("선택된 윷 결과가 유효하지 않습니다: " + selectedYutResultStr);
            return;
        }

        List<Piece> movablePieces = controller.getMovablePiecesForCurrentPlayer();
        int selectedPieceIndex = pieceChoiceDropdown.getSelectionModel().getSelectedIndex();
        Piece pieceToMove = null;

        if (selectedPieceIndex >= 0 && selectedPieceIndex < movablePieces.size()) {
            pieceToMove = movablePieces.get(selectedPieceIndex);
        }

        if (pieceToMove == null) {
            showError("선택된 말이 유효하지 않습니다.");
            return;
        }
        controller.applySelectedYutAndPiece(yutResultToApply, pieceToMove);
    }
} 
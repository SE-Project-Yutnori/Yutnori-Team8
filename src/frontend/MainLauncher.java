package frontend;

import javafx.application.Application;
import javax.swing.SwingUtilities;

public class MainLauncher {
    public static void main(String[] args) {
        // UI 선택 다이얼로그 표시
        String[] options = {"Swing UI", "JavaFX UI"};
        int choice = javax.swing.JOptionPane.showOptionDialog(
            null,
            "사용할 UI를 선택하세요:",
            "UI 선택",
            javax.swing.JOptionPane.DEFAULT_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 0) {
            // Swing UI 실행
            SwingUtilities.invokeLater(() -> {
                YutGameUI ui = new YutGameUI();
                ui.promptForGameSetup();
            });
        } else if (choice == 1) {
            // JavaFX UI 실행
            try {
                Application.launch(YutGameJavaFXUI.class, args);
            } catch (Exception e) {
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(null, 
                    "JavaFX 초기화 중 오류가 발생했습니다: " + e.getMessage(),
                    "오류",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

package frontend;

import backend.model.BoardShape;

/**
 * 보드 패널의 공통 인터페이스
 */
public interface BoardPanelInterface {
    /**
     * 보드 모양을 설정합니다.
     * @param shape 설정할 보드 모양
     */
    void setBoardShape(BoardShape shape);

    /**
     * 현재 보드 모양을 반환합니다.
     * @return 현재 보드 모양
     */
    BoardShape getBoardShape();

    /**
     * 보드를 다시 그립니다.
     */
    void refresh();

    /**
     * 보드 좌표를 초기화합니다.
     */
    void initializeCoords();
}
/**
 * 
 */
/**
 * 
 */
module yutnori {
	requires java.desktop;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.base;
	exports frontend;
	exports backend.controller;
	exports backend.game;
	exports backend.model;
	opens frontend to javafx.graphics;
	opens backend.controller to javafx.graphics;
	opens backend.game to javafx.graphics;
	opens backend.model to javafx.graphics;
	requires org.junit.jupiter.api;
}
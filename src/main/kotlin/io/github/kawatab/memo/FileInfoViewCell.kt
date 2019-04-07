/**
 * memo/FileInfoViewCell.kt - file info cell in list view
 *
 * Copyright (c) 2019 Yasuhiro Yamakawa <kawatab@yahoo.co.jp>
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 */

package io.github.kawatab.memo

import java.io.IOException
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.VBox
import javafx.scene.layout.Priority
import javafx.scene.Parent


class FileInfoViewCell() : ListCell<FileInfoModel>() {
    private val cellContainer = VBox(5.0)
    @FXML lateinit private var preview: Label
    @FXML lateinit private var date: Label
    private var bound = false

    init {
	val fxmlLoader = FXMLLoader(FileInfoViewCell::class.java.getResource("/fileinfoviewcell.fxml"))
	fxmlLoader.setController(this)
	try {
	    fxmlLoader.load<Parent>()
	} catch (e: IOException) {
	    throw RuntimeException(e)
	}
  
	cellContainer.setPrefHeight(64.0)
	cellContainer.setMaxHeight(64.0)

	initComponent()
    }

    private fun initComponent() {
	VBox.setVgrow(preview, Priority.ALWAYS)
	VBox.setVgrow(date, Priority.ALWAYS)
	cellContainer.getChildren().addAll(preview, date)
    }

    override fun updateItem(info: FileInfoModel?, empty: Boolean) {
	super.updateItem(info, empty)

	if (!bound) {
	    preview.prefWidthProperty().bind(getListView().widthProperty().subtract(25))
	    date.prefWidthProperty().bind(getListView().widthProperty().subtract(25))
	    
	    bound = true
	}

	preview.textProperty().unbind()
	date.textProperty().unbind()

	if (empty) {
	    setText(null)
	    setGraphic(null)
	} else {
	    preview.textProperty().bind(info?.previewProperty)
	    date.textProperty().bind(info?.dateProperty)
	    setGraphic(cellContainer)
	}
    }
}

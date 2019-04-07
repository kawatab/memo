/**
 * memo/WindowController.kt - window controller class
 *
 * Copyright (c) 2019 Yasuhiro Yamakawa <kawatab@yahoo.co.jp>
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 */

package io.github.kawatab.memo

import java.net.URL
import java.util.ResourceBundle
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.control.SelectionModel
import javafx.scene.control.TextArea
import javafx.util.Callback


class WindowController(memo: Memo) : Initializable {
    @FXML lateinit private var folderChoice: ChoiceBox<String>
    @FXML lateinit private var newButton: Button
    @FXML lateinit private var moveButton: Button
    @FXML lateinit private var cutButton: Button
    @FXML lateinit private var pasteButton: Button
    @FXML lateinit private var textArea: TextArea
    @FXML lateinit private var listView: ListView<FileInfoModel>

    private val activeFileList: ObservableList<FileInfoModel> = FXCollections.observableArrayList()
    private val archiveFileList: ObservableList<FileInfoModel> = FXCollections.observableArrayList()
    private lateinit var selectedItemListener: ChangeListener<FileInfoModel>
    private var currentFileList = activeFileList
    
    private val memo = memo
    private var textChanged = false
    private var orderChanged = false
    private var readyToSave = false
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
	memo.getActiveFileList().forEach { activeFileList.add(it) }
	memo.getArchiveFileList().forEach { archiveFileList.add(it) }
	prepareChoiceBox()
	prepareTextArea()
	prepareListView()
    }

    // for buttons
    fun clickSelectAll(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
	textArea.selectAll()
    }
    
    fun clickCutText(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
	textArea.cut()
    }

    fun clickCopyText(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
	textArea.copy()
    }
    
    fun clickPaste(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
	textArea.paste()
    }

    fun clickNew(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
	val currentFile = listView.getSelectionModel().getSelectedItem()

	runCatching {
	    saveFile(currentFile)
	}.onFailure {
	    println("Failed to create a new file.")
	}.onSuccess {
	    println("Saved successfully before creating a new file.")
	    createNewFile()
	}
    }

    fun clickMove(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
	val selectedItem = listView.getSelectionModel().getSelectedItem()

	runCatching {
	    memo.moveCurrentFile(selectedItem)
	}.onFailure {
	    println("Failed to move.")
	}.onSuccess {
	    moveToFolder(selectedItem)
	}
    }
	    
    // for folderChoice
    private fun prepareChoiceBox() {
	val selectionModel = folderChoice.getSelectionModel()
	folderChoice.getItems().addAll("Active", "Archive");
	selectionModel.selectFirst()

	selectionModel.selectedItemProperty().addListener(object: ChangeListener<String> {
	    override fun changed(observable: ObservableValue<out String>,
	    oldValue: String?, newValue: String?) {
		(newValue != null && newValue == "Active").let {
		    textArea.setEditable(it)
		    newButton.setDisable(!it)
		    cutButton.setDisable(!it)
		    pasteButton.setDisable(!it)

		    currentFileList = if (it) {
			activeFileList
		    } else {
			val currentListView = listView.getSelectionModel().getSelectedItem()
			savePreviousFile(currentListView)
			archiveFileList
		    }
		}

		changeFolder()
	    }
	})
    }

    // for listView
    private fun prepareListView() {
	selectedItemListener = object: ChangeListener<FileInfoModel> {
	    override fun changed(observable: ObservableValue<out FileInfoModel>,
	    oldValue: FileInfoModel?, newValue: FileInfoModel?) {
		if (oldValue !== newValue) {
		    saveOrDeletePreviousFile(oldValue)
		    memo.currentFile = newValue?.path
		    loadCurrentFile()
		}
	    }
	}

	listView.setItems(currentFileList)

	// set custom cell
	listView.cellFactory = Callback<ListView<FileInfoModel>, ListCell<FileInfoModel>> { FileInfoViewCell() }

	val selectionModel = listView.getSelectionModel()
	selectionModel.setSelectionMode(SelectionMode.SINGLE)
	
	selectionModel.selectedItemProperty().addListener(selectedItemListener)

	when {
	    currentFileList.size > 0 -> Platform.runLater {
		doWithDisabledListener { FXCollections.sort(currentFileList, FileInfoModel.firstNewest) }
		selectionModel.selectFirst()
	    }
	    else -> createNewFile()
	}
    }

    private fun saveOrDeletePreviousFile(oldValue: FileInfoModel?) {
	if (currentFileList === activeFileList) {
	    when {
		textArea.text.trim().isEmpty() -> deleteEmptyFile(oldValue)
		textChanged -> savePreviousFile(oldValue)
	    }
	}
    }

    private fun savePreviousFile(oldValue: FileInfoModel?) {
	runCatching {
	    oldValue?.let { saveFile(it) }
	}.onFailure {
	    println("Didn't save before switching file.")
	}.onSuccess {
	    println("Saved successfully before switching file.")
	}
    }
    
    private fun deleteEmptyFile(oldValue: FileInfoModel?) {
	Platform.runLater {
	    runCatching {
		oldValue?.let {
		    memo.deleteFile(it.path)
		    currentFileList.remove(it)
		}
	    }.onFailure {
		println("Failed to delete empty file before switching file.")
	    }
	}
    }

    private fun doWithDisabledListener(function: () -> Unit) {
	val selectedItemProperty = listView.getSelectionModel().selectedItemProperty()
	selectedItemProperty.removeListener(selectedItemListener)
	function()
	
	selectedItemProperty.addListener(selectedItemListener)
    }
    
    private fun moveToFolder(selectedItem: FileInfoModel) {
	Platform.runLater {
	    val selectionModel = listView.getSelectionModel()
	    val selectedIndex = selectionModel.getSelectedIndex()

	    currentFileList.removeAt(selectedIndex)

	    (if (currentFileList === activeFileList) archiveFileList else activeFileList).add(selectedItem)

	    when {
		currentFileList.size > 0 -> {
		    textChanged = false
		    selectionModel.select(minOf(selectedIndex, currentFileList.size))
		}
		currentFileList === activeFileList -> createNewFile()
		else -> clearText()
	    }
	}
    }

    private fun changeFolder() {
	Platform.runLater {
	    (currentFileList.size > 0).let {
		moveButton.setDisable(!it)
		
		when {
		    it -> {
			FXCollections.sort(currentFileList, FileInfoModel.firstNewest)
			doWithDisabledListener { listView.setItems(currentFileList) }
			listView.getSelectionModel().selectFirst()
		    }
		    else -> clearText()
		}
	    }
	}
    }

    // for textArea
    private fun prepareTextArea() {
	textArea.textProperty().addListener(object: ChangeListener<String> {
	    override fun changed(observableValue: ObservableValue<out String>, s: String, s2: String) {
		moveButton.setDisable(s2.isEmpty())
		textChanged = true
		readyToSave = false
	    }
	})
    }

    private fun clearText() {
	textArea.text = ""
	textChanged = false
	readyToSave = false
	moveButton.setDisable(true)
    }
    
    // File Access
    private fun createNewFile() {
	runCatching {
	    memo.createFile()?.let {
		doWithDisabledListener({currentFileList.add(0, memo.createFileInfo(it))})
		listView.getSelectionModel().selectFirst()
		memo.currentFile = it
	    }
	}.onFailure {
	    println("Failed to create new file.")
	}.onSuccess {
	    println("Created a new file successfully.")
	    clearText()
	}
    }
    
    private fun saveFile(fileInfo: FileInfoModel) {
	if (textChanged) {
	    val lines = textArea.text.split("\n")
	    
	    runCatching {
		memo.writeFile(fileInfo.path, lines)
	    }.onFailure {
		println("Failed to save file.")
		throw it
	    }.onSuccess {
		textChanged = false
		readyToSave = false
		orderChanged = !textArea.text.trim().isEmpty() && listView.getSelectionModel().getSelectedIndex() > 0
		memo.updateFileInfo(fileInfo)
	    }
	}
    }
    
    private fun loadCurrentFile() {
	runCatching<String> {
	    memo.readCurrentFile()
	}.onFailure {
	    println("Failed to load file.")
	}.onSuccess {
	    textArea.text = it
	    moveButton.setDisable(false)
	    textChanged = false
	    readyToSave = false
	    println("Loaded file successfully.")
	}
    }


    // Scheduled tasks
    fun autoUpdate() {
	if (activeFileList === currentFileList) {
	    when {
		textChanged -> {
		    if (readyToSave) {
			val selectionModel = listView.getSelectionModel()
			val selectedItem = selectionModel.getSelectedItem()

			runCatching {
			    saveFile(selectedItem)
			}.onFailure {
			    return // do nothing if failed
			}.onSuccess {
			    println("Saved automatically.")
			    textChanged = false
			}
		    }
		}
		orderChanged -> Platform.runLater {
		    FXCollections.sort(currentFileList, FileInfoModel.firstNewest)
		    orderChanged = false
		}
	    }

	    readyToSave = textChanged
	}
    }
}

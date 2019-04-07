/**
 * memo/Memo.kt - Main application class
 *
 * Copyright (c) 2019 Yasuhiro Yamakawa <kawatab@yahoo.co.jp>
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 */

package io.github.kawatab.memo

import java.nio.file.Path
import java.util.Date
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import javafx.concurrent.WorkerStateEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Duration


class Memo : Application () {
    private val io = FileIO()
    private val autoUpdateService = AutoUpdateService()
    var currentFile: Path? = null

    override fun start(stage: Stage) {
	if (io.isAvailable()) {
	    stage.title = "memo"

	    val fxmlLoader = FXMLLoader(javaClass.getResource("/mainwindow.fxml")) 
	    val controller = WindowController(this)
	    fxmlLoader.setController(controller);
	    val parent = fxmlLoader.load<Parent>() 
	    val scene = Scene(parent)
	    stage.scene = scene
	    stage.show()

	    setAutoUpdateService(controller)
	} else {
	    Platform.exit()
	}
    }
    
    fun readCurrentFile(): String {
	return io.readFile(currentFile).joinToString(separator = "\n")
    }
    
    fun getActiveFileList(): List<FileInfoModel> {
	return io.getActiveFileList().map { path -> createFileInfo(path) }
    }

    fun getArchiveFileList(): List<FileInfoModel> {
	return io.getArchiveFileList().map { path -> createFileInfo(path) }
    }

    fun createFile(): Path? {
	val fileName = Date().getTime().toString() + ".txt"
	return io.createFile(fileName)
    }

    fun writeFile(path: Path, lines: List<String>) {
	io.writeFile(path, lines)
    }
    
    fun deleteCurrentFile() {
	io.deleteFile(currentFile)
    }
    
    fun deleteFile(path: Path) {
	io.deleteFile(path)
    }
    
    fun moveCurrentFile(fileInfo: FileInfoModel) {
	io.moveFile(currentFile)?.let { fileInfo.path = it }
    }
    
    fun createFileInfo(path: Path): FileInfoModel {
	val preview = io.getPreviewOfContents(path)
	val date = io.getLastModifiedDate(path)
	return FileInfoModel(preview, date, path)
    }
    
    fun updateFileInfo(fileInfo: FileInfoModel) {
	if (fileInfo.path == currentFile) {
	    fileInfo.setPreview(io.getPreviewOfContents(currentFile as Path))
	    fileInfo.setDate(io.getLastModifiedDate(currentFile as Path))
	}
    }
    
    private fun setAutoUpdateService(controller: WindowController) {
	autoUpdateService.apply {
	    setPeriod(Duration.seconds(1.0))
	    setOnSucceeded { controller.autoUpdate() }
	    start()
	}
    }

    private inner class AutoUpdateService : ScheduledService<Boolean>() {
	override protected fun createTask(): Task<Boolean> {
	    return object: Task<Boolean>() {
		override protected fun call(): Boolean {
		    return true
		}
	    }
	}
    }
}

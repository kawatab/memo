/**
 * memo/FileInfoModel.kt - file info model for list view
 *
 * Copyright (c) 2019 Yasuhiro Yamakawa <kawatab@yahoo.co.jp>
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 */

package io.github.kawatab.memo

import java.nio.file.Path
import java.util.Comparator
import javafx.beans.property.SimpleStringProperty

class FileInfoModel(preview: String, date: String, path: Path) {
    val previewProperty = SimpleStringProperty(this, "preview")
    val dateProperty = SimpleStringProperty(this, "date")
    var path: Path = path

    init {
	setPreview(preview)
	setDate(date)
    }
    
    fun setPreview(preview: String) {
	previewProperty.set(preview)
    }

    fun getPreview(): String {
	return previewProperty.get()
    }
    
    fun setDate(date: String) {
	dateProperty.set(date)
    }

    fun getDate(): String {
	return dateProperty.get()
    }

    companion object {
	val firstNewest = Comparator<FileInfoModel> { left, right -> right.getDate().compareTo(left.getDate()) }
    }
}

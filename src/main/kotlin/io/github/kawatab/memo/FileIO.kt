/**
 * memo/FileIO.kt - class for handling files
 *
 * Copyright (c) 2019 Yasuhiro Yamakawa <kawatab@yahoo.co.jp>
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 */

package io.github.kawatab.memo

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.streams.asSequence


class FileIO() {
    private val workDirectory: Path?
    private val archiveDirectory: Path?

    init {
	workDirectory = runCatching {
	    val home = Paths.get(System.getProperty("user.home"))
	    val path = home.resolve(DEFAULT_DIRECTORY)
	    
	    if (!Files.exists(path)) {
		println("Create work directory.: FileIO.kt::init")
		Files.createDirectory(path)
	    }

	    path
	}.onFailure() {
	    println("Tried to create work directory, but failed.: FileIO.kt::init")
	}.run {
	    getOrNull()
	}

	archiveDirectory = runCatching {
	    if (workDirectory == null) {
		null 
	    } else {
		val path = workDirectory.resolve(ARCHIVE_DIRECTORY)
		
		if (!Files.exists(path)) {
		    println("Created archive directory.: FileIO.kt::init")
		    Files.createDirectory(path)
		}
		
		path
	    }
	}.onFailure() {
	    println("Tried to create archive directory, but failed.: FileIO.kt::init")
	}.run {
	    getOrNull()
	}
    }

    fun isAvailable(): Boolean {
	return workDirectory != null && archiveDirectory != null
    }
    
    @Synchronized fun createFile(fileName: String): Path? {
	return if (workDirectory != null) {
	    workDirectory.resolve(fileName).also { Files.createFile(it) }
	} else {
	    println("Cannot find \".memo\": FileIO.kt::createFile")
	    null
	}
    }

    @Synchronized fun getActiveFileList(): List<Path> {
	return Files.list(workDirectory).asSequence().toList().filter { !Files.isDirectory(it) }
    }

    @Synchronized fun getArchiveFileList(): List<Path> {
	return Files.list(archiveDirectory).asSequence().toList().filter { !Files.isDirectory(it) }
    }

    fun readFile(file: Path?): List<String> {
	return Files.readAllLines(file, Charset.forName("UTF-8"))
    }

    @Synchronized fun writeFile(path: Path, lines: List<String>) {
	if (Files.exists(path)) {
	    Files.write(path, lines, Charset.forName("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING)
	} else {
	    Files.createFile(path)
	}
    }

    @Synchronized fun deleteFile(file: Path?) {
	Files.delete(file)
    }

    @Synchronized fun moveFile(file: Path?): Path? {
	return file?.let {
	    val newPath = (if (workDirectory == it.getParent()) archiveDirectory else workDirectory)?.resolve(it.getFileName())

	    if (newPath == null) {
		println("Cannot move file: FileIO.kt::moveFile")
	    } else {
		Files.move(it, newPath, StandardCopyOption.REPLACE_EXISTING)
	    }
	    
	    newPath
	}
    }
    
    fun getLastModifiedDate(path: Path): String {
	return runCatching {
	    val dateFormat = SimpleDateFormat(TIMESTAMP_PATTERN)
	    val lastUpdate = Files.getLastModifiedTime(path)
	    dateFormat.format(Date(lastUpdate.toMillis()))
	}.onFailure {
	    println("Cannot read file modified date: FileIO.kt::getLastModifiedDate")
	}.run {
	    getOrDefault("")
	}
    }

    fun getPreviewOfContents(path: Path): String {
	return runCatching {
	    if (Files.exists(path)) {
		val lines = Files.readAllLines(path, Charset.forName("UTF-8"))
		lines.joinToString(limit = 128, separator = " ").trim()
	    } else {
		""
	    }
	}.onFailure {
	    println("Cannot read file: FileIO.kt::getPreviewOfContents")
	}.run {
	    getOrDefault("")
	}
    }

    companion object {
	val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"
	val DEFAULT_DIRECTORY = ".memo"
	val ARCHIVE_DIRECTORY = "archive"
    }
}

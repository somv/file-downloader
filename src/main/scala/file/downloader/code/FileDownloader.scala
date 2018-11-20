package file.downloader.code

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import com.typesafe.config.ConfigFactory
import org.apache.commons.vfs2.{FileObject, FileSystemManager, VFS}

object FileDownloader extends Downloader {

  var READ_TIMEOUT = 10000

  var LOCATION = ""

  val fileSystemManager: FileSystemManager = VFS.getManager

  def main(args: Array[String]): Unit = {

    val conf = ConfigFactory.load

    val configuredLocation = try {
      conf.getString("location")
    } catch {
      case exception: Exception => ""
    }

    LOCATION = getParentDirPath(configuredLocation)
    println()

    READ_TIMEOUT = try {
      conf.getInt("readTimeOut")
    } catch {
      case exception: Exception => 10000
    }

    println(s"********** Downloads can be found at ${LOCATION} **********")
    println()

    println("Enter the number of sources that you want to test.")
    val n = scala.io.StdIn.readInt()

    val sources = Array.ofDim[String](n)

    println("Please enter the source links one by one.")
    var i = 0
    while(i<n) {
      sources(i) = scala.io.StdIn.readLine()
      i = i + 1
    }

    println()
    for(source <- sources) {
      download(source)
    }

  }

  /**
    * @param link: Link of the file which has to be downloaded.
    */
  override def download(link: String): Unit = {

    val parentDir = new File(LOCATION)
    println(s"Downloading source: ${link}")
    try {
      download(link.trim, parentDir)
      println()
      println("Download complete.")
      println()
    } catch {
      case exception: Exception =>
        println(s"Exception occurred. Message: ${exception.getMessage}")
        println("Download aborted.")
        println()
    }

  }

  /**
    * @param link: Link of the file to be downloaded.
    * @return File name from the URL (string after the last forward slash),
    *         if it is not present in the URL then will return index.html
    */
  override def fileName(link: String): String = {
    val part = link.substring(link.lastIndexOf('/') + 1)
    if (part.nonEmpty) part else "index.html"
  }

  /**
    * Performs basic checks on the location string,
    * if location string is empty then returns the current user directory.
    * @param location: Dir location where the downloaded file has to be saved.
    * @return Parent directory path based on the output location provided in application.conf file
    */
  override def getParentDirPath(location: String): String = {
    try {
      val locationType = fileSystemManager.resolveFile(location).getType
      if(locationType.equals("imaginary") | locationType.equals("file")) {
        println(s"Bad location for download in the configuration file. downloading in the directory ${System.getProperty("user.dir")}")
        return System.getProperty("user.dir")
      } else {
        location
      }
    } catch {
      case exception: Exception =>
        println(s"Bad location for download in the configuration file. Exception: (${exception.getMessage}). Downloaded file can be found at (${System.getProperty("user.dir")}).")
        System.getProperty("user.dir")
    }
  }

  /**
    *
    * @param fileObject: Object of the file which is being downloaded.
    * @param parentDir: Dir where downloaded file is supposed to be saved.
    * @return Checks if there is enough space available on the file system for the download.
    */
  override protected def isEnoughSpace(fileObject: FileObject, parentDir: File): Boolean = {
    val freeSpace = parentDir.getFreeSpace
    val contentLength = fileObject.getContent.getSize
    if(freeSpace >= contentLength) true else false
  }

  /**
    * Writes input stream to output stream by taking 1024 bytes at a time.
    * It also helps in printing the download percentage on console.
    * @param in: Input stream
    * @param out: Output stream
    *
    */
  override protected def copy(in: InputStream, out: OutputStream, fileObject: FileObject) : Unit = {
    val buf = new Array[Byte](1024)
    val totalBytes = fileObject.getContent.getSize
    val percentageSet: scala.collection.mutable.Set[Int] = scala.collection.mutable.Set()
    var processedBytes = 0
    while (true) {
      val read = in.read(buf)
      processedBytes = processedBytes + read
      if (read == -1) {
        out.flush
        return
      }
      downloadProgress(processedBytes, totalBytes, percentageSet)
      out.write(buf, 0, read)
    }
  }

  /**
    * Utility program to check download progress.
    * Writes download percentages in multiplication of 10 on the console.
    * @param currentIteration: Current iteration being written.
    * @param totalIterations: Total iterations required to write the file to local file system.
    */
  protected def downloadProgress(processedBytes: Long, totalBytes: Long, percentageSet: scala.collection.mutable.Set[Int]): Unit = {
    if(totalBytes == 0) {
      if(percentageSet.isEmpty) {
        percentageSet += 0
        println("This source has returned zero content length. Progress of the download hence can not be calculated, please wait for the download complete message.")
      }
      return
    }
    val processedBytesPercentage = (processedBytes*100/totalBytes).toInt
    if(processedBytesPercentage % 10 == 0 && !percentageSet.contains(processedBytesPercentage)) {
        print(s"${processedBytesPercentage}% ")
        percentageSet += processedBytesPercentage
    }
  }

  /**
    *  If there is size difference between the original file and
    *  the downloaded file then remove the downloaded file.
    * @param fileObject: Object of the file which is being downloaded.
    * @param outputFile: This is the downloaded file that has been written on the local file system.
    */
  override protected def handlePartialDownload(fileObject: FileObject, outputFile: File): Unit = {
    val outputFileSize = outputFile.length()
    val contentLength = fileObject.getContent.getSize
    // Some sources does not return content length, so deleting only files which has some content length.
    if(contentLength!=0 && !outputFileSize.equals(contentLength)) {
      outputFile.delete()
    }
  }

  /**
    * Checks if there is enough space on the local file system otherwise throw exception and abort download.
    * Handles partial download after the file has been downloaded.
    * Closes the File Object.
    * @param link: Link of the file to be downloaded.
    * @param dir: Location, where the downloaded file has to be saved.
    * @return: downloaded file
    */
  override def download(link: String, dir: File): File = {
    val outputFile = new File(dir, fileName(link))
    val fileObject: FileObject = fileSystemManager.resolveFile(link)
    if(isEnoughSpace(fileObject, dir)) {
      try {
        downloadFile(fileObject, outputFile)
      } finally {
        handlePartialDownload(fileObject, outputFile)
        fileObject.close()
      }
    } else {
      throw new NotEnoughSpaceOnDiskException("Not enough space on the disk.")
    }
    outputFile
  }

  /**
    * Copies the input stream to output stream.
    * @param fileObject: Object of the file which is being downloaded.
    * @param outputFile: File written from output stream.
    */
  override def downloadFile(fileObject: FileObject, outputFile: File): Unit = {
    val in: InputStream = fileObject.getContent.getInputStream
    try {
      val out: OutputStream = new FileOutputStream(outputFile)
      try {
        copy(in, out, fileObject)
      }
      finally out.close
    }
    finally in.close
  }

}

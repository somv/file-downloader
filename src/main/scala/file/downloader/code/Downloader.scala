package file.downloader.code

import java.io.{File, InputStream, OutputStream}

import org.apache.commons.vfs2.FileObject

trait Downloader {

  /**
    * @return File name from the URL (string after the last forward slash),
    *         if it is not present in the URL then will return index.html
    */
  def fileName(link: String) : String

  /**
    * @return Extension hook to download the file from the link,
    *         it takes default location set in the config file.
    */
  def download(link: String): Unit

  /**
    * Download file from given link to a given directory.
    * @return downloaded file
    */
  protected def download(link: String, dir: File): File

  /**
    * Called by downloadFile(link: String, dir: File)
    */
  protected def downloadFile(fileObject: FileObject, outputFile: File): Unit

  /**
    * Copies input stream to output stream with buffer array of size 1024
    * output stream writes data to file system
    */
  protected def copy(in: InputStream, out: OutputStream, fileObject: FileObject) : Unit

  /**
    * @return Checks if there is enough space available on the file system for the download
    */
  protected def isEnoughSpace(fileObject: FileObject, file: File): Boolean

  /**
    * Deletes the output file if it is partially downloaded
    */
  protected def handlePartialDownload(fileObject: FileObject, file: File): Unit

  /**
    * @return Parent directory path based on the output location provided in application.conf file
    */
  def getParentDirPath(location: String): String

  /**
    * Writes progress in percentage on the console (e.g 10% 20% up to 100%)
    * @param currentIteration: Current iteration being written
    * @param totalIterations: Total iterations required to write the file to local file system
    */
  protected def downloadProgress(processedBytes: Long, totalBytes: Long, percentageSet: scala.collection.mutable.Set[Int]): Unit

}

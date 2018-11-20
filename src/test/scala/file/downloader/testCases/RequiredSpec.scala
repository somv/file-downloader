package file.downloader.testCases

import java.io.File

import file.downloader.code.{FileDownloader, NotEnoughSpaceOnDiskException, ParentDirNotFoundException}
import org.scalatest.FlatSpec

class RequiredSpec extends FlatSpec {

  it should "throw DownloadInterruptedException | The directory location does not exists on the file system." in {
    val link = "ftp://speedtest.tele2.net/5MB.zip"
    val dir = new File("")
    assertThrows[ParentDirNotFoundException] {
      FileDownloader.download(link, dir)
    }
  }

  it should "throw DownloadInterruptedException | Not enough space on the disk." in {
    val link = "ftp://speedtest.tele2.net/1000GB.zip"
    val dir = new File("/Users/b0205051/Desktop")
    assertThrows[NotEnoughSpaceOnDiskException] {
      FileDownloader.download(link, dir)
    }
  }

  it should "get file name from the url" in {
    val link = "ftp://speedtest.tele2.net/5MB.zip"
    assertResult("5MB.zip") {
      FileDownloader.fileName(link)
    }
  }

  it should "file name should be index.html" in {
    val link = "https://www.wikipedia.org/"
    assertResult("index.html") {
      FileDownloader.fileName(link)
    }
  }

}

package file.downloader.testCases

import file.downloader.code.{FileDownloader}
import org.scalatest.FlatSpec

class RequiredSpec extends FlatSpec {

  it should "get file name from the url" in {
    val link = "ftp://speedtest.tele2.net/5MB.zip"
    assertResult("5MB.zip") {
      FileDownloader.fileName(link)
    }
  }

  it should "return file name as index.html" in {
    val link = "https://www.wikipedia.org/"
    assertResult("index.html") {
      FileDownloader.fileName(link)
    }
  }

  it should "throw exception for invalid directory location abc/def" in {
    val currentDir = System.getProperty("user.dir")
    assertResult(currentDir) {
      FileDownloader.getParentDirPath("abc/def")
    }
  }

}

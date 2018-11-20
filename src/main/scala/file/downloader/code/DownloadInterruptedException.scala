package file.downloader.code

final case class ParentDirNotFoundException(private val message: String, private val cause: Throwable = None.orNull) extends Exception(message)

final case class NotEnoughSpaceOnDiskException(private val message: String, private val cause: Throwable = None.orNull) extends Exception(message)

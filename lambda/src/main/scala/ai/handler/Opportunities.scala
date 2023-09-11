package ai.handler

import java.util.Base64
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import sttp.client4.*
import cats.data.EitherT
import cats.effect.IO
import ujson.Value.Value
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

object Opportunities {

  val backend = DefaultFutureBackend()

  def fetchData(): EitherT[IO, String, String] = {
    val request = basicRequest
      .get(uri"https://api.yoma.world/api/v1/opportunities")
      .header("accept:", "application/json")
      .response(asStringAlways)

    val result = for {
      response        <- EitherT.liftF(IO.fromFuture(IO(request.send(backend))))
      parsed          <- EitherT.fromEither(
                           Try(ujson.read(response.body)).toEither.left
                             .map(e => e.getMessage)
                         )
      data            <- EitherT.fromEither(
                           Try(parsed("data").str).toEither.left
                             .map(e => e.getMessage)
                             .flatMap(s =>
                               Either
                                 .cond(
                                   s.nonEmpty,
                                   s,
                                   s"Failure in fetching opportunities data: ${response.body}"
                                 )
                             )
                         )
      decodedData      = decodeBase64(data)
      decompressedData = decompressGzip(decodedData)
    } yield decompressedData

    result
  }

  def decodeBase64(encoded: String): Array[Byte] = {
    Base64.getDecoder.decode(encoded)
  }

  def decompressGzip(compressed: Array[Byte]): String = {
    val inputStream  = new GZIPInputStream(new ByteArrayInputStream(compressed))
    val outputStream = new ByteArrayOutputStream()

    val buffer = new Array[Byte](1024)
    var length = 0
    while ({ length = inputStream.read(buffer); length } > 0) {
      outputStream.write(buffer, 0, length)
    }

    new String(outputStream.toByteArray, "UTF-8")
  }
}

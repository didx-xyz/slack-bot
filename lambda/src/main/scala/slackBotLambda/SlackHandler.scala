package slackBotLambda

import cats.data.EitherT
import io.circe.generic.auto._
import io.circe.parser._
import sttp.client4.*
import ujson.Value.Value

import java.net.URLDecoder
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps
import io.circe.Json

object SlackHandler {

  case class Input(body: String)
  case class Output(body: String)

  val backend = DefaultFutureBackend()

  val verifyUrl: String = "url_verification"

  def run(event: Input): IO[Output] = {
    scribe.info(s"Received call: ${event.body}")

  /*Verify ownership of the Events API subscription URL (event_subscriptions.request_url in app manifest)*/
  private def handleChallengeRequest(event: Input): IO[Output] = {
    scribe.info(s"Handling challenge request from event: ${event.body}")
    val response: EitherT[IO, Output, Output] = for {
      challenge <- readChallenge(event.body)
    } yield {
      Output(challenge)
    }
    response.merge
  }
    val response: EitherT[IO, Output, Output] = for {
      userId   <- getUserId(event.body)
      botToken <- getBotToken
      response <- fetchUserInfo(userId, botToken)
      _         = scribe.info(s"Slack response: ${response}")
    } yield {
      val realName = response("user")("profile")("real_name").str
      Output(s"Hello $realName!")
    }
    response.merge
  }

  private def handleFileCommand(event: Input): IO[Output] = {
    scribe.info(s"Handling file command from event: ${event.body}")
    val response: EitherT[IO, Output, Output] = for {
      channelId <- getChannelId(event.body)
      botToken  <- getBotToken
      response  <- fetchFileList(channelId, botToken)
      _          = scribe.info(s"Slack response: ${response}")
    } yield {
      val listOfFiles = response("files").arr
      val file        = listOfFiles.last
      val fileId      = file("id").str
      val fileLink    = file("permalink").str
      Output(s"File ID: $fileId, File Public Link: $fileLink")
    }
    response.merge
  }
  private def getBotToken: EitherT[IO, Output, String] = {
    EitherT.fromOptionF(
      cats.effect.std.Env[IO].get("SLACK_BOT_TOKEN"),
      Output("Missing slack token")
    )
  }

  private def readChallenge(requestBody: String): EitherT[IO, Output, String] = {
    val parsed = parseJson(requestBody)
    EitherT.fromOption(
      parsed.findAllByKey("challenge").head.asString,
      Output("Couldn't read challenge")
    )
  }

  private def getUserId(requestBody: String): EitherT[IO, Output, String] = {
    val parsed = parseCommandUrlParams(requestBody)
    EitherT.fromOption(parsed.get("user_id"), Output("Couldn't get UserId"))
  }
  }

  private def fetchUserInfo(userId: String, token: String): EitherT[IO, Output, ujson.Value] = {
    val request = basicRequest
      .get(uri"https://slack.com/api/users.info?&user=${userId}")
      .auth
      .bearer(token)
      .response(asStringAlways)
    for {
      response <- EitherT.liftF(IO.fromFuture(IO(request.send(backend))))
      parsed   <- EitherT.fromEither(
                    Try(ujson.read(response.body)).toEither.left
                      .map(e => Output(e.getMessage))
                  )
      _        <- EitherT.fromEither(
                    Try(parsed("ok").bool).toEither.left
                      .map(e => Output(e.getMessage))
                      .flatMap(isOk =>
                        Either
                          .cond(isOk, (), Output(s"Failure in communicating with slack: ${response.body}"))
                      )
                  )
    } yield ujson.read(response.body)
  }

  def parseCommandUrlParams(body: String): Map[String, String] = {
    body
      .split("&")
      .collect { case s"$key=$value" => key -> URLDecoder.decode(value, "UTF-8") }
      .toMap
  }

  def parseJson(body: String): Json = {
    val parsed = parse(body)

    parsed match {
      case Left(failure) => {
        scribe.error(s"Failed to parse json from body: $body")
        Json.Null
      }
      case Right(json)   =>
        json
    }
  }

}

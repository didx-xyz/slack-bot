package local

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Queue
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.Http
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.ErrorAction
import org.http4s.server.middleware.ErrorHandling
import org.http4s.server.middleware.Logger
import slackBotLambda.SlackHandler

object MyHttpServer extends IOApp.Simple {
  val run = runServer

  def runServer: IO[Nothing] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"9876")
      .withHttpApp(
        Logger.httpApp(logHeaders = true, logBody = true)(errorLogging(routes.orNotFound))
      )
      .build
      .void
      .useForever
  }

  private def routes: HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes.of[IO] {
      case GET -> Root / "hello"           => Ok("world")
      case req @ POST -> Root / "commands" =>
        for {
          input  <- requestToLambdaInput(req)
          result <- SlackHandler.run(input)
        } yield lambdaOutputToResponse(result)
      case req @ POST -> Root / "events"   =>
        for {
          input  <- requestToLambdaInput(req)
          result <- input match {
                      case _ if input.body.contains("url_verification") => {
                        val challenge = SlackHandler.handleChallengeRequest(input)
                        challenge.map(_.body)
                      }
                      case _                                            => {
                        for {
                          _ <- SlackHandler.handleMessage(input).start
                        } yield ""
                      }
                    }

        } yield Response[IO](Status.Ok).withEntity(result) // immediately respond with 200
    }
  }

  private def requestToLambdaInput(req: Request[IO]): IO[SlackHandler.Input] =
    req.as[String].map(SlackHandler.Input.apply)

  private def lambdaOutputToResponse(resp: SlackHandler.Output): Response[IO] = {
    Response().withEntity(resp.body)
  }

  private def errorLogging(route: Http[IO, IO]): Http[IO, IO] = ErrorHandling.Recover.total(
    ErrorAction.log(
      route,
      messageFailureLogAction = (t, msg) => IO(scribe.error(msg, t)),
      serviceErrorLogAction = (t, msg) => IO(scribe.error(msg, t))
    )
  )

}

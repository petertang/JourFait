package controllers

import models.Account
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsPath
import play.api.libs.json.Json._
import play.api.libs.json.Reads
import play.api.libs.json.Reads._
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.functorReads
import play.api.mvc.{ Action, Controller, Result }
import models.Tables._
import play.api.db.slick.DBAction
import play.api.db.slick._
import play.api.libs.json.JsError
import org.apache.commons.mail.Email
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import org.joda.time.DateTime
import java.net.URLEncoder
import play.api.i18n.Messages
import play.api.mvc.Cookie
import play.api.Play.current
import play.api.Configuration

object AccountController extends Controller {

  implicit val accountRead: Reads[(Account, String)] = (
    (JsPath \ "username").read[String] and
    (JsPath \ "firstName").read[String] and
    (JsPath \ "lastName").read[String] and
    (JsPath \ "email").read[String](email) and
    (JsPath \ "password").read[String])(accountJsonToAccountWithPassword _)

  def accountJsonToAccountWithPassword(username: String, firstName: String, lastName: String, email: String, password: String) = {
    new Tuple2(Account(username, firstName, lastName, email, false, None), password)
  }

  def sendEmail(account: Account)(implicit dbSession: Session) = {
    val token = Accounts.createToken(account)
    val email: Email = new SimpleEmail();
    val config: Configuration = current.configuration
    email.setHostName(config.getString("email.host.name").getOrElse(""))
    email.setSmtpPort(config.getInt("email.port.number").getOrElse(465));
    email.setAuthenticator(new DefaultAuthenticator(config.getString("email.authenticator.user").get, config.getString("email.authenticator.password").get));
    email.setSSLOnConnect(true);
    email.setFrom(config.getString("email.from.email").getOrElse("jourfait@jourfait.com"));
    email.setSubject(Messages("verification.email.subject"));
    email.setMsg(Messages("verification.email.body",
      "http://localhost:9000/verifyAccount?" +
        "account=" + URLEncoder.encode(account.username, "UTF-8") +
        "&ver_token=" + URLEncoder.encode(token, "UTF-8")))
    if (config.getBoolean("email.override.enabled").getOrElse(true))
      // TODO: handle error with no override address
      email.addTo(config.getString("email.override.address").orNull)
    else
      email.addTo(account.email)
    email.send();
  }

  trait WithLoggedInSession extends Result {
    
  }
  
  def createAccount = DBAction(parse.json) {
    implicit rs =>
      rs.dbSession.withTransaction {
        val account = rs.body
        account.validate[(Account, String)].fold(
          valid = {
            case (account, password) =>
              val newAccount = Accounts.createAccount(account, password)
              sendEmail(account)
              Ok.withSession(rs.request.session + ("username" -> account.username))
          },
          invalid = {
            errors => BadRequest(JsError.toFlatJson(errors))
          })
      }
  }

  def login = DBAction(parse.json) {
    implicit rs =>
      val credentials = rs.body
      val username = (credentials \ "username").as[String]
      val password = (credentials \ "password").as[String]
      Accounts.login(username, password).fold[Result](
        BadRequest)(
          user => {
            val sessionMap: Map[String, String] = Map(("username", user.username))
            Ok.withSession(rs.request.session + ("username" -> user.username)).
              withCookies(Cookie("verified", user.verified.toString, httpOnly = false))
          })
  }

  def logout = Action {
    request => Redirect(routes.Application.index).withNewSession
  }

  def verify(username: String, token: String) = DBAction {
    implicit rs =>
      if (Accounts.verifyAccount(username, token))
        Redirect(routes.TasksController.list)
      else
        BadRequest
  }

  def resendVerifyEmail = DBAction {
    implicit rs =>
      rs.request.session.get("username").fold(BadRequest)(
        username => {
          val account: Option[Account] = Accounts.findByUsername(username)
          sendEmail(account.get)
          Ok
        })
  }
}
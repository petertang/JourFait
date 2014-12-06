package controllers.helpers

import play.mvc.Security.Authenticated
import controllers.routes
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.mvc.Result
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Security

trait Secured {
  def username(request: RequestHeader) = request.session.get("username")
  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.index)
  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }
}

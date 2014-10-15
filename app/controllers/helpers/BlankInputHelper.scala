package controllers.helpers

object BlankInputHelper {
  import views.html.helper.FieldConstructor
  implicit val myFields = FieldConstructor(views.html.helper.blankFieldConstructorTemplate.f)
}
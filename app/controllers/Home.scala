/**
 *
 * to do sample project
 *
 */

package controllers

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure


import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._

import model.ViewValueHome
import model.TodoWithCategory

import lib.persistence.onMySQL._
import lib.model.Todo
import lib.model.TodoCategory

import play.api.data._
import play.api.data.Forms._

import forms.AddTodoForm.addTodoForm

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {

  val errorTodoCategory = TodoCategory.apply(name="ERROR", slug="ERROR", color=TodoCategory.Color.RED)

  def index() = Action { implicit req =>
    val vv = ViewValueHome(
      title  = "Home",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )
    Ok(views.html.Home(vv))
  }

  def getTodoList() = Action.async { implicit req => 
    val getAllTodoFuture = TodoRepository.getAll()
    val getAllTodoCategoryFuture = TodoCategoryRepository.getAll()

    val getAllTodoWithCategoryFuture: Future[Seq[TodoWithCategory]] = for {
        todoSeq     <- getAllTodoFuture
        categorySeq <- getAllTodoCategoryFuture
    } yield {
      todoSeq.map(todo => {
          val category = categorySeq.find(
            _.id == todo.v.categoryId
          ).getOrElse(
            errorTodoCategory
          )
          TodoWithCategory(
            todo = todo.v, 
            category = category.v
          )
        }
      )
    }

    val vv = ViewValueHome(
      title = "Todo一覧", 
      cssSrc = Seq("main.css"), 
      jsSrc  = Seq("main.js"), 
    )

    for {
      todoWithCategorySeq <- getAllTodoWithCategoryFuture
      categorySeq         <- getAllTodoCategoryFuture
    } yield {
      Ok(views.html.Todo(vv, todoWithCategorySeq, categorySeq, addTodoForm))
    }
  }

  def addTodo() = Action(parse.form(addTodoForm)).async { implicit req => 
    val todoData = req.body
    val todoWithNoId: Todo#WithNoId = Todo.apply(
      categoryId = TodoCategory.Id(todoData.categoryId), 
      title      = todoData.title, 
      body       = todoData.body, 
      state      = lib.model.Todo.Status.TODO
    )
    val addTodoFuture = TodoRepository.add(todoWithNoId)
    addTodoFuture.map(id => 
      Redirect("/todo")  
    )
  }
}


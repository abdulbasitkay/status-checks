package com.newmotion.devops.status.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.newmotion.devops.status.{Result, Results}
import com.newmotion.devops.status.StatusCheck.Checks
import com.newmotion.devops.status.sprayjson.StatusJsonProtocol
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.{Future, TimeoutException}

class StatusRouteSpec extends WordSpec with Matchers with ScalatestRouteTest with StatusJsonProtocol {

  "The status routes" should {
    "return OK for the /heartbeat" in  {
      val emptyCheck = Checks("empty-check")

      Get("/heartbeat") ~> StatusRoutes.statusRoute(emptyCheck) ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }

    "return a Result for a single check" in {
      val singleCheck = Checks("single_check").internal("me", (n:String) => Future(Result(n,true,Map())))

      Get("/status") ~> StatusRoutes.statusRoute(singleCheck) ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[Results] shouldEqual Results("single_check", Seq(Result("me", true, Map())), Seq())
      }
    }

    "return a Result with a false status when an exception occurred" in {
      val exceptionCheck = Checks("exception_check").internal("me", (n:String) => Future(throw new TimeoutException("bla")))

      Get("/status") ~> StatusRoutes.statusRoute(exceptionCheck) ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[Results] shouldEqual Results("exception_check", Seq(Result("me", false, Map("exception" -> "bla"))), Seq())
      }
    }

    "return a Result when there are two checks" in {
      val twoChecks = Checks("two_checks")
                        .internal("one", (n:String) => Future(Result(n,true,Map())))
                        .external("two", (n:String) => Future(Result(n,true,Map("response_time"->"0.040"))))

      Get("/status") ~> StatusRoutes.statusRoute(twoChecks) ~> check {
        println(responseAs[String])
        response.status shouldEqual StatusCodes.OK
        responseAs[Results] shouldEqual Results(
                                          "two_checks",
                                          Seq(Result("one", true, Map())),
                                          Seq(Result("two", true, Map("response_time"->"0.040")))
                                        )
      }
    }
  }
}

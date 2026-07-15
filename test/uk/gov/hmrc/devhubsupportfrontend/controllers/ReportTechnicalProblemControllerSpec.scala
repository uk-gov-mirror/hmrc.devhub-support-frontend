/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.devhubsupportfrontend.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.utils.WithSupportSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{ReportTechnicalProblemConfirmationView, ReportTechnicalProblemView, SupportPageConfirmationForHoneyPotFieldView}

class ReportTechnicalProblemControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val reportTechnicalProblemView                  = app.injector.instanceOf[ReportTechnicalProblemView]
    val reportTechnicalProblemConfirmationView      = app.injector.instanceOf[ReportTechnicalProblemConfirmationView]
    val supportPageConfirmationForHoneyPotFieldView = app.injector.instanceOf[SupportPageConfirmationForHoneyPotFieldView]

    val underTest = new ReportTechnicalProblemController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      SupportServiceMock.aMock,
      reportTechnicalProblemView,
      reportTechnicalProblemConfirmationView,
      supportPageConfirmationForHoneyPotFieldView
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)
    val supportSessionId                     = SupportSessionId.random

    val fullName: String              = "Peter Smith"
    val emailAddress: String          = "peter@example.com"
    val whatWereYouDoing: String      = "I was trying to check the status of my application"
    val whatDoYouNeedHelpWith: String = "Help me SDST, you're my only hope"
    val service: String               = "third-party-developer"
    val referrer: String              = "referrer"
    val honeypotUrl                   = "It's a trap"
  }

  trait IsLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()
  }

  trait NotLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.fails()
  }

  trait IsPartLoggedInEnablingMFA {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeedsPartLoggedInEnablingMfa()
  }

  "ReportTechnicalProblemController" when {
    "invoke page" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest)(supportSessionId)
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.page(Some("third-party-developer"), Some("referrerUrl")))(requestWithSupportCookie)

        status(result) shouldBe OK
        contentAsString(result) should include("Get help with a technical problem")
      }

      "succeed when session does not exist" in new Setup {
        val request = FakeRequest()

        val result = addToken(underTest.page(None, None))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("Get help with a technical problem")
      }
    }

    "invoke action" should {
      "submit new request with name, email, comments" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"              -> fullName,
            "emailAddress"          -> emailAddress,
            "whatWereYouDoing"      -> whatWereYouDoing,
            "whatDoYouNeedHelpWith" -> whatDoYouNeedHelpWith,
            "referrer"              -> referrer,
            "service"               -> service
          )
        SupportServiceMock.ReportTechnicalProblem.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/report-technical-problem-confirm/ticket-ref")

        SupportServiceMock.ReportTechnicalProblem.verifyCalledWith(
          fullName,
          emailAddress,
          whatWereYouDoing,
          whatDoYouNeedHelpWith,
          Some(service),
          Some(referrer),
          None,
          Some(sessionId.toString())
        )
      }

      "submit new request with no name, email or details" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"              -> "",
            "emailAddress"          -> "",
            "whatWereYouDoing"      -> "",
            "whatDoYouNeedHelpWith" -> ""
          )
        SupportServiceMock.ReportTechnicalProblem.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("Enter your full name")
        contentAsString(result) should include("Enter your email address")
        contentAsString(result) should include("Enter details of what you were doing")
        contentAsString(result) should include("Enter details of what went wrong")
      }

      "submit new request with invalid name and email" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"              -> "12345678901234567890123456789012345678901234567890123456789012345678901",
            "emailAddress"          -> "invalidemailaddress",
            "whatWereYouDoing"      -> whatWereYouDoing,
            "whatDoYouNeedHelpWith" -> whatDoYouNeedHelpWith
          )
        SupportServiceMock.ReportTechnicalProblem.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("Full name cannot be longer than 70 characters")
        contentAsString(result) should include("Enter an email address in the correct format, like name@example.com")
      }

      "submit new request when honeypot field filled in but logged in" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"              -> fullName,
            "emailAddress"          -> emailAddress,
            "whatWereYouDoing"      -> whatWereYouDoing,
            "whatDoYouNeedHelpWith" -> whatDoYouNeedHelpWith,
            "referrer"              -> referrer,
            "service"               -> service,
            "url"                   -> honeypotUrl
          )
        SupportServiceMock.ReportTechnicalProblem.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/report-technical-problem-confirm/ticket-ref")

        SupportServiceMock.ReportTechnicalProblem.verifyCalledWith(
          fullName,
          emailAddress,
          whatWereYouDoing,
          whatDoYouNeedHelpWith,
          Some(service),
          Some(referrer),
          None,
          Some(sessionId.toString())
        )
      }

      "show dummy confirmation page when honeypot field filled in but not logged in" in new Setup with NotLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"              -> fullName,
            "emailAddress"          -> emailAddress,
            "whatWereYouDoing"      -> whatWereYouDoing,
            "whatDoYouNeedHelpWith" -> whatDoYouNeedHelpWith,
            "referrer"              -> referrer,
            "service"               -> service,
            "url"                   -> honeypotUrl
          )

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("We've sent you a confirmation email")
        contentAsString(result) should include("Your request will be sent to the right team as quickly as possible")
        contentAsString(result) should include("We'll send an email to your email address whenever there's an update")

        verifyZeroInteractions(SupportServiceMock.aMock)
      }
    }

    "invoke confirmation page" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest)(supportSessionId)
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.confirmationPage("ticket-ref"))(requestWithSupportCookie)

        status(result) shouldBe OK
        contentAsString(result) should include("Your support request has been submitted")
        contentAsString(result) should include("ticket-ref")
      }
    }
  }
}

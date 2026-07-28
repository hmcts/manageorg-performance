package simulations

import com.typesafe.config.{Config, ConfigFactory}
import io.gatling.core.Predef.{exec, _}
import io.gatling.http.Predef._
import scenarios._
import utils._
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.pause.PauseType
import scala.concurrent.duration._
import scala.io.Source

class ManageOrgSimulation extends Simulation{

  val config: Config = ConfigFactory.load()
	/* TEST TYPE DEFINITION */
	/* pipeline = nightly pipeline against the AAT environment (see the Jenkins_nightly file) */
	/* perftest (default) = performance test against the perftest environment */
	val testType = scala.util.Properties.envOrElse("TEST_TYPE", "perftest")

	//set the environment based on the test type
	val environment = testType match{
		case "perftest" => "perftest"
		case "pipeline" => "perftest"
		case _ => "**INVALID**"
	}

	/* ******************************** */
	/* ADDITIONAL COMMAND LINE ARGUMENT OPTIONS */
	val debugMode = System.getProperty("debug", "off") //runs a single user e.g. ./gradlew gatlingRun -Ddebug=on (default: off)
	val env = System.getProperty("env", environment) //manually override the environment aat|perftest e.g. ./gradlew gatlingRun -Denv=aat
	/* ******************************** */

	/* PERFORMANCE TEST CONFIGURATION */
	val approveOrgTargetPerHour:Double = 360 //360
  val approveOtherOrgTargetPerHour:Double = 20

	val rampUpDurationMins = 5 //5
	val rampDownDurationMins = 5 //5 
	val testDurationMins = 60 //60

	val numberOfPipelineUsers = 1
	val pipelinePausesMillis:Long = 3000 //3 seconds

	//Determine the pause pattern to use:
	//Performance test = use the pauses defined in the scripts
	//Pipeline = override pauses in the script with a fixed value (pipelinePauseMillis)
	//Debug mode = disable all pauses
	val pauseOption:PauseType = debugMode match{
		case "off" if testType == "perftest" => constantPauses
		case "off" if testType == "pipeline" => customPauses(pipelinePausesMillis)
		case _ => disabledPauses
	}

  val httpProtocol = http
		.baseUrl(Environment.BaseUrl.replace("#{env}", s"${env}"))
		.inferHtmlResources()
		.silentResources
    .disableCaching

	before{
		println(s"Test Type: ${testType}")
		println(s"Test Environment: ${env}")
		println(s"Debug Mode: ${debugMode}")
	}

  val ManageAndApproveOrg = scenario("Create a new org with Multiple PBAs and Approve")
		.exitBlockOnFail {
      exec(_.set("env", s"${env}"))
        .exec(
          CreateOrg.CreateNewOrg,
          CreateOrg.SubmitOrg,
          ApproveOrg.ApproveOrgHomepage,
          ApproveOrg.ApproveOrgLogin,
          ApproveOrg.SearchOrg,
          ApproveOrg.ViewOrg,
          ApproveOrg.AddNewPBA,
          ApproveOrg.ApproveNewOrg,
          ApproveOrg.Logout
        )
    }

  val ManageAndApproveOtherOrg = scenario("Create a new Other org and Approve")
    .exitBlockOnFail {
      exec(_.set("env", s"${env}"))
        .exec(
          CreateOrg.CreateNewOrg,
          CreateOrg.SubmitOtherOrg,
          ApproveOrg.ApproveOrgHomepage,
          ApproveOrg.ApproveOrgLogin,
          ApproveOrg.SearchOrg,
          ApproveOrg.ViewOrg,
          ApproveOrg.ApproveNewOtherOrg,
          ApproveOrg.Logout
        )
    }

	/*===============================================================================================
	* Simulation Configuration
	 ===============================================================================================*/

	def simulationProfile(simulationType: String, userPerHourRate: Double, numberOfPipelineUsers: Double): Seq[OpenInjectionStep] = {
		val userPerSecRate = userPerHourRate / 3600
		simulationType match {
			case "perftest" =>
				if (debugMode == "off") {
					Seq(
						rampUsersPerSec(0.00) to (userPerSecRate) during (rampUpDurationMins minutes),
						constantUsersPerSec(userPerSecRate) during (testDurationMins minutes),
						rampUsersPerSec(userPerSecRate) to (0.00) during (rampDownDurationMins minutes)
					)
				}
				else{
					Seq(atOnceUsers(1))
				}
			case "pipeline" =>
				Seq(rampUsers(numberOfPipelineUsers.toInt) during (2 minutes))
			case _ =>
				Seq(nothingFor(0))
		}
	}

  //defines the test assertions, based on the test type
  def assertions(simulationType: String): Seq[Assertion] = {
    simulationType match {
      case "perftest" =>
        if (debugMode == "off") {
          Seq(global.successfulRequests.percent.gte(95),
            details("CreateOrg_020_SubmitNewOrgRegistration").successfulRequests.count.gte((approveOrgTargetPerHour * 0.9).ceil.toInt),
            details("CreateOrg_020_SubmitOtherOrgRegistration").successfulRequests.count.gte((approveOrgTargetPerHour * 0.9).ceil.toInt),
            details("AdminOrg_070_AddPBA").successfulRequests.count.gte((approveOrgTargetPerHour * 0.9).ceil.toInt),
            details("AdminOrg_080_ApproveOrg").successfulRequests.count.gte((approveOrgTargetPerHour * 0.9).ceil.toInt)
          )
        }
        else{
          Seq(global.successfulRequests.percent.gte(95),
            details("CreateOrg_020_SubmitNewOrgRegistration").successfulRequests.count.is(1),
            details("CreateOrg_020_SubmitOtherOrgRegistration").successfulRequests.count.is(1),
            details("AdminOrg_070_AddPBA").successfulRequests.count.is(1),
            details("AdminOrg_080_ApproveOrg").successfulRequests.count.is(2)
          )
        }
      case "pipeline" =>
        Seq(global.successfulRequests.percent.gte(95),
            forAll.successfulRequests.percent.gte(90)
        )
      case _ =>
        Seq()
    }
  }

	setUp(
		ManageAndApproveOrg.inject(simulationProfile(testType, approveOrgTargetPerHour, numberOfPipelineUsers)).pauses(pauseOption),
    ManageAndApproveOtherOrg.inject(simulationProfile(testType, approveOtherOrgTargetPerHour, numberOfPipelineUsers)).pauses(pauseOption),
		
	).protocols(httpProtocol)
     .assertions(assertions(testType))
}
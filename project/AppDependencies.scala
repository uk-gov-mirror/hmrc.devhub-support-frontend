import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  private val hmrcMongoVersion = "2.13.0"
  private val apiDomainVersion = "1.2.0"
  private val tpdDomainVersion = "0.15.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"             % "13.11.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "commons-validator"       %  "commons-validator"                      % "1.9.0",
    "uk.gov.hmrc"             %% "http-metrics"                           % "2.9.0",
    "uk.gov.hmrc"             %% "api-platform-api-domain"                % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain"                % tpdDomainVersion,
    "uk.gov.hmrc"             %% "play-conditional-form-mapping-play-30"  % "3.5.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"                 % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"                % hmrcMongoVersion,
    "org.jsoup"               %  "jsoup"                                  % "1.22.1",
    "uk.gov.hmrc"             %% "api-platform-api-domain-fixtures"       % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-test-tpd-domain"           % tpdDomainVersion
  ).map(_ % "test")

  val it = Seq.empty
}

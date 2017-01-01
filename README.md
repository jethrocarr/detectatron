# Detectatron

Detectatron is a web service intended for the purpose of feeding in video files from video surveillance systems and
applying AI to detect whether the video features undesirable elements such as the presence of humans. It is intended to
replace the shoddy pixel-based motion detection that ships as part of most off-the-shelf systems and instead look for
what really matters.

Detectatron is not intended for direct end user consumption, rather it is a backend processing service that should sit
behind other applications such as a public facing graphical frontend and integration sources such as S3/Lambda and
native video capture services.


# Features

Detectatron is in development, planned functionality is:

* Simple HTTP interface.
* Ability to scan still images.
* Ability to scan video, via means of extracting frames from video files.
* Provide summary metrics - Human in video true/false? Useful for alarm systems or "events of note" type systems.
* Provide detailed metrics - Useful for systems capable of using probability metrics from the backend AI.

The backend AI currently consists of Amazon Rekognition, however due to the high cost of per-image processing, it is
likely to take the form of a pipeline that involves some simple local in-app checks/validations to reduce the need to
query the more expensive AI (As of Jan 2017, $0.40 - $1 USD depending on volume).


# Requirements

The server running Detectatron needs to be properly configured with IAM credentials that can use the AWS Rekognition
service. If running inside AWS EC2, the use of IAM roles is highly recommended rather than hard-coded credentials.


# Usage

## Scanning Image Files

A still image can be POSTed as a multipart file upload to the `/scanner/image` endpoint:

    $ curl -F file=@samples/security_cam_path_person_close.jpg http://localhost:8080/scanner/image
    [
      {"name":"People","confidence":98.551994},
      {"name":"Person","confidence":98.55204},
      {"name":"Human","confidence":98.44648},
      {"name":"Plant","confidence":96.440735},
      {"name":"Potted Plant","confidence":96.440735}
    ]



# Build & Execution

Development is done with IntelliJ, however any Gradle & Java compatible IDE should work.

Standard gradle build commands can be used to build and run the application:

    gradle bootRun

A standalone self-contained JAR executable can be built and run with:

    gradle bootRepackage
    LATEST=`find build -name '*.jar' | tail -n1`
    java -jar $LATEST


# Testing

Detectatron aims for 80%+ code coverage with unit and integration tests. The tests can
be executed with:

    gradle test

If the tests fail, you can obtain additional information with the `--info`
parameter. This can show errors such as missing configuration causing faults
with the test suite.

    gradle test --info

Note that the tests require access to a function AWS account as a number
of the tests take place against AWS service endpoints.

It is possible to bypass tests by adding `-x test` to your normal gradle
commands, for example:

    gradle bootRun -x test

This of course is not recommended, but it can be useful if you need to separate
the build task and the testing task (eg as part of a CI/CD workflow).
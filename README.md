# Detectatron

Detectatron is a web service intended for the purpose of feeding in video files from video surveillance systems and
applying AI to detect whether the video features undesirable elements such as the presence of humans. It is intended to
replace the shoddy pixel-based motion detection that ships as part of most off-the-shelf systems and instead look for
what really matters.

Detectatron is not intended for direct end user consumption, rather it is a backend processing service that should sit
behind other applications such as a public facing graphical frontend and integration sources such as S3/Lambda and
native video capture services.


# Features

Detectatron is *still in development and is terrible code that needs a refactor
and much more unit testing*.

That being said, it does have the following features:

* A simple HTTP interface.
* Ability to scan still images.
* Ability to scan video, via means of extracting frames from video files.
* Provide summary metrics - Human in video true/false? Useful for alarm systems or "events of note" type systems.
* Provide detailed metrics - Useful for systems capable of using probability metrics from the backend AI.
* Accept a video, perform processing and return JSON with HTTP status codes indicating nature of detected event.
* Store video events in S3 for retention.

Currently the backend AI currently consists purely of Amazon Rekognition, however due to the high cost of per-image
processing, it would be nice to extend to use JavaCV (OpenCV) to do some sort of a pipeline that involves some simple
local in-app checks/validations to reduce the need to query the more expensive AWS service (As of Jan 2017,
$0.40 - $1 USD depending on volume).


# Requirements & Execution

The server running Detectatron needs to be properly configured with IAM credentials that can use the AWS Rekognition
service and the desired S3 bucket. If running inside AWS EC2, the use of IAM roles is highly recommended rather than
hard-coded credentials.

The following is an example suitable IAM policy that allows the use of Rekognition for label detection and grants the
ability to write files to an S3 bucket.

    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Stmt1486339503000",
                "Effect": "Allow",
                "Action": [
                    "s3:PutObject"
                ],
                "Resource": [
                    "arn:aws:s3:::YOUR_S3_BUCKET_HERE/*"
                ]
            }
        ]
    }
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Stmt1486349781000",
                "Effect": "Allow",
                "Action": [
                    "rekognition:DetectLabels"
                ],
                "Resource": [
                    "*"
                ]
            }
        ]
    }

If you wish for all videos that get uploaded to also be retained in an S3 bucket
you can also define an S3 bucket to store the files by setting the `S3_BUCKET`
environmental to your desired location when starting the application.

Eg:

    export S3_BUCKET=YOUR_S3_BUCKET_HERE
    java -jar -Xm512M JARFILE

The exact memory allocation will vary based on what you send the service, a
512MB heap seems to work nicely but less is possible.

Java/JDK 8+ is required.


# Usage with connector

The main intention is that Detectatron will be run alongside a connector which can pull video content from the camera
or video recording system and push it up. Detectatron can then store and score the video and return the data back to
the connector to use if suitable.

The following is the list of supported platforms and their associated connectors:
* [Unifi Video](https://github.com/jethrocarr/detectatron-connector-unifi)


## Arming/Disarming

Because of the high cost of image processing, this service has the ability to be armed/disarmed. When armed, the service
performs image detection and scoring. When disarmed, the service will not perform image detection and scoring. This
allows for easy integration with alarm systems.

The default state after application startup is "armed". To change state:

    curl http://localhost:8080/arming/
    curl http://localhost:8080/arming/armed
    curl http://localhost:8080/arming/disarmed

State is not persisted across application restarts.


## Submit Event

The event submission endpoint accepts a video file and stores in S3, as well as processing to look for interesting
elements in the video. It's intended for use by connector applications, such as the Unifi Connector.

    $ curl -F file=@samples/video_front_humans_1.mp4 http://localhost:8080/event



# Power Usage

Detectatron also provides API endpoints that expose additional information, these may be useful if writing your own
integrations or doing development and testing.


## Tagging Image Files

A still image can be POSTed as a multipart file upload to the `/tag/image` endpoint:

    $ curl -F file=@samples/security_cam_hallway_cat_closeup.jpg http://localhost:8080/tag/image
    {
      "rawLabels": [
        {
          "name": "Animal",
          "confidence": 83.152725
        },
        {
          "name": "Cat",
          "confidence": 83.152725
        },
        ......
        {
          "name": "Room",
          "confidence": 57.65361
        }
      ],
      "keyLabels": [
        {
          "name": "Cat",
          "confidence": 83.152725
        },
        {
          "name": "Pet",
          "confidence": 83.152725
        }
      ],
      "allTags": [
        "Animal",
        "Cat",
        "Mammal",
        "Manx",
        "Pet",
        "Canopy",
        "Chair",
        "Furniture",
        "Apartment",
        "Housing",
        "Indoors",
        "Room"
      ],
      "keyTags": [
        "Cat",
        "Pet"
      ]
    }


## Tagging Video Files

A video file can be POSTed as a multipart file upload to the `/tag/video` endpoint:

    $ curl -F file=@samples/video_front_humans_1.mp4 http://localhost:8080/tag/video


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


# License

    Detectatron is licensed under the Apache License, Version 2.0 (the "License").
    See the LICENSE.txt or http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

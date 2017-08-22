[![Codacy Badge](https://api.codacy.com/project/badge/Grade/83364a10ed534f6e8961564d4877a039)](https://www.codacy.com/app/joesan/power-simulator?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=joesan/power-simulator&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/joesan/power-simulator/branch/master/graph/badge.svg)](https://codecov.io/gh/joesan/power-simulator)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/joesan/power-simulator.svg?branch=master)](https://travis-ci.org/joesan/power-simulator)

Note: This project is not maintained anymore. Please have a look at: https://github.com/joesan/plant-simulator

# power-simulator
A simple simulator app that simulates the operation of a power producing plant

For simplicity, this simulator assumes that there are essentially two types of power producing
plants:

1. A PowerPlant that is steerable by turning it on or off (ex., battery)
2. A PowerPlant that is steerable by ramping it up (ex., generators, gas turbines)

## Dependencies

[plant-simulator](https://github.com/joesan/plant-simulator)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 
See deployment for notes on how to deploy the project on a live system.

### Prerequisites

```
1. Install Java Version 8
2. Install Scala Version 2.11.8
3. Install IntelliJ - Latest community edition and then install the latest Scala plugin available
```

### Installing

Follow the steps below to import the project into IntelliJ

```
1. Clone the project from: 
   git clone https://github.com/joesan/power-simulator.git
   
2. Fire up IntelliJ and import the project
   
3. If you are opening IntelliJ for the first time, set up the Scala library in IntelliJ
```

### Running tests

You have the option here to either run tests from IntelliJ or from the command line

To run tests from the command line, do the following:

```
1. Open a terminal and navigate to the project root folder 
   
2. Issue the following command:
   sbt clean test
```
To run any specific tests from within IntelliJ, simply right click the test that you wish you
run and click Run

### Running the application

This application is built as a web application using the Play framework. To run the application,
perform the following steps: TODO: Descibe how to dockerize!

```
1. Open a terminal and navigate to the project root folder 
   
2. Issue the following command:
   sbt run
   
3. Navigate to the following url on your favorite browser:
   http://localhost:9000
   
   [TODO...] document!
```

## Deployment

[[TODO]] Add additional notes about how to deploy this on a live system

## Built With

* [SBT](http://www.scala-sbt.org/) - Scala Build Tool

## Contributing

[[TODO]] Add CONTRIBUTING.md to the project root

## Authors / Maintainers

* *Joesan*           - [Joesan @ GitHub](https://github.com/joesan/)

## License

Feel free to use it

## Acknowledgments

* To everybody that helped in this project
* The [Monix library](https://monix.io/)


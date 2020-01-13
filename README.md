# State Calculator

Calculates a new state out of an initial state and a series of events.

# Run all tests

```sbt test```

# Run unit tests

```sbt 'testOnly * -- -l tags.Integration'```

# Run integration tests

```sbt 'testOnly * -- -n tags.Integration'```

# Generate coverage report

```sbt clean coverage test coverageReport```

# Build

```sbt assembly```

# Run with script

```./run.sh src/test/resources/sample-initial.json src/test/resources/sample-events.json```
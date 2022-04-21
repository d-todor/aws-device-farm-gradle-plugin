//
// Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// Copyright 2017 Andreas Marschke. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License").
// You may not use this file except in compliance with the License.
// A copy of the License is located at
//
// http://aws.amazon.com/apache2.0
//
// or in the "license" file accompanying this file. This file is distributed
// on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
// express or implied. See the License for the specific language governing
// permissions and limitations under the License.
//
package com.amazonaws.devicefarm;

import com.amazonaws.devicefarm.extension.DeviceFarmExtension;
import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.model.ExecutionResult;
import com.amazonaws.services.devicefarm.model.ExecutionStatus;
import com.amazonaws.services.devicefarm.model.GetRunRequest;
import com.amazonaws.services.devicefarm.model.Run;

import org.gradle.api.logging.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DeviceFarmResultPoller {

    private final Logger logger;
    private final AWSDeviceFarm api;
    private final DeviceFarmUtils utils;
    private long SLEEP_STEP_MS = TimeUnit.SECONDS.toMillis(10);

    public DeviceFarmResultPoller(DeviceFarmExtension extension,
                                  Logger logger,
                                  AWSDeviceFarm deviceFarmClient,
                                  DeviceFarmUtils utils) {
        this.logger = logger;
        this.api = deviceFarmClient;
        this.utils = utils;
    }

    public Run pollTestRunForArn(String arn) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        logger.lifecycle("Monitoring Run from Arn: " + arn);
        String lastStatus = "";
        while (true) {
            Run run = api.getRun(new GetRunRequest().withArn(arn)).getRun();

            final long currentTime = System.currentTimeMillis();
            final long elapsedTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime);
            if (!lastStatus.equals(run.getStatus())) {
                logger.lifecycle("Test running for " + elapsedTimeSeconds + " seconds, status: " + run.getStatus());
                lastStatus = run.getStatus();
            }

            if (run.getStatus().equals(ExecutionStatus.COMPLETED.toString())) {
                final String result = run.getResult();
                if (ExecutionResult.PASSED.toString().equals(result)) {
                    logger.lifecycle("Run reached COMPLETED SUCCESSFULLY!");
                    return run;
                } else {
                    logger.lifecycle("Run completed with a non-success result " + result);
                    logger.lifecycle("See " + utils.getRunUrlFromArn(run.getArn()) + " for details");
                    throw new RuntimeException("Run completed with a non-success result " + result);
                }
            }

            Thread.sleep(SLEEP_STEP_MS);
        }
    }
}


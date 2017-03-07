/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.compatibility.common.tradefed;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelperTest;
import com.android.compatibility.common.tradefed.command.CompatibilityConsoleTest;
import com.android.compatibility.common.tradefed.config.ConfigurationFactoryTest;
import com.android.compatibility.common.tradefed.presubmit.IntegrationTest;
import com.android.compatibility.common.tradefed.presubmit.PresubmitSetupValidation;
import com.android.compatibility.common.tradefed.presubmit.ValidateTestsAbi;
import com.android.compatibility.common.tradefed.result.ChecksumReporterTest;
import com.android.compatibility.common.tradefed.result.ConsoleReporterTest;
import com.android.compatibility.common.tradefed.result.MetadataReporterTest;
import com.android.compatibility.common.tradefed.result.ResultReporterTest;
import com.android.compatibility.common.tradefed.result.SubPlanCreatorTest;
import com.android.compatibility.common.tradefed.targetprep.PropertyCheckTest;
import com.android.compatibility.common.tradefed.targetprep.SettingsPreparerTest;
import com.android.compatibility.common.tradefed.testtype.CompatibilityHostTestBaseTest;
import com.android.compatibility.common.tradefed.testtype.CompatibilityTestTest;
import com.android.compatibility.common.tradefed.testtype.JarHostTestTest;
import com.android.compatibility.common.tradefed.testtype.ModuleDefTest;
import com.android.compatibility.common.tradefed.testtype.ModuleRepoTest;
import com.android.compatibility.common.tradefed.testtype.SubPlanTest;
import com.android.compatibility.common.tradefed.util.CollectorUtilTest;
import com.android.compatibility.common.tradefed.util.OptionHelperTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A test suite for all compatibility tradefed unit tests.
 * <p/>
 * All tests listed here should be self-contained, and do not require any external dependencies.
 */
@RunWith(Suite.class)
@SuiteClasses({
    // build
    CompatibilityBuildHelperTest.class,

    // command
    CompatibilityConsoleTest.class,

    //config
    ConfigurationFactoryTest.class,

    // presubmit
    IntegrationTest.class,
    PresubmitSetupValidation.class,
    ValidateTestsAbi.class,

    //result
    ChecksumReporterTest.class,
    ConsoleReporterTest.class,
    MetadataReporterTest.class,
    ResultReporterTest.class,
    SubPlanCreatorTest.class,

    // targetprep
    PropertyCheckTest.class,
    SettingsPreparerTest.class,

    // testtype
    CompatibilityHostTestBaseTest.class,
    CompatibilityTestTest.class,
    JarHostTestTest.class,
    ModuleDefTest.class,
    ModuleRepoTest.class,
    SubPlanTest.class,

    // util
    CollectorUtilTest.class,
    OptionHelperTest.class,
})
public class UnitTests {
    // empty on purpose
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.compatibility.common.tradefed.targetprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.OptionSetter;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.util.FileUtil;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Unit tests for {@link DynamicConfigPusher}.
 */
@RunWith(JUnit4.class)
public class DynamicConfigPusherTest {
    private static final String RESOURCE_DYNAMIC_CONFIG = "test-dynamic-config";
    private DynamicConfigPusher mPreparer;
    private ITestDevice mMockDevice;
    private CompatibilityBuildHelper mMockBuildHelper;
    private IBuildInfo mMockBuildInfo;

    @Before
    public void setUp() {
        mPreparer = new DynamicConfigPusher();
        mMockDevice = EasyMock.createMock(ITestDevice.class);
        mMockBuildInfo = EasyMock.createMock(IBuildInfo.class);
        mMockBuildHelper = new CompatibilityBuildHelper(mMockBuildInfo);
        EasyMock.expect(mMockDevice.getDeviceDescriptor()).andStubReturn(null);
    }

    /**
     * Test that when we look up resources locally, we search them from the build helper.
     */
    @Test
    public void testLocalRead() throws Exception {
        OptionSetter setter = new OptionSetter(mPreparer);
        setter.setOptionValue("config-filename", "config-test-name");
        setter.setOptionValue("extract-from-resource", "false");

        File check = new File("anyfilewilldo");
        mMockBuildHelper = new CompatibilityBuildHelper(mMockBuildInfo) {
            @Override
            public File getTestFile(String filename) throws FileNotFoundException {
                return check;
            }
        };

        EasyMock.replay(mMockDevice, mMockBuildInfo);
        File res = mPreparer.getLocalConfigFile(mMockBuildHelper, mMockDevice);
        assertEquals(check, res);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    /**
     * Test that when we look up resources locally, we search them from the build helper and throw
     * if it's not found.
     */
    @Test
    public void testLocalRead_fileNotFound() throws Exception {
        OptionSetter setter = new OptionSetter(mPreparer);
        setter.setOptionValue("config-filename", "config-test-name");
        setter.setOptionValue("extract-from-resource", "false");

        mMockBuildHelper = new CompatibilityBuildHelper(mMockBuildInfo) {
            @Override
            public File getTestFile(String filename) throws FileNotFoundException {
                throw new FileNotFoundException("test");
            }
        };
        try {
            EasyMock.replay(mMockDevice, mMockBuildInfo);
            mPreparer.getLocalConfigFile(mMockBuildHelper, mMockDevice);
            fail("Should have thrown an exception.");
        } catch (TargetSetupError expected) {
            // expected
            assertEquals("Cannot get local dynamic config file from test directory null",
                    expected.getMessage());
        }
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    /**
     * Test when we try to unpack a resource but it does not exists.
     */
    @Test
    public void testResourceRead_notFound() throws Exception {
        OptionSetter setter = new OptionSetter(mPreparer);
        setter.setOptionValue("config-filename", "not-an-existing-resource-name");
        setter.setOptionValue("extract-from-resource", "true");
        try {
            EasyMock.replay(mMockDevice, mMockBuildInfo);
            mPreparer.getLocalConfigFile(mMockBuildHelper, mMockDevice);
            fail("Should have thrown an exception.");
        } catch (TargetSetupError expected) {
            // expected
            assertEquals("Fail to unpack 'not-an-existing-resource-name.dynamic' from resources "
                    + "null", expected.getMessage());
        }
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    /**
     * Test when we get a config from the resources.
     */
    @Test
    public void testResourceRead() throws Exception {
        OptionSetter setter = new OptionSetter(mPreparer);
        setter.setOptionValue("config-filename", RESOURCE_DYNAMIC_CONFIG);
        setter.setOptionValue("extract-from-resource", "true");
        File res = null;
        try {
            EasyMock.replay(mMockDevice, mMockBuildInfo);
            res = mPreparer.getLocalConfigFile(mMockBuildHelper, mMockDevice);
            assertTrue(res.exists());
            assertTrue(FileUtil.readStringFromFile(res).contains("<dynamicConfig>"));
        } finally {
            FileUtil.deleteFile(res);
        }
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    /**
     * Test when we get a config from the resources under the alternative name.
     */
    @Test
    public void testResourceRead_resourceFileName() throws Exception {
        OptionSetter setter = new OptionSetter(mPreparer);
        setter.setOptionValue("config-filename", "moduleName");
        setter.setOptionValue("extract-from-resource", "true");
        // Look up the file under that name instead of the config-filename
        setter.setOptionValue("dynamic-resource-name", RESOURCE_DYNAMIC_CONFIG);
        File res = null;
        try {
            EasyMock.replay(mMockDevice, mMockBuildInfo);
            res = mPreparer.getLocalConfigFile(mMockBuildHelper, mMockDevice);
            assertTrue(res.exists());
            assertTrue(FileUtil.readStringFromFile(res).contains("<dynamicConfig>"));
        } finally {
            FileUtil.deleteFile(res);
        }
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }
}

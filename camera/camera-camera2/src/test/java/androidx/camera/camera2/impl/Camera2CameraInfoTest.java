/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.view.Surface;

import androidx.camera.core.CameraInfoInternal;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.TorchState;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class Camera2CameraInfoTest {

    private static final String CAMERA0_ID = "0";
    private static final int CAMERA0_SENSOR_ORIENTATION = 90;
    @CameraSelector.LensFacing
    private static final int CAMERA0_LENS_FACING_ENUM = CameraSelector.LENS_FACING_BACK;
    private static final int CAMERA0_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_BACK;
    private static final boolean CAMERA0_FLASH_INFO_BOOLEAN = true;

    private static final String CAMERA1_ID = "1";
    private static final int CAMERA1_SENSOR_ORIENTATION = 0;
    private static final int CAMERA1_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_FRONT;
    private static final boolean CAMERA1_FLASH_INFO_BOOLEAN = false;

    private static final int FAKE_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3;

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics0;
    private CameraCharacteristics mCameraCharacteristics1;
    private ZoomControl mMockZoomControl;
    private TorchControl mMockTorchControl;

    @Before
    public void setUp() throws CameraAccessException {
        initCameras();

        mCameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        mCameraCharacteristics0 = mCameraManager.getCameraCharacteristics(CAMERA0_ID);
        mCameraCharacteristics1 = mCameraManager.getCameraCharacteristics(CAMERA1_ID);

        mMockZoomControl = mock(ZoomControl.class);
        mMockTorchControl = mock(TorchControl.class);
    }

    @Test
    public void canCreateCameraInfo() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        assertThat(cameraInfoInternal).isNotNull();
    }

    @Test
    public void cameraInfo_canReturnSensorOrientation() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        assertThat(cameraInfoInternal.getSensorRotationDegrees()).isEqualTo(
                CAMERA0_SENSOR_ORIENTATION);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forBackCamera() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);

        // Note: these numbers depend on the camera being a back-facing camera.
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA0_SENSOR_ORIENTATION);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 90 + 360) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 180 + 360) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 270 + 360) % 360);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forFrontCamera() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics1,
                mMockZoomControl, mMockTorchControl);

        // Note: these numbers depend on the camera being a front-facing camera.
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA1_SENSOR_ORIENTATION);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 90) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 180) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 270) % 360);
    }

    @Test
    public void cameraInfo_canReturnLensFacing() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        assertThat(cameraInfoInternal.getLensFacing()).isEqualTo(CAMERA0_LENS_FACING_ENUM);
    }

    @Test
    public void cameraInfo_canReturnHasFlashUnit_forBackCamera() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        assertThat(cameraInfoInternal.hasFlashUnit()).isEqualTo(CAMERA0_FLASH_INFO_BOOLEAN);
    }

    @Test
    public void cameraInfo_canReturnHasFlashUnit_forFrontCamera() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics1,
                mMockZoomControl, mMockTorchControl);
        assertThat(cameraInfoInternal.hasFlashUnit()).isEqualTo(CAMERA1_FLASH_INFO_BOOLEAN);
    }

    @Test
    public void cameraInfo_canReturnTorchState() {
        CameraInfoInternal cameraInfoInternal = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        when(mMockTorchControl.getTorchState()).thenReturn(new MutableLiveData<>(TorchState.OFF));
        assertThat(cameraInfoInternal.getTorchState().getValue()).isEqualTo(TorchState.OFF);
    }

    // zoom related tests just ensure it uses ZoomControl to get the value
    // Full tests are performed at ZoomControlTest / ZoomControlRoboTest.
    @Test
    public void cameraInfo_getZoomRatio_valueIsCorrect() {
        CameraInfoInternal cameraInfo = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        when(mMockZoomControl.getZoomRatio()).thenReturn(new MutableLiveData<>(3.0f));
        assertThat(cameraInfo.getZoomRatio().getValue()).isEqualTo(3.0f);
    }

    @Test
    public void cameraInfo_getZoomPercentage_valueIsCorrect() {
        CameraInfoInternal cameraInfo = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        when(mMockZoomControl.getLinearZoom()).thenReturn(new MutableLiveData<>(0.2f));
        assertThat(cameraInfo.getLinearZoom().getValue()).isEqualTo(0.2f);
    }

    @Test
    public void cameraInfo_getMaxZoomRatio_valueIsCorrect() {
        CameraInfoInternal cameraInfo = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        when(mMockZoomControl.getMaxZoomRatio()).thenReturn(new MutableLiveData<>(8.0f));
        assertThat(cameraInfo.getMaxZoomRatio().getValue()).isEqualTo(8.0f);
    }

    @Test
    public void cameraInfo_getMinZoomRatio_valueIsCorrect() {
        CameraInfoInternal cameraInfo = new Camera2CameraInfo(mCameraCharacteristics0,
                mMockZoomControl, mMockTorchControl);
        when(mMockZoomControl.getMinZoomRatio()).thenReturn(new MutableLiveData<>(1.0f));
        assertThat(cameraInfo.getMinZoomRatio().getValue()).isEqualTo(1.0f);
    }


    private void initCameras() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                FAKE_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics0.set(CameraCharacteristics.LENS_FACING, CAMERA0_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics0.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA0_SENSOR_ORIENTATION);

        // Mock the flash unit availability
        shadowCharacteristics0.set(
                CameraCharacteristics.FLASH_INFO_AVAILABLE, CAMERA0_FLASH_INFO_BOOLEAN);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        // **** Camera 1 characteristics ****//
        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                FAKE_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics1.set(CameraCharacteristics.LENS_FACING, CAMERA1_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics1.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA1_SENSOR_ORIENTATION);

        // Mock the flash unit availability
        shadowCharacteristics1.set(
                CameraCharacteristics.FLASH_INFO_AVAILABLE, CAMERA1_FLASH_INFO_BOOLEAN);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }
}

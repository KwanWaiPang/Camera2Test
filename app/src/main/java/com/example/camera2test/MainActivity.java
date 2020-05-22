package com.example.camera2test;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.Arrays;

import static android.hardware.camera2.CaptureRequest.SENSOR_EXPOSURE_TIME;

public class MainActivity extends AppCompatActivity {

    private TextureView mTextureView;//显示数据流的UI控件
    private CameraCaptureSession mCameraCaptureSession;//是一个事务，用来向相机设备发送获取图像的请求。
    private CameraDevice mCameraDevice;//是一个连接的相机设备代表，你可以把它看作为相机设备在 java 代码中的表现
    private Surface mPreviewSurface;//Surface来自控件TextureView，它用来显示摄像头的图像，

    private CameraCharacteristics mCameraCharacteristics;

    //权限
    private static String[] PERMISSIONS_STORAGE = {
//            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,//写权限
            Manifest.permission.CAMERA//照相权限
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ////如果提示【Fail to connect to camera service】很可能是没申请权限，或申请权限了但用户没有给你权限
        //华为手机摄像头权限申请
        //用于判断SDK版本是否大于23
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            //检查权限
            int i = ContextCompat.checkSelfPermission(this,PERMISSIONS_STORAGE[0]);
            //如果权限申请失败，则重新申请权限
            if(i!= PackageManager.PERMISSION_GRANTED){
                //重新申请权限函数
                startRequestPermission();
                Log.e("这里","权限请求成功");
            }
        }



        //预览用的surface
        mTextureView = (TextureView) this.findViewById(R.id.texture_view_back);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            //SurfaceTexture准备就绪后调用这个方法
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // TODO 自动生成的方法存根
                mPreviewSurface = new Surface(surface);//定义一个surface

                //1、先通过通过context.getSystemService(Context.CAMERA_SERVICE) 方法来获取CameraManager
                final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);//系统服务，专门用于检测和打开相机，以及获取相机设备特性

                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    //2、再通过调用CameraManager .open()方法在回调中得到CameraDevice。ID0为后置摄像头
                    manager.openCamera("0", new CameraDevice.StateCallback() {//打开指定的相机设备

                        //成功打开时的回调，此时 camera 就准备就绪，并且可以得到一个 CameraDevice 实例。
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            mCameraDevice = camera;//所回调的相机设备赋予给mCameraDevice
                            try {
                                //获取曝光时间这个属性。
                                mCameraCharacteristics = manager.getCameraCharacteristics(Integer.toString(0));
                                Range<Long> range=mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                                long max=range.getUpper();
                                long min=range.getLower();
                                //通过Toast输出
                                Toast.makeText(MainActivity.this, "max:"+max+"min:"+min,Toast.LENGTH_LONG).show();




                                //CameraCaptureSession 是一个事务，用来向相机设备发送获取图像的请求
                                //3、通过CameraDevice.createCaptureSession() 在回调中获取CameraCaptureSession
                                mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface), new CameraCaptureSession.StateCallback() {
                                    //CameraCaptureSession.StateCallback是其内部类

                                    //相机设备完成配置，并开始处理捕捉请求时回调
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {

                                        mCameraCaptureSession = session;//是一个事务，用来向相机设备发送获取图像的请求。
                                        try {
                                            //4、构建CaptureRequest, 有三种模式可选 预览/拍照/录像
                                            //通过下面方法获得一个CaptureRequest.Builder对象。
                                            //基本配置都是通过该构造者来配置
                                            //最后通过 CaptureRequest.Builder 对象的 build() 方法便可得到 CaptureRequest 实例（见setRepeatingRequest方法）
                                            CaptureRequest.Builder builder;//先拿到一个 CaptureRequest.Builder 对象
                                            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                            //TEMPLATE_PREVIEW ： 用于创建一个相机预览请求。相机会优先保证高帧率而不是高画质。适用于所有相机设备
                                            //TEMPLATE_STILL_CAPTURE ： 用于创建一个拍照请求。
                                            //TEMPLATE_RECORD ： 用于创建一个录像请求。

                                            //设置曝光时间

//                                            builder.set(CaptureRequest.BLACK_LEVEL_LOCK, false);//黑电平补偿是锁定
                                            //要先设置下面两句，才可以控制曝光度
                                            builder.set(CaptureRequest.CONTROL_AE_MODE,0);
                                            builder.set(CaptureRequest.CONTROL_MODE,0);
                                            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) 10000);


                                            //通过 CaptureRequest.Builder 对象设置一些捕捉请求的配置
                                            //设置指定key的值
//                                            builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                            builder.addTarget(mPreviewSurface);//绑定目标Surface

                                            //5、通过 CameraCaptureSession发送CaptureRequest, capture表示只发一次请求, setRepeatingRequest表示不断发送请求.
                                            //不断的重复请求捕捉画面，常用于预览或者连拍场景。
                                            mCameraCaptureSession.capture(builder.build(), null, null);
                                        } catch (CameraAccessException e1) {
                                            e1.printStackTrace();
                                        }
                                    }

                                    //该会话无法按照相应的配置发起请求时回调
                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                                    }
                                }, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        //当 camera 不再可用或打开失败时的回调，通常在该方法中进行资源释放的操作。
                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {

                        }

                        //当 camera 打开失败时的回调，error 为具体错误原因
                        // 定义在 CameraDevice.StateCallback 类中。通常在该方法中也要进行资源释放的操作。
                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {

                        }
                    }, null);
                }catch (CameraAccessException e){
                    e.printStackTrace();
                }
            }


            //在surface发生format或size变化时调用。
            //SurfaceTexture缓冲大小变化
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }


            //SurfaceTexture即将被销毁
            //在此处回调做一些释放资源的操作
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }


            //SurfaceTexture通过updateImage更新
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }


    private void startRequestPermission(){
        //321为请求码
        ActivityCompat.requestPermissions(this,PERMISSIONS_STORAGE,321);
    }

}

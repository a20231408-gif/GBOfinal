package com.tuempresa.inventariovial.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

object FieldCameraSelection {
    fun select(context:Context,id:String?) { context.getSharedPreferences("field_camera",0).edit().putString("id",id).apply() }
    fun chosen(context:Context)=context.getSharedPreferences("field_camera",0).getString("id",null)
}
/** Uses Camera2 for an explicitly selected lens; Android activity remains the default. */
class FieldTakePicture:ActivityResultContracts.TakePicture() {
    override fun createIntent(context:Context,input:Uri):Intent {
        val id=FieldCameraSelection.chosen(context) ?: return super.createIntent(context,input)
        return Intent(context,FieldCameraActivity::class.java).putExtra("cameraId",id).putExtra("output",input)
    }
}

class FieldCameraActivity:ComponentActivity() {
    private lateinit var texture:TextureView
    private lateinit var label:TextView
    private lateinit var button:Button
    private var device:CameraDevice?=null
    private var session:CameraCaptureSession?=null
    private var reader:ImageReader?=null
    private var preview:Surface?=null
    private val thread=HandlerThread("field-camera")
    private lateinit var handler:Handler
    private var opening=false
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        thread.start();handler=Handler(thread.looper)
        setResult(Activity.RESULT_CANCELED)
        texture=TextureView(this);label=TextView(this);button=Button(this).apply { text="Tomar fotografía";isEnabled=false;setOnClickListener { capture() } }
        val layout=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            addView(texture,LinearLayout.LayoutParams(-1,0,1f));addView(label);addView(button)
            addView(Button(this@FieldCameraActivity).apply { text="Cancelar";setOnClickListener { finish() } })
        }
        setContentView(layout)
        texture.surfaceTextureListener=object:TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(s:SurfaceTexture,w:Int,h:Int) { open() }
            override fun onSurfaceTextureSizeChanged(s:SurfaceTexture,w:Int,h:Int) = Unit
            override fun onSurfaceTextureUpdated(s:SurfaceTexture) = Unit
            override fun onSurfaceTextureDestroyed(s:SurfaceTexture):Boolean { close();return true }
        }
    }
    override fun onResume() { super.onResume();if(texture.isAvailable) open() }
    override fun onPause() { close();super.onPause() }
    private fun fail(e:Throwable) { runOnUiThread { label.text="No se pudo usar esta cámara: ${e.message}";button.isEnabled=false } }
    @Suppress("MissingPermission") private fun open() {
        if(opening || device!=null || !texture.isAvailable) return
        opening=true
        try {
            val manager=getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id=requireNotNull(intent.getStringExtra("cameraId"))
            val chars=manager.getCameraCharacteristics(id)
            val map=requireNotNull(chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
            val jpeg=map.getOutputSizes(android.graphics.ImageFormat.JPEG).maxBy { it.width.toLong()*it.height }
            reader=ImageReader.newInstance(jpeg.width,jpeg.height,android.graphics.ImageFormat.JPEG,2)
            reader!!.setOnImageAvailableListener({ source ->
                source.acquireLatestImage()?.use { image ->
                    try {
                        val bytes=ByteArray(image.planes[0].buffer.remaining());image.planes[0].buffer.get(bytes)
                        @Suppress("DEPRECATION") val uri=requireNotNull(intent.getParcelableExtra<Uri>("output"))
                        requireNotNull(contentResolver.openOutputStream(uri,"wt")).use { it.write(bytes) }
                        runOnUiThread { setResult(Activity.RESULT_OK);finish() }
                    } catch(e:Exception) { fail(e) }
                }
            },handler)
            val size=map.getOutputSizes(SurfaceTexture::class.java).filter { it.width<=1920 }.maxByOrNull { it.width.toLong()*it.height }
                ?: map.getOutputSizes(SurfaceTexture::class.java).first()
            texture.surfaceTexture!!.setDefaultBufferSize(size.width,size.height)
            preview=Surface(texture.surfaceTexture)
            manager.openCamera(id,object:CameraDevice.StateCallback() {
                override fun onOpened(camera:CameraDevice) {
                    opening=false
                    if(isFinishing || !texture.isAvailable || reader==null || preview==null) { camera.close();return }
                    device=camera
                    @Suppress("DEPRECATION") camera.createCaptureSession(listOf(preview!!,reader!!.surface),object:CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s:CameraCaptureSession) {
                            if(device!=camera || reader==null) { s.close();return }
                            session=s
                            try {
                                val request=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(preview!!) }
                                s.setRepeatingRequest(request.build(),null,handler)
                                runOnUiThread { label.text="${jpeg.width} × ${jpeg.height}";button.isEnabled=true }
                            } catch(e:Exception) { fail(e) }
                        }
                        override fun onConfigureFailed(s:CameraCaptureSession) { fail(IllegalStateException("Vista previa no disponible")) }
                    },handler)
                }
                override fun onDisconnected(camera:CameraDevice) { camera.close();device=null;opening=false;fail(IllegalStateException("Cámara desconectada")) }
                override fun onError(camera:CameraDevice,error:Int) { camera.close();device=null;opening=false;fail(IllegalStateException("Error $error")) }
            },handler)
        } catch(e:Exception) { opening=false;fail(e) }
    }
    private fun capture() {
        try {
            val camera=requireNotNull(device);val manager=getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val chars=manager.getCameraCharacteristics(camera.id)
            @Suppress("DEPRECATION") val rotation=windowManager.defaultDisplay.rotation
            val degrees=when(rotation) {Surface.ROTATION_90->90;Surface.ROTATION_180->180;Surface.ROTATION_270->270;else->0}
            val sensor=chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val front=chars.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT
            val request=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader!!.surface)
                set(CaptureRequest.JPEG_ORIENTATION,(sensor+(if(front) degrees else -degrees)+360)%360)
            }
            button.isEnabled=false
            session!!.capture(request.build(),object:CameraCaptureSession.CaptureCallback() {
                override fun onCaptureFailed(s:CameraCaptureSession,r:CaptureRequest,f:CaptureFailure) { fail(IllegalStateException("Captura fallida")) }
            },handler)
        } catch(e:Exception) { fail(e) }
    }
    private fun close() { session?.close();session=null;device?.close();device=null;reader?.close();reader=null;preview?.release();preview=null;opening=false }
    override fun onDestroy() { close();thread.quitSafely();super.onDestroy() }
}

package com.devdroid.posedetectiononvideo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(), SurfaceTextureListener, MoviePlayer.PlayerFeedback {

    private lateinit var poseDetector: PoseDetector
    private var mPlayTask: MoviePlayer.PlayTask? = null
    private lateinit var uri: Uri
    private var mSurfaceTextureReady = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelectFile?.setOnClickListener {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_PICK
            startActivityForResult(Intent.createChooser(intent, "Select Video"), 101);
            checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE
            )
        }


        btnPlay.setOnClickListener {

            clickPlayStop()
        }

        mTextureView.surfaceTextureListener = this

        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            Toast.makeText(this@MainActivity, "Permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


            @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK && requestCode == 101){
            if(data?.data!=null){

                uri = data.data!!

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(applicationContext,uri)
                imgView.setImageBitmap(mediaMetadataRetriever.getFrameAtIndex(0))

            }
        }
    }

    override fun onPause() {
        super.onPause()
        // We're not keeping track of the state in static fields, so we need to shut the
        // playback down.  Ideally we'd preserve the state so that the player would continue
        // after a device rotation.
        //
        // We want to be sure that the player won't continue to send frames after we pause,
        // because we're tearing the view down.  So we wait for it to stop here.
        if (mPlayTask != null) {
            stopPlayback()
            mPlayTask!!.waitForStop()
        }
    }
    private fun stopPlayback() {
        mPlayTask?.requestStop()
    }

    fun clickPlayStop() {

            if (mPlayTask != null) {

                return
            }

            val callback = SpeedControlCallback()
            /*if (((CheckBox) findViewById(R.id.locked60fps_checkbox)).isChecked()) {
                // TODO: consider changing this to be "free running" mode

                callback.setFixedPlaybackRate(60);
            }*/
            val st: SurfaceTexture = mTextureView.getSurfaceTexture()!!
            val surface = Surface(st)
            var player: MoviePlayer? = null
            Log.d(
                "FILE:",
                "" + Environment.getExternalStorageDirectory().absolutePath
                        + "/video.mp4" + "  "
            )

            if(uri!=null){
                val uriPathHelper = URIPathHelper()
                val path = uriPathHelper.getPath(applicationContext,uri)
                try {
                    player = MoviePlayer(File(path), surface, callback)
                } catch (ioe: IOException) {
                    surface.release()
                    return
                }
                adjustAspectRatio(player.getVideoWidth(), player.getVideoHeight())
                mPlayTask = MoviePlayer.PlayTask(player, this)
                mPlayTask!!.execute()
            }
            else{

            }

    }

    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val viewWidth = mTextureView.width
        val viewHeight = mTextureView.height
        val aspectRatio = videoHeight.toDouble() / videoWidth
        val newWidth: Int
        val newHeight: Int
        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt()
            newHeight = viewHeight
        }
        val xoff = (viewWidth - newWidth) / 2
        val yoff = (viewHeight - newHeight) / 2
        val txform = Matrix()
        mTextureView.getTransform(txform)
        txform.setScale(
            newWidth.toFloat() / viewWidth,
            newHeight.toFloat() / viewHeight
        )
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        mTextureView.setTransform(txform)
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {


        Log.d("LOG:", "Here")

        val bm = mTextureView.bitmap

        if (bm != null) {
            Log.d("LOG:", "not null")
            val inputImage = InputImage.fromBitmap(bm, 0)
            poseDetector.process(inputImage)
                .addOnSuccessListener { pose ->
                    if (parentLayout.childCount > 4) parentLayout.removeViewAt(4)
                    imgView.setImageBitmap(bm)
                    if (!pose.allPoseLandmarks.isEmpty()) {
                        val draw = Draw(applicationContext, pose)
                        parentLayout.addView(draw)
                    }
                    Log.d("LOG:", "Success")
                }
                .addOnFailureListener { Log.d("LOG:", "Failure") }
        } else Log.d("LOG:", "null")
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        mSurfaceTextureReady = false
        // assume activity is pausing, so don't need to update controls
        // assume activity is pausing, so don't need to update controls
        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {

        mSurfaceTextureReady = true
        val bm = mTextureView.bitmap
        imgView.setImageBitmap(bm)
    }

    override fun playbackStopped() {

    }


}

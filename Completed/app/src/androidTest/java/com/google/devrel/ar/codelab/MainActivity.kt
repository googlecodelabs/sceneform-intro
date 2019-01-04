package com.google.devrel.ar.codelab

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast

import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

import pro.izdi.sceneformkt.nodes.PointCloudNode

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    lateinit var trackableGestureDetector: GestureDetector

    lateinit var andyRenderable: ModelRenderable
    lateinit var arFragment: ArFragment

    lateinit var pointCloudNode: PointCloudNode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ux)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)

        pointCloudNode = PointCloudNode(this)
        arFragment.arSceneView.scene.addChild(pointCloudNode)

        arFragment.arSceneView.planeRenderer.isEnabled = false

        arFragment.arSceneView.scene.setOnUpdateListener(::onFrame)

        ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build()
            .thenAccept { renderable -> andyRenderable = renderable }
            .exceptionally { throwable ->
                val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }

        // set on tap listener
        arFragment.arSceneView.scene.setOnPeekTouchListener { hitTestResult, motionEvent ->
            arFragment.onPeekTouch(hitTestResult, motionEvent)

            if (hitTestResult.node != null) {
                Log.i(TAG, "Touching a Sceneform node")
            }

            trackableGestureDetector.onTouchEvent(motionEvent)
        }
        trackableGestureDetector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    onSingleTap(e)
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }
            }
        )
    }

    fun onFrame(frameTime: FrameTime) {
        arFragment.onUpdate(frameTime)
        val frame = arFragment.arSceneView.arFrame
        val camera = frame.camera

        // If not tracking, don't draw 3d objects.
        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        // Visualize tracked points.
        val pointCloud = frame.acquirePointCloud()
        pointCloudNode.update(pointCloud)

        // Application is responsible for releasing the point cloud resources after using it.
        pointCloud.release()

    }

    fun onSingleTap(motionEvent: MotionEvent) {
        val arSceneView = arFragment.arSceneView
        val frame = arSceneView.arFrame
        if (frame != null && frame.camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(motionEvent)) {
                Log.i(TAG, "Distance to a hit: ${hit.distance}")
                val trackable = hit.trackable
                if (trackable is Point) {
                    // Anchor down
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arSceneView.scene)

                    val node = Node()
                    node.setParent(anchorNode)
                    node.worldRotation = Quaternion()

                    // Create node and add to the anchor
                    val andy = TransformableNode(arFragment.transformationSystem)
                    andy.setParent(node)
                    andy.renderable = andyRenderable
                    andy.select()
                }
                Log.i(TAG, "Instance of ${trackable.javaClass.name}")
            }
        }
        Log.i(TAG, "Tracking state: ${frame.camera.trackingState}")
    }
}

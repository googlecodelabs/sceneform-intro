package com.google.devrel.ar.codelab;

import android.content.Context;

import com.google.ar.core.PointCloud;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.RenderableDefinition;
import com.google.ar.sceneform.rendering.Vertex;

import java.nio.FloatBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PointCloudNode extends Node {
    private long timestamp;
    private Vertex[] ptbuffer;
    private int[] indexbuffer;

    private CompletableFuture<Material> materialHolder;

    // This is the extent of the point
    private static  final float POINT_DELTA = 0.003f;

    public PointCloudNode(Context context) {
        float r = 53/ 255f;
        float g = 174 /255f;
        float b = 256/255f;

        Color color = new Color(r,g,b,1.0f);
        materialHolder = MaterialFactory.makeOpaqueWithColor(context, color);
    }

    /**
     * Update the renderable for the point cloud.  This creates a small quad for each feature point.
     * @param cloud the ARCore point cloud.
     */
    public void update(PointCloud cloud) {

        if (!isEnabled()) {
            return;
        }
        // If this is the same cloud as last time, skip it.  Also, if the material has not loaded yet,
        // skip this.
        if (this.timestamp != cloud.getTimestamp() && materialHolder.getNow(null) != null) {
            timestamp = cloud.getTimestamp();
            FloatBuffer buf = cloud.getPoints();

            // Point clouds are 4 values x,y,z and a confidence value.
            int numFeatures = buf.limit()/4;

            // no features in the cloud
            if (numFeatures < 1) {
                setRenderable(null);
                return;
            }

            // 4 points per feature.
            int numPoints = numFeatures * 4;

            // draw a square (2 triangles) per feature.
            // 0 1 2
            // 2 3 0
            int numIndices = numFeatures * 6;

            // allocate a buffer on the high water mark.
            if (ptbuffer == null || ptbuffer.length < numPoints) {
                ptbuffer = new Vertex[numPoints];
                indexbuffer = new int[numIndices];
            }

            Vector3 feature = new Vector3();
            Vector3[] p = { new Vector3(), new Vector3(), new Vector3(), new Vector3()};

            for (int i = 0; i < buf.limit() / 4; i++) {
                // feature point
                feature.x = buf.get(i*4);
                feature.y = buf.get(i*4+1);
                feature.z = buf.get(i*4+2);

                p[0].x = feature.x;
                p[0].y = feature.y + POINT_DELTA;
                p[0].z = feature.z;

                p[1].x = feature.x + POINT_DELTA;
                p[1].y = feature.y;
                p[1].z = feature.z;

                p[2].x = feature.x;
                p[2].y = feature.y - POINT_DELTA;
                p[2].z = feature.z;

                p[3].x = feature.x + POINT_DELTA;
                p[3].y = feature.y;
                p[3].z = feature.z;

                ptbuffer[i*4] = Vertex.builder().setPosition(p[0]).build();
                ptbuffer[i*4+1] = Vertex.builder().setPosition(p[1]).build();
                ptbuffer[i*4+2] = Vertex.builder().setPosition(p[2]).build();
                ptbuffer[i*4+3] = Vertex.builder().setPosition(p[3]).build();

                indexbuffer[i * 6] = i*4;
                indexbuffer[i * 6 + 1] = i*4+1;
                indexbuffer[i * 6 + 2] = i*4+2;
                indexbuffer[i * 6 + 3] = i*4+2;
                indexbuffer[i * 6 + 4] = i*4+3;
                indexbuffer[i * 6 + 5] = i*4;
            }


            RenderableDefinition.Submesh submesh = RenderableDefinition.Submesh.builder()
                    .setName("pointcloud")
                    .setMaterial(materialHolder.getNow(null))
                    .setTriangleIndices(IntStream.of(indexbuffer).limit(numFeatures * 6).boxed().collect(Collectors.toList())).build();

            RenderableDefinition def = RenderableDefinition.builder()
                    .setVertices(Stream.of(ptbuffer).limit(numPoints).collect(Collectors.toList()))
                    .setSubmeshes(Stream.of(submesh).collect(Collectors.toList()))
                    .build();

            ModelRenderable.builder().setSource(def).build().thenAccept(this::setRenderable);
        }
    }
}

package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Runtime representation of the exact triangle mesh converted from the
 * project-provided m40_gasmask.glb.
 *
 * The GLB itself is NOT parsed at runtime. A tiny preconverted binary resource
 * is loaded once when this client class is initialized.
 */
public final class M40MaskMesh {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE =
            "/assets/domesurvival/models/m40_mask_mesh.bin";

    private static final MeshData DATA = load();

    private M40MaskMesh() {
    }

    public static int triangleCount() {
        return DATA.indices.length / 3;
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        if (DATA.indices.length == 0) {
            return;
        }

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normalMatrix = last.normal();

        for (int index : DATA.indices) {
            int v = index * 8;

            consumer.vertex(
                            pose,
                            DATA.vertices[v],
                            DATA.vertices[v + 1],
                            DATA.vertices[v + 2]
                    )
                    .color(255, 255, 255, 255)
                    .uv(
                            DATA.vertices[v + 6],
                            DATA.vertices[v + 7]
                    )
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(packedLight)
                    .normal(
                            normalMatrix,
                            DATA.vertices[v + 3],
                            DATA.vertices[v + 4],
                            DATA.vertices[v + 5]
                    )
                    .endVertex();
        }
    }

    private static MeshData load() {
        try (InputStream raw = M40MaskMesh.class.getResourceAsStream(RESOURCE)) {
            if (raw == null) {
                LOGGER.error("DomeSurvival M40 mesh resource is missing: {}", RESOURCE);
                return MeshData.EMPTY;
            }

            try (DataInputStream input =
                         new DataInputStream(new BufferedInputStream(raw))) {
                byte[] magic = new byte[4];
                input.readFully(magic);

                if (magic[0] != 'D'
                        || magic[1] != 'M'
                        || magic[2] != '4'
                        || magic[3] != '0') {
                    throw new IllegalStateException("Invalid M40 mesh magic");
                }

                int version = input.readInt();
                if (version != 1) {
                    throw new IllegalStateException(
                            "Unsupported M40 mesh version: " + version
                    );
                }

                int vertexCount = input.readInt();
                int indexCount = input.readInt();

                if (vertexCount <= 0
                        || vertexCount > 100_000
                        || indexCount <= 0
                        || indexCount > 1_000_000
                        || indexCount % 3 != 0) {
                    throw new IllegalStateException(
                            "Invalid M40 mesh counts: vertices="
                                    + vertexCount
                                    + ", indices="
                                    + indexCount
                    );
                }

                float[] vertices = new float[vertexCount * 8];
                for (int i = 0; i < vertices.length; i++) {
                    vertices[i] = input.readFloat();
                }

                int[] indices = new int[indexCount];
                for (int i = 0; i < indexCount; i++) {
                    int index = input.readInt();
                    if (index < 0 || index >= vertexCount) {
                        throw new IllegalStateException(
                                "M40 index out of bounds: " + index
                        );
                    }
                    indices[i] = index;
                }

                LOGGER.info(
                        "Loaded DomeSurvival M40 mask mesh: {} vertices, {} triangles",
                        vertexCount,
                        indexCount / 3
                );

                return new MeshData(vertices, indices);
            }
        } catch (Exception exception) {
            LOGGER.error("Unable to load DomeSurvival M40 mask mesh", exception);
            return MeshData.EMPTY;
        }
    }

    private record MeshData(float[] vertices, int[] indices) {
        private static final MeshData EMPTY =
                new MeshData(new float[0], new int[0]);
    }
}

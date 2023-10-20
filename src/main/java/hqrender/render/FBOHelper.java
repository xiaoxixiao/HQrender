package hqrender.render;

import static hqrender.HqRender.LOG;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.GLAllocation;

import org.apache.commons.codec.binary.Base64;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

public class FBOHelper {

    public int renderTextureSize = 128;
    public int framebufferID = -1;
    public int depthbufferID = -1;
    public int textureID = -1;

    private IntBuffer lastViewport;
    private int lastTexture;
    private int lastFramebuffer;

    public FBOHelper(int textureSize) {
        renderTextureSize = textureSize;

        createFramebuffer();
    }

    public void resize(int newSize) {
        deleteFramebuffer();
        renderTextureSize = newSize;
        createFramebuffer();
    }

    public void begin() {
        // 检查当前OpenGL的错误状态。如果有错误，通常会抛出异常或记录错误。
        checkGlErrors("FBO Begin Init");

        // 获取当前绑定的Framebuffer的ID，以便稍后恢复。
        lastFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        // 将自定义的Framebuffer绑定到OpenGL环境中。这样后续的渲染操作就会输出到这个Framebuffer。
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebufferID);
        // 创建一个直接IntBuffer，用于存储当前的视口（Viewport）设置。
        lastViewport = GLAllocation.createDirectIntBuffer(16);
        // 获取并保存当前的视口设置到lastViewport缓冲区。
        GL11.glGetInteger(GL11.GL_VIEWPORT, lastViewport);
        // 设定新的视口大小，用于我们的Framebuffer。
        GL11.glViewport(0, 0, renderTextureSize, renderTextureSize);
        // 设置OpenGL的模式为模型视图矩阵（Model-View Matrix）。
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        // 保存当前的模型视图矩阵，以便稍后可以恢复。
        GL11.glPushMatrix();
        // 重置模型视图矩阵。
        GL11.glLoadIdentity();
        // 获取并保存当前绑定的2D纹理ID，以便稍后可以恢复。
        lastTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // 设置用于清除Framebuffer的颜色。这里设置为全透明的黑色。
        GL11.glClearColor(0, 0, 0, 0);
        // 清除颜色和深度缓冲。
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        // 设置面剔除模式为剔除正面（Front Face）。
        GL11.glCullFace(GL11.GL_FRONT);
        // 启用深度测试，这样就可以正确处理3D场景中对象的前后关系。
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        // 启用光照计算。
        GL11.glEnable(GL11.GL_LIGHTING);
        // 启用法线自动单位化，这样光照计算在进行缩放时仍然准确。
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        // 再次检查OpenGL的错误状态。
        checkGlErrors("FBO Begin Final");
    }

    public void end() {
        // 检查OpenGL错误。
        checkGlErrors("FBO End Init");
        // 恢复面剔除模式为默认的剔除背面（Back Face）。
        GL11.glCullFace(GL11.GL_BACK);
        // 禁用法线自动单位化。
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        // 禁用光照计算。
        GL11.glDisable(GL11.GL_LIGHTING);
        // 设置OpenGL的模式为模型视图矩阵（Model-View Matrix）。
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        // 恢复保存的模型视图矩阵。
        GL11.glPopMatrix();
        // 恢复之前保存的视口设置。
        GL11.glViewport(lastViewport.get(0), lastViewport.get(1), lastViewport.get(2), lastViewport.get(3));
        // 恢复默认的Framebuffer。
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, lastFramebuffer);
        // 恢复之前绑定的2D纹理。
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lastTexture);
        // 再次检查OpenGL错误。
        checkGlErrors("FBO End Final");
    }

    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
    }

    public void restoreTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lastTexture);
    }

    public void saveToFile(File file) {
        // 绑定Framebuffer的纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // 设置像素存储模式
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        // 获取纹理的宽和高
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        // 创建一个整数缓冲区用于存储纹理数据
        IntBuffer texture = BufferUtils.createIntBuffer(width * height);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, texture);

        // 将缓冲区数据复制到一个整数数组中
        int[] texture_array = new int[width * height];
        texture.get(texture_array);

        // 创建一个BufferedImage对象，并将纹理数据复制到其中
        BufferedImage image = new BufferedImage(renderTextureSize, renderTextureSize, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, renderTextureSize, renderTextureSize, texture_array, 0, width);

        // 确保文件目录存在
        file.mkdirs();

        // 将图像保存为PNG格式
        try {
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            // 如果出现异常，则不执行任何操作
        }
    }

    public String getBase64() {
        // Bind framebuffer texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        IntBuffer texture = BufferUtils.createIntBuffer(width * height);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, texture);

        int[] texture_array = new int[width * height];
        texture.get(texture_array);

        BufferedImage image = new BufferedImage(renderTextureSize, renderTextureSize, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, renderTextureSize, renderTextureSize, texture_array, 0, width);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "PNG", out);
        } catch (IOException e) {
            // Do nothing
        }

        return Base64.encodeBase64String(out.toByteArray());
    }

    private void createFramebuffer() {
        // 生成一个新的帧缓冲对象
        framebufferID = EXTFramebufferObject.glGenFramebuffersEXT();

        // 生成一个新的纹理对象
        textureID = GL11.glGenTextures();

        // 保存当前绑定的帧缓冲和纹理对象，以便稍后恢复
        int currentFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // 绑定新生成的帧缓冲对象
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebufferID);

        // 设置纹理参数
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        // 创建一个空的纹理对象
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            renderTextureSize,
            renderTextureSize,
            0,
            GL12.GL_BGRA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);

        // 恢复之前绑定的纹理对象
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);

        // 创建并绑定深度缓冲
        depthbufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, depthbufferID);
        EXTFramebufferObject.glRenderbufferStorageEXT(
            EXTFramebufferObject.GL_RENDERBUFFER_EXT,
            GL11.GL_DEPTH_COMPONENT,
            renderTextureSize,
            renderTextureSize);

        // 将深度缓冲对象绑定到帧缓冲对象
        EXTFramebufferObject.glFramebufferRenderbufferEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
            EXTFramebufferObject.GL_RENDERBUFFER_EXT,
            depthbufferID);

        // 将纹理对象绑定到帧缓冲对象
        EXTFramebufferObject.glFramebufferTexture2DEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
            GL11.GL_TEXTURE_2D,
            textureID,
            0);

        // 恢复之前绑定的帧缓冲对象
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, currentFramebuffer);
    }

    private void deleteFramebuffer() {
        EXTFramebufferObject.glDeleteFramebuffersEXT(framebufferID);
        GL11.glDeleteTextures(textureID);
        EXTFramebufferObject.glDeleteRenderbuffersEXT(depthbufferID);
    }

    public static void checkGlErrors(String message) {
        int error = GL11.glGetError();

        if (error != 0) {
            String error_name = GLU.gluErrorString(error);
            LOG.error("########## GL ERROR ##########");
            LOG.error("@ " + message);
            LOG.error(error + ": " + error_name);
        }
    }
}

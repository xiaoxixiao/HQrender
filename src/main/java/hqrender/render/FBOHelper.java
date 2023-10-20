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
        // GLAllocation.createDirectIntBuffer:
        //这个方法用于创建一个直接分配的整数缓冲区，即一个"直接"的IntBuffer。直接缓冲区的主要特点是它们在本地内存中创建，而不是在Java堆上。这使得它们特别适合与本地代码和库（如OpenGL）交互，因为本地代码可以直接访问这些缓冲区的内容，无需经过任何中间转换。
        //GLAllocation可能是某个库或框架提供的工具类，专门用于处理与OpenGL相关的资源分配。从名字来看，它似乎是专门用于OpenGL资源的分配和管理。
        // (16):
        //这是createDirectIntBuffer方法的参数，它表示你想要创建的缓冲区的大小。在这种情况下，你正在创建一个可以存储16个整数的缓冲区
        // 总的来说，这行代码创建了一个直接的整数缓冲区，可以存储16个整数，并将其引用保存在lastViewport变量中。
        lastViewport = GLAllocation.createDirectIntBuffer(16);
        // GL11.glGetInteger:
        //这是OpenGL的函数调用，它的功能是获取指定的OpenGL状态变量的整数值或整数值数组。
        // GL11.GL_VIEWPORT:
        //这是一个OpenGL常量，表示我们要查询的是关于视口的状态。视口定义了渲染输出在窗口中的区域，并将裁剪区域（通常是一个矩形）映射到规范化设备坐标。
        //当你查询GL_VIEWPORT时，你会得到一个包含四个整数值的数组：[x, y, width, height]，其中：
        //x, y：是视口矩形左下角的位置。
        //width, height：是视口的宽度和高度。
        // lastViewport:
        //这是先前创建的直接整数缓冲区。它用于存储glGetInteger返回的结果。由于GL_VIEWPORT返回四个整数值，所以这个缓冲区应该至少能容纳四个整数。这解释了为什么你可能在之前创建一个能够存储16个整数的缓冲区（尽管实际上你只需要4个位置来保存视口参数）。
        // 综上所述，这行代码的目的是获取当前OpenGL上下文的视口参数，并将其保存在lastViewport缓冲区中。这在你需要保存当前视口状态，以后再恢复它时，是很有用的。 --------------------------------------------------
        GL11.glGetInteger(GL11.GL_VIEWPORT, lastViewport);
        // 设定新的视口大小，用于我们的Framebuffer。
        // GL11.glViewport:-------------
        //这是OpenGL的函数调用，用于定义裁剪窗口的大小和位置。简单地说，视口定义了你的渲染输出在整个窗口或帧缓冲区中的哪个部分。它还决定了裁剪窗口如何映射到规范化设备坐标。0, 0:
        //这是视口的左下角的位置。在这种情况下，视口位于窗口或帧缓冲区的左下角。
        // renderTextureSize, renderTextureSize:--------------
        //这是视口的宽度和高度。从代码中可以看出，视口是一个正方形，其边长为renderTextureSize.
        // 可能的应用场景是：当你正在渲染到一个纹理，这个纹理的大小是renderTextureSize x renderTextureSize，你会想要确保视口的大小与纹理的大小匹配，这样渲染的内容会完整地填充整个纹理,
        // 总的来说，这行代码的作用是设置OpenGL的视口大小和位置，确保其与给定的纹理大小匹配。这对于渲染到纹理或在帧缓冲对象中进行离屏渲染非常有用。
        GL11.glViewport(0, 0, renderTextureSize, renderTextureSize);
        // 设置OpenGL的模式为模型视图矩阵（Model-View Matrix）.
        // GL11.glMatrixMode:
        //这是OpenGL的函数调用，用于设置当前的矩阵模式。所谓的“当前的矩阵模式”是指后续的矩阵操作（如加载、乘法或其他变换）将影响哪个矩阵堆栈。
        //GL11.GL_MODELVIEW:
        //这是OpenGL的一个常量，表示模型视图矩阵模式。
        //模型视图矩阵（Modelview Matrix）用于定义物体在世界空间中的位置、旋转和缩放，以及定义摄像机或观察者的位置和方向。简单地说，模型视图矩阵描述了如何将对象从其本地坐标系变换到摄像机的视野中。
        //当OpenGL处于GL_MODELVIEW模式时，后续的矩阵操作将影响模型视图矩阵,
        // 使用这个命令的常见场景是在设置场景中的对象或摄像机的变换之前。例如，当你想移动、旋转或缩放一个物体，或者移动和旋转摄像机时，你会先设置矩阵模式为GL_MODELVIEW，然后进行相应的矩阵操作。
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        // 保存当前的模型视图矩阵，以便稍后可以恢复
        // GL11.glPushMatrix:
        //这是OpenGL的函数调用，用于将当前矩阵压入对应的矩阵堆栈。
        //当你在OpenGL中进行渲染时，你会经常使用变换矩阵来更改物体的位置、旋转和缩放。OpenGL维护了几个矩阵堆栈，其中最常用的是模型视图矩阵堆栈和投影矩阵堆栈。glPushMatrix方法允许你保存当前矩阵的状态，这样你可以在后面的操作中安全地修改它
        GL11.glPushMatrix();
        // 重置模型视图矩阵
        // GL11.glLoadIdentity:
        //这是OpenGL的函数调用，它的作用是将当前矩阵（由glMatrixMode设置的矩阵模式决定）设置为单位矩阵。
        //单位矩阵是一个特殊的矩阵，主对角线上的元素都是1，其他位置的元素都是0。在图形学中，单位矩阵对于任何向量或其他矩阵的变换都没有任何效果，就好像没有进行变换一样。
        //在OpenGL中，glLoadIdentity常常被用于以下场景：
        //重置矩阵：在应用一系列变换后（如旋转、平移、缩放等），你可能想要重置矩阵，开始一个新的变换序列。使用glLoadIdentity可以方便地将矩阵重置为其初始状态。
        //开始新的变换序列：在开始新的变换序列之前，通常首先加载单位矩阵，确保之前的变换不会影响新的变换。
        GL11.glLoadIdentity();
        // 获取并保存当前绑定的2D纹理ID，以便稍后可以恢复
        // GL11.glGetInteger:
        //这是OpenGL的一个函数，它的作用是获取指定的OpenGL状态变量的整数值。
        //GL11.GL_TEXTURE_BINDING_2D:
        //这是OpenGL的一个常量，代表当前绑定的2D纹理的ID。
        //因此，这行代码的作用是获取当前OpenGL上下文中已绑定的2D纹理的ID，并将其保存在lastTexture变量中。这通常在你需要临时改变绑定的纹理，但随后又希望恢复到原先的纹理时使用。
        lastTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // 设置用于清除Framebuffer的颜色。这里设置为全透明的黑色
        // GL11.glClearColor:
        //这是OpenGL的一个函数，用于定义当颜色缓冲区被清除时所使用的颜色。
        //参数 (0, 0, 0, 0):
        //这四个参数分别代表颜色的红、绿、蓝和alpha（透明度）成分。值的范围在0.0到1.0之间。
        //在此例中，颜色为(0, 0, 0)，这代表黑色。Alpha值为0意味着完全透明。
        //因此，这行代码的意思是，当清除颜色缓冲区时，使用完全透明的黑色作为背景颜色。
        GL11.glClearColor(0, 0, 0, 0);
        // 清除颜色和深度缓冲
        // GL11.glClear:
        //这是OpenGL的一个函数，用于清除指定的缓冲区。
        //GL11.GL_COLOR_BUFFER_BIT:
        //这是OpenGL的一个常量，表示颜色缓冲区。当此标志被设置并传递给glClear函数时，颜色缓冲区将被清除，并填充为之前使用glClearColor指定的颜色。
        //GL11.GL_DEPTH_BUFFER_BIT:
        //这是OpenGL的一个常量，表示深度缓冲区。当此标志被设置并传递给glClear函数时，深度缓冲区将被清除，并填充为之前使用glClearDepth指定的值（默认值为1.0）。
        //通过使用逻辑"或"操作（|），你可以同时清除多个缓冲区。在这个例子中，代码同时清除了颜色缓冲区和深度缓冲区。
        //清除缓冲区通常在每一帧的开始进行，以确保开始新的渲染时，之前帧的内容不会影响当前帧的渲染结果。
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        // 设置面剔除模式为剔除正面（Front Face）
        // GL11.glCullFace:
        //这是OpenGL的一个函数，用于指定哪一个方向的面（正面或背面）应该被剔除。
        //GL11.GL_FRONT:
        //这是OpenGL的一个常量，表示多边形的正面（或前面）。
        //所以，这行代码的意思是设置OpenGL只剔除正面朝向的多边形。这意味着在实际的渲染过程中，只有那些背面朝向的多边形会被渲染，正面朝向的多边形会被忽略。
        //要注意的是，仅仅调用glCullFace并不会启动面剔除机制。为了真正启用面剔除，你还需要调用glEnable(GL_CULL_FACE)。
        //通常，面剔除被用于如下场景：
        //在闭合的三维模型中，例如一个立方体或球体，由于摄像机只能看到表面，所以模型内部的面是不可见的，因此可以安全地剔除。
        //在某些算法中，如shadow volume，特定方向的面剔除是算法的一部分。
        //面剔除的方向（正面或背面）是基于多边形的顶点的顺序决定的，通常使用逆时针或顺时针规则。
        GL11.glCullFace(GL11.GL_FRONT);
        // 启用深度测试，这样就可以正确处理3D场景中对象的前后关系
        // GL11.glEnable:
        //这是OpenGL的一个函数，用于启用指定的OpenGL功能。
        //GL11.GL_DEPTH_TEST:
        //这是OpenGL的一个常量，表示深度测试功能。
        //所以，这行代码的意思是启用OpenGL的深度测试功能。
        //当深度测试被启用时：
        //每个像素都有一个深度值，通常与其在3D空间中的Z坐标相关。
        //在绘制像素之前，OpenGL会检查其深度值与深度缓冲区中该像素位置的深度值。
        //基于设置的深度比较函数（例如，GL_LESS表示只有当新像素的深度值小于或等于深度缓冲区中的值时，才绘制新像素），OpenGL会决定是否绘制这个像素。
        //启用深度测试通常是3D图形编程中的基本步骤，因为这可以确保物体以正确的顺序绘制，即使在渲染命令发送给GPU的顺序不是从前到后的情况下也是如此。这对于场景中的物体交错和覆盖的情况尤为重要。
        //但要注意，为了深度测试能正常工作，你还需要确保在清除帧缓冲区时也清除深度缓冲区（使用GL_DEPTH_BUFFER_BIT与glClear函数）。
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        // 启用光照计算
        // GL11.glEnable:
        //这是OpenGL的一个函数，用于启用指定的OpenGL功能。
        //GL11.GL_LIGHTING:
        //这是OpenGL的一个常量，表示光照功能。
        //因此，这行代码的意思是启用OpenGL的光照功能。
        //当光照被启用时：
        //场景中的物体将根据设置的光源、物体的材质属性以及光照模型的其他参数（如环境光、散射光、镜面光等）来进行渲染。
        //你可以定义一个或多个光源，每个光源都有其特定的位置、颜色和其他属性，如光的衰减率。
        //物体的材质属性决定了它如何与光互动。例如，一个具有高镜面属性的物体会有明亮的高光。
        //要使光照效果生效，除了启用GL_LIGHTING外，你通常还需要进行以下操作：
        //定义和启用一个或多个光源，例如使用GL_LIGHT0、GL_LIGHT1等。
        //设置物体的材质属性，如使用glMaterial函数。
        GL11.glEnable(GL11.GL_LIGHTING);
        // 启用法线自动单位化，这样光照计算在进行缩放时仍然准确
        // GL11.glEnable:
        //这是OpenGL的一个函数，用于启用指定的OpenGL功能。
        //GL12.GL_RESCALE_NORMAL:
        //这是OpenGL的一个常量，表示法线重缩放功能。
        //当GL_RESCALE_NORMAL被启用时：
        //OpenGL会自动根据模型视图矩阵的缩放因子对法线进行重缩放，确保它们保持单位长度，从而使光照计算保持正确。
        //这对于进行了非均匀缩放的物体尤为重要，因为非均匀缩放可能导致法线不再垂直于表面。
        //值得注意的是，GL_RESCALE_NORMAL适用于进行了均匀缩放的物体。如果物体经历了非均匀缩放。
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

        // 使用OpenGL的glGetError方法来获取最新的错误码。
        int error = GL11.glGetError();

        // 检查是否有错误发生。GL11.GL_NO_ERROR的值为0，所以这里简单地检查error是否不等于0。
        if (error != 0) {
            //使用GLU.gluErrorString方法将错误码转换为人类可读的错误描述字符串。
            String error_name = GLU.gluErrorString(error);
            //使用某个日志库（如SLF4J、Log4j等）的error方法来记录错误。这里连续记录了三条错误消息，提供了
            // 错误的上下文、
            // 错误码和
            // 错误描述。
            LOG.error("########## GL ERROR ##########");
            LOG.error("@ " + message);
            LOG.error(error + ": " + error_name);
        }
    }
}

package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.resources.DataType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The sources of a shader program (CPU side): read by a worker, compiled by its GL element (Shader).
 * Its paths are the files of the stages, from the working directory: vertex, fragment, and geometry when there are three.
 */
public class ShaderData extends DataType {
    private final String[] paths;
    private final boolean shader2D;

    // Files watched for hot reload: replaced in full, read by the watcher thread
    private volatile List<String> sourcePaths;

    // Written by the preload (worker), read once loaded (main thread)
    private String vertexSource, fragmentSource, geometrySource;
    // By stage: the files of the #line ids of its source (ShaderPreprocessor.files), to translate the compiler log
    private List<List<String>> stageFiles = Collections.emptyList();

    public ShaderData(String name, boolean shader2D, String ... paths) {
        super(TYPE_SET_UP.THREAD_CONTEXT, name);
        this.paths = paths.clone();
        this.shader2D = shader2D;
        sourcePaths = validPaths() ? List.of(this.paths) : Collections.emptyList();
    }

    public boolean isDimension2DShader() {
        return shader2D;
    }

    public boolean usesGeometryShader() {
        return paths.length == 3;
    }

    public int numberSource() {
        return paths.length;
    }

    public String getSourcePath(int index) {
        return paths[index];
    }

    private boolean validPaths() {
        for (String path : paths) {
            if (path == null || path.isEmpty())
                return false;
        }

        return true;
    }

    @Override
    public boolean hasSource() {
        return validPaths();
    }

    @Override
    public List<String> sourcePaths() {
        return sourcePaths;
    }

    // Preload (worker): the files the sources are made of with their includes, and the missing ones (watched)
    public void setSourcePaths(Collection<String> files) {
        sourcePaths = List.copyOf(files);
    }

    // The new version failed (a missing include): its files are watched too, so creating the include reloads it
    @Override
    protected void reloadFailed(DataType copy) {
        Set<String> files = new LinkedHashSet<>(sourcePaths);
        files.addAll(copy.sourcePaths());
        setSourcePaths(files);
    }

    // Preload (worker): the preprocessed sources, geometry null without a geometry stage
    public void setSources(String vertex, String fragment, String geometry) {
        vertexSource = vertex;
        fragmentSource = fragment;
        geometrySource = geometry;
    }

    // Preload (worker): by stage (vertex, fragment, geometry), the files of the #line ids of its source
    public void setStageFiles(List<List<String>> files) {
        stageFiles = List.copyOf(files);
    }

    // Files of the #line ids of the source of the stage (0 vertex, 1 fragment, 2 geometry), empty when unknown
    public List<String> getStageFiles(int stage) {
        return stage < stageFiles.size() ? stageFiles.get(stage) : Collections.emptyList();
    }

    public String getVertexSource() {
        return vertexSource;
    }

    public String getFragmentSource() {
        return fragmentSource;
    }

    public String getGeometrySource() {
        return geometrySource;
    }

    // Nothing to compute: the sources are read by the preload, compiled by the GL element
    @Override
    protected boolean internLoad() {
        return vertexSource != null && fragmentSource != null && (!usesGeometryShader() || geometrySource != null);
    }

    // Subtypes give their own copy: a ShaderData copy would lose what they add
    @Override
    protected DataType createReloadCopy() {
        if (getClass() != ShaderData.class || !validPaths())
            return null;

        return new ShaderData(dataName, shader2D, paths);
    }

    @Override
    protected void swapContent(DataType copy) {
        ShaderData other = (ShaderData) copy;

        String vertex = other.vertexSource, fragment = other.fragmentSource, geometry = other.geometrySource;
        other.vertexSource = vertexSource;
        other.fragmentSource = fragmentSource;
        other.geometrySource = geometrySource;
        vertexSource = vertex;
        fragmentSource = fragment;
        geometrySource = geometry;

        List<String> files = other.sourcePaths;
        other.sourcePaths = sourcePaths;
        sourcePaths = files;

        List<List<String>> stages = other.stageFiles;
        other.stageFiles = stageFiles;
        stageFiles = stages;
    }

    @Override
    public Class<? extends GLElement> glElementType() {
        return Shader.class;
    }

    @Override
    public GLElement createGLElement() {
        return new Shader(this);
    }

    @Override
    protected void internCancelPreload() {
        internUnload();
    }

    @Override
    protected void internUnload() {
        vertexSource = null;
        fragmentSource = null;
        geometrySource = null;
    }
}

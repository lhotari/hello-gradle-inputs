package inputtest;

import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.CompositeFileCollection;
import org.gradle.api.internal.file.CompositeFileTree;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.internal.file.collections.FileTreeAdapter;
import org.gradle.api.internal.file.collections.MinimalFileTree;
import org.gradle.api.tasks.TaskState;

import java.io.File;
import java.lang.reflect.Method;

public class TaskInputsTaskListener implements TaskExecutionListener {
    @Override
    public void beforeExecute(Task task) {

    }

    @Override
    public void afterExecute(Task task, TaskState state) {
        System.out.println("--- TASK " + task.getPath() + " ---");
        System.out.println("BuildDir is " + task.getProject().getBuildDir());
        handleFileCollection(task.getInputs().getFiles());
        System.out.println("-------------");
    }

    private void handleFileCollection(FileCollection fileCollection) {
        if (fileCollection instanceof UnionFileCollection) {
            for (FileCollection source : ((UnionFileCollection) fileCollection).getSources()) {
                handleFileCollection(source);
            }
            return;
        }
        if (fileCollection instanceof FileTreeAdapter) {
            MinimalFileTree minimalFileTree = ((FileTreeAdapter) fileCollection).getTree();
            if (minimalFileTree instanceof DirectoryTree) {
                addDirectoryTree((DirectoryTree) minimalFileTree);
                return;
            }
        }
        FileTree fileTree = fileCollection.getAsFileTree();
        if (fileTree instanceof CompositeFileTree) {
            for (FileCollection sourceCollection : getSourceCollections((CompositeFileTree) fileTree)) {
                handleFileCollection(sourceCollection);
            }
            return;
        }

        for (File file : fileCollection.getFiles()) {
            addFile(file);
        }
    }

    // call CompositeFileCollection.getSourceCollections by using reflection since the method is protected
    private Iterable<FileCollection> getSourceCollections(CompositeFileTree fileTree) {
        try {
            Method method = CompositeFileCollection.class.getDeclaredMethod("getSourceCollections");
            method.setAccessible(true);
            return (Iterable<FileCollection>) method.invoke(fileTree);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addFile(File file) {
        System.out.println("Input file: " + file);
    }

    private void addDirectoryTree(DirectoryTree tree) {
        System.out.println("Input directory: " + tree.getDir() + " includes: " + tree.getPatterns().getIncludes() + " excludes: " + tree.getPatterns().getExcludes());
    }
}
package com.stock.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 项目根目录解析工具。确保相对路径在任何工作目录下都能正确解析。
 */
public class ProjectPaths {

    private static Path projectRoot;

    static {
        projectRoot = detectProjectRoot();
    }

    private static Path detectProjectRoot() {
        // 从当前工作目录开始，向上查找包含 csv/ 目录的根
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 5; i++) {
            if (Files.isDirectory(dir.resolve("csv")) && Files.isDirectory(dir.resolve("data"))) {
                return dir;
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        // 兜底：使用当前工作目录
        return Paths.get("").toAbsolutePath();
    }

    public static Path getProjectRoot() {
        return projectRoot;
    }

    public static Path resolve(String relative) {
        return projectRoot.resolve(relative);
    }
}

package com.cklsit.logsviewer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogsViewer - 轻量级游戏内日志查看插件。
 *
 * 设计目标：小而快，兼容 1.8.x - 1.21.x 全版本服务器。
 * 实现上只依赖最基础的 Bukkit API（Java 8 编译产物），
 * 按需读取 logs/latest.log，从尾部向前筛选最近 N 条日志。
 */
public final class LogsViewer extends JavaPlugin implements CommandExecutor {

    /** 单次最多显示的条数，防止刷屏 */
    private static final int MAX_LINES = 50;

    /** 标准日志行: [HH:mm:ss LEVEL]: message（堆栈等非标准行会被附加到上一条） */
    private static final Pattern LINE_PATTERN =
            Pattern.compile("^\\[(\\d{2}:\\d{2}:\\d{2})\\s+([A-Za-z]+)\\]:\\s?(.*)$");

    @Override
    public void onEnable() {
        getCommand("logsview").setExecutor(this);
        getLogger().info("LogsViewer enabled (1.8.x - 1.21.x) - use /logsview <error|warn|info> <amount> [content]");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /logsview <error|warn|info> <数量> [关键词]");
            sender.sendMessage(ChatColor.GRAY + "示例: /logsview error 10        # 最近 10 条 ERROR 日志");
            sender.sendMessage(ChatColor.GRAY + "示例: /logsview warn 5 崩溃      # 最近 5 条含“崩溃”的 WARN 日志");
            return true;
        }

        String level = args[0].toLowerCase(Locale.ROOT);
        if (!level.equals("error") && !level.equals("warn") && !level.equals("info")) {
            sender.sendMessage(ChatColor.RED + "日志类型只能是 error / warn / info");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "数量必须是正整数");
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "数量必须是正整数");
            return true;
        }
        if (amount > MAX_LINES) {
            amount = MAX_LINES;
            sender.sendMessage(ChatColor.GRAY + "单次最多显示 " + MAX_LINES + " 条，已按上限处理");
        }

        String keyword = args.length >= 3 ? args[2] : null;

        File logFile = new File(Bukkit.getWorldContainer(), "logs/latest.log");
        if (!logFile.isFile()) {
            sender.sendMessage(ChatColor.RED + "找不到日志文件: " + logFile.getPath());
            return true;
        }

        List<LogEntry> entries;
        try {
            entries = readLog(logFile);
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "读取日志失败: " + e.getMessage());
            getLogger().warning("Failed to read " + logFile.getPath() + ": " + e.getMessage());
            return true;
        }

        // 从尾部（最新）向前收集匹配的条目
        List<LogEntry> result = new ArrayList<LogEntry>();
        for (int i = entries.size() - 1; i >= 0 && result.size() < amount; i--) {
            LogEntry entry = entries.get(i);
            if (!entry.level.equalsIgnoreCase(level)) {
                continue;
            }
            if (keyword != null
                    && !entry.text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                continue;
            }
            result.add(entry);
        }

        if (result.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "没有找到匹配的 " + level.toUpperCase(Locale.ROOT)
                    + " 日志" + (keyword != null ? "（关键词: " + keyword + "）" : ""));
            return true;
        }

        ChatColor color = colorFor(level);
        sender.sendMessage(ChatColor.DARK_AQUA + "===== LogsViewer | 最近 " + result.size()
                + " 条 " + level.toUpperCase(Locale.ROOT) + " 日志 =====");
        for (LogEntry entry : result) {
            for (String line : entry.text.split("\n", -1)) {
                sender.sendMessage(color + line);
            }
        }
        return true;
    }

    /** 按行读取日志文件，非标准行（异常堆栈、多行输出）附加到上一条日志 */
    private List<LogEntry> readLog(File file) throws IOException {
        List<LogEntry> entries = new ArrayList<LogEntry>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = LINE_PATTERN.matcher(line);
                if (m.matches()) {
                    entries.add(new LogEntry(m.group(2), line));
                } else if (!entries.isEmpty() && !line.trim().isEmpty()) {
                    entries.get(entries.size() - 1).text += "\n" + line;
                }
            }
        } finally {
            reader.close();
        }
        return entries;
    }

    private ChatColor colorFor(String level) {
        if (level.equalsIgnoreCase("error")) {
            return ChatColor.RED;
        }
        if (level.equalsIgnoreCase("warn")) {
            return ChatColor.YELLOW;
        }
        return ChatColor.GREEN;
    }

    /** 一条日志（主行 + 可能跟随的堆栈/续行） */
    private static final class LogEntry {
        final String level;
        String text;

        LogEntry(String level, String text) {
            this.level = level;
            this.text = text;
        }
    }
}

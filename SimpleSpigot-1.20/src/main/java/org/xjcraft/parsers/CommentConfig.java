package org.xjcraft.parsers;

import com.google.common.base.Charsets;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlRepresenter;
import org.jetbrains.annotations.NotNull;
import org.xjcraft.utils.StringUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CommentConfig extends YamlConfiguration {
    Map<String, String> comments = new HashMap<>();
    private final Yaml yaml;
    private final DumperOptions yamlOptions = new DumperOptions();
    private final LoaderOptions loaderOptions = new LoaderOptions();
    private final Representer yamlRepresenter = new YamlRepresenter();

    public CommentConfig(File file) {
        super();
        this.yaml = new Yaml(new SafeConstructor(new LoaderOptions()), this.yamlRepresenter, this.yamlOptions);
        Validate.notNull(file, "File cannot be null");
//        YamlConfiguration config = new YamlConfiguration();

        try {
            load(file);
        } catch (Exception var4) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, var4);
        }
    }

    public void load(File file) throws IOException, InvalidConfigurationException {
        Validate.notNull(file, "File cannot be null");
        FileInputStream stream = new FileInputStream(file);
        super.load((new InputStreamReader(stream, Charsets.UTF_8)));
    }

    //替换父类的yaml
    @Override
    public void loadFromString(@NotNull String contents) throws InvalidConfigurationException {
        Validate.notNull(contents, "Contents cannot be null");

        Map<?, ?> input;
        try {
            loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE); // SPIGOT-5881: Not ideal, but was default pre SnakeYAML 1.26
            input = (Map<?, ?>) yaml.load(contents);
        } catch (YAMLException e) {
            throw new InvalidConfigurationException(e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }

        String header = parseHeader(contents);
        if (header.length() > 0) {
            options().header(header);
        }

        this.map.clear();

        if (input != null) {
            convertMapsToSections(input, this);
        }
    }

    //替换父类的yaml
    @NotNull
    protected String parseHeader(@NotNull String input) {
        String[] lines = input.split("\r?\n", -1);
        StringBuilder result = new StringBuilder();
        boolean readingHeader = true;
        boolean foundHeader = false;

        for (int i = 0; (i < lines.length) && (readingHeader); i++) {
            String line = lines[i];

            if (line.startsWith(COMMENT_PREFIX)) {
                if (i > 0) {
                    result.append("\n");
                }

                if (line.length() > COMMENT_PREFIX.length()) {
                    result.append(line.substring(COMMENT_PREFIX.length()));
                }

                foundHeader = true;
            } else if ((foundHeader) && (line.length() == 0)) {
                result.append("\n");
            } else if (foundHeader) {
                readingHeader = false;
            }
        }

        return result.toString();
    }

    //替换父类的yaml
    protected void convertMapsToSections(@NotNull Map<?, ?> input, @NotNull ConfigurationSection section) {
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map) {
                convertMapsToSections((Map<?, ?>) value, section.createSection(key));
            } else {
                section.set(key, value);
            }
        }
    }


    public boolean saveCommentConfig(File file, boolean hasComment) throws IOException {
        this.yamlOptions.setIndent(this.options().indent());
        this.yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yamlOptions.setAllowUnicode(true);
        this.yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        String dump = yaml.dump(getValues(false));
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);
        try {
            writer.write(dump);
        } finally {
            writer.close();
        }
//        yamlConfiguration.save(file);
        if (!hasComment) return true;
        File temp = new File(file.getPath() + ".tmp");
        String encodeing = "UTF-8";
//        for (String s : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
//            if (s.contains("Dfile.encoding")) {
//                encodeing = s.substring(s.indexOf('=') + 1, s.length());
//            }
//        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encodeing));
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(temp.getPath()), encodeing));
        String line;
        Tree tree = new Tree(null, "");
        int level = 0;

        while ((line = br.readLine()) != null) {
            if (line.contains(":")) {
                int i = countLevel(line);
                if (i <= level) {
                    tree = new Tree(tree.getFather(i - 1), line);
                } else {
                    tree = new Tree(tree, line);
                }
                level = tree.getLevel();
                String substring = tree.getKey();
                if (comments.containsKey(substring)) {
                    for (int j = 0; j < i; j++) {
                        pw.print("  ");
                    }
                    pw.print("#");
                    pw.println(comments.get(substring));
                }
            }
            pw.println(line);
        }
        br.close();
        pw.close();
        file.delete();
//        file.renameTo(new File(file.getPath()+".old"));
        temp.renameTo(file);
        return true;
    }


    private int countLevel(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '-') continue;
            return i / 2;
        }
        return -1;
    }

    public void set(String key, Object value, String comment) {
        super.set(key, value);
        this.set(key, comment);
    }

    public void set(String key, String comment) {
        if (comment != null) {
            comments.put(key, comment);
        }
    }


    public static class Tree {
        Tree father;
        String key;

        public Tree(Tree father, String key) {
            this.key = StringUtil.isEmpty(key) ? null : getKeyword(key);
            this.father = father;


        }

        private String getKeyword(String key) {
            int start = -1;
            int end = -1;
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (c != ' ' && c != '-') {
                    if (start < 0) {
                        start = i;
                    }
                    if (c == ':') {
                        end = i;
                        break;
                    }
                }
            }
            return key.substring(start, end);
        }


        public int getLevel() {
            if (father == null) return -1;
            return father.getLevel() + 1;
        }

        public String getKey() {
            if (father == null) return "";
            String parent = father.getKey();
            if (!StringUtil.isEmpty(parent))
                parent += ".";
            return parent + key;
        }

        public Tree getFather(int i) {
            if (father == null) return this;
            if (getLevel() == i) return this;
            return father.getFather(i);
        }
    }
}

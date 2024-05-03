package org.xjcraft.parsers;

import com.google.common.base.Charsets;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlRepresenter;
import org.xjcraft.utils.StringUtil;
import org.yaml.snakeyaml.DumperOptions;
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
    private final Representer yamlRepresenter = new YamlRepresenter();

    public static CommentConfig create(File file) {
        return new CommentConfig(file);
    }

    public CommentConfig(File file) {
        super();
        this.yaml = new Yaml(new SafeConstructor(), this.yamlRepresenter, this.yamlOptions);
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

    public void loadFromString(String contents) throws InvalidConfigurationException {
        Validate.notNull(contents, "Contents cannot be null");

        Map input;
        try {
            input = (Map) this.yaml.load(contents);
        } catch (YAMLException var4) {
            throw new InvalidConfigurationException(var4);
        } catch (ClassCastException var5) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }

        String header = this.parseHeader(contents);
        if (header.length() > 0) {
            this.options().header(header);
        }

        if (input != null) {
            this.convertMapsToSections(input, this);
        }

    }
//    @Override
//    public Map<String, Object> getValues(boolean deep) {
//        Map<String, Object> map = super.getValues(deep);
//        squash(map);
//        return map;
//    }
//
//    private void squash(Map<String, Object> map) {
//        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
//        while (iterator.hasNext()){
//            Map.Entry<String, Object> next = iterator.next();
//            if (next.getValue() instanceof MemorySection){
//                ((MemorySection) next.getValue()).getValues(false)
//            }
//        }
//    }

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

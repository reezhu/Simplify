package org.xjcraft.example.config;

import lombok.Data;
import org.xjcraft.annotation.Comment;
import org.xjcraft.annotation.Instance;
import org.xjcraft.annotation.RConfig;

/**
 * Created by Ree on 2017/8/23.
 */
@RConfig
@Data
public class ExampleParentConfig {
    @Instance
    public static final ExampleParentConfig config = new ExampleParentConfig();
    @Comment("详细的信息打印")
    Boolean debug = true;
}

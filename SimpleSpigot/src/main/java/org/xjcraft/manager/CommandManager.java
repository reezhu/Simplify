package org.xjcraft.manager;

import com.google.gson.JsonObject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.xjcraft.CommonPlugin;
import org.xjcraft.adapter.Pluggable;
import org.xjcraft.annotation.RCommand;
import org.xjcraft.annotation.RCommand.Permisson;
import org.xjcraft.api.CommonCommandExecutor;
import org.xjcraft.utils.JSON;
import org.xjcraft.utils.StringUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandManager {

    private Pluggable plugin;
    private Map<String, CommandElement> map = new LinkedHashMap<>();
    private static String MAGIC = "@";  //标记用魔法字符

    public CommandManager(Pluggable plugin) {
        this.plugin = plugin;
    }

    public void register(CommonCommandExecutor executor) {
        Class<? extends CommonCommandExecutor> aClass = executor.getClass();
        for (Method method : aClass.getDeclaredMethods()) {
            RCommand annotation = method.getAnnotation(RCommand.class);
            if (annotation == null) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                plugin.getLoggerApi().warning("注入方法需要设置为公开：" + method.getName());
                return;
            }
            String value = annotation.value();
            CommandElement commandElement = new CommandElement();
            int i = 0;
            Type[] parameterTypes = method.getGenericParameterTypes();
            String example = "/<label> " + value;
            for (Type clazz : parameterTypes) {
                if (clazz == CommandSender.class || clazz == Command.class) {

                } else if (clazz == String.class || clazz == Integer.class || clazz == Double.class || clazz == Player.class || clazz == Boolean.class || clazz == Long.class || clazz == JsonObject.class) {
                    i++;
                    example += " <" + ((Class) clazz).getSimpleName() + ">";
                } else {
                    plugin.getLoggerApi().warning("不支持的指令变量名：" + " <" + clazz.getClass().getSimpleName() + " " + clazz.getTypeName() + "> 于指令" + value + "中");
                    return;
                }
            }
            if (!StringUtil.isEmpty(annotation.desc())) {
                example += "  ";
                example += annotation.desc();
            }
            commandElement.setExample(example);
            commandElement.setParameters(method.getParameterTypes());
            commandElement.setSize(i);
            commandElement.setMethod(method);
            commandElement.setExcutor(executor);
            commandElement.setConfig(annotation);
            map.put(value + MAGIC + i, commandElement);
        }

    }

    public boolean dispatch(CommandSender sender, Command command, String label, String[] args) {
        String key;
        if (args.length < 1) {
            if (map.containsKey("help" + MAGIC + "0")) {
                key = "help" + MAGIC + "0";
            } else {
                for (CommandElement commandElement : map.values()) {
                    sender.sendMessage(commandElement.getExample().replaceAll("<label>", label));
                }
                return true;
            }


        } else {
            key = args[0] + MAGIC + (args.length - 1);
        }
        if (map.containsKey(key)) {

            CommandElement commandElement = map.get(key);
            //长度判断
            if (args.length > 0 && args.length - 1 < commandElement.size) {
                sender.sendMessage(commandElement.getExample().replaceAll("<label>", label));
                return true;
            }
            //运行者判断
            if (commandElement.getConfig().sender() != RCommand.Sender.ALL) {
                if ((commandElement.getConfig().sender() != RCommand.Sender.CONSOLE || !(sender instanceof ConsoleCommandSender)) && (commandElement.getConfig().sender() != RCommand.Sender.PLAYER || !(sender instanceof Player))) {
                    sender.sendMessage(String.format("该指令只能由%s执行！", commandElement.getConfig().sender() == RCommand.Sender.CONSOLE ? "控制台" : "玩家"));
                    return true;
                }
            }
            //权限判断
            boolean hasPermissionConfig = !StringUtil.isEmpty(commandElement.getConfig().permisson());
            boolean hasPermission = hasPermissionConfig && sender.hasPermission(commandElement.getConfig().permisson());
//            if (!StringUtil.isEmpty(commandElement.getConfig().permisson())) {
//                if (!sender.hasPermission(commandElement.getConfig().permisson())) {
//                    sender.sendMessage(command.getPermissionMessage());
//                    return true;
//                }
//            }
            //当配置了权限但是默认用户是all时，将默认用户切换为op以判断权限
            RCommand.Permisson defaultUser = commandElement.getConfig().defaultUser();
            if (hasPermissionConfig && defaultUser == Permisson.ALL) {
                defaultUser = RCommand.Permisson.OP;
            }
            //判断默认执行者
            switch (defaultUser) {
                case OP:
                    if (!hasPermission && !sender.isOp()) {
                        sender.sendMessage(command.getPermissionMessage());
                        return true;
                    }
                    break;
                case NONE:
                    if (!hasPermission) {
                        sender.sendMessage(command.getPermissionMessage());
                        return true;
                    }
                    break;
                case NotOP:
                    if (!hasPermission && sender.isOp()) {
                        sender.sendMessage(command.getPermissionMessage());
                        return true;
                    }
                    break;
                default:
                    break;
            }

            try {
                //元素注入
                Class[] elementParameters = commandElement.getParameters();
                Object[] parameters = new Object[elementParameters.length];
                int count = 1;
                for (int i = 0; i < parameters.length; i++) {
                    switch (elementParameters[i].getSimpleName()) {
                        case "CommandSender":
                            parameters[i] = sender;
                            break;
                        case "Command":
                            parameters[i] = command;
                            break;
                        case "Boolean":
                            String bl = args[count++];
                            parameters[i] = StringUtil.isYes(bl);
                            break;
                        case "Integer":
                            parameters[i] = Integer.parseInt(args[count++]);
                            break;
                        case "Double":
                            parameters[i] = Double.parseDouble(args[count++]);
                            break;
                        case "Long":
                            parameters[i] = Long.parseLong(args[count++]);
                            break;
                        case "Player":
                            parameters[i] = ((CommonPlugin) plugin).getServer().getPlayerExact(args[count++]);
                            if (parameters[i] == null) {
                                sender.sendMessage(String.format("玩家%s不存在！ Player %s not found!", args[count - 1], args[count - 1]));
                                sender.sendMessage(commandElement.getExample().replaceAll("<label>", label));
                                return true;
                            }
                            break;
                        case "JsonObject":
                            try {
                                parameters[i] = JSON.parseJSON(args[count++], JsonObject.class);
                            } catch (Exception e) {
                                sender.sendMessage(String.format("%s不是一个有效的json格式, not a valid json format", args[count - 1]));
                                e.printStackTrace();
                            }
                            break;
                        default:
                            parameters[i] = args[count++];
                            break;

                    }
                }

                commandElement.getMethod().invoke(commandElement.getExcutor(), parameters);
            } catch (Exception e) {
                plugin.getLoggerApi().warning(String.format("指令%s运行失败，指令示例：%s ，实际指令：%s", label, commandElement.getExample().replaceAll("<label>", label), label + " " + StringUtil.join(args, " ")));
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }

    }


}

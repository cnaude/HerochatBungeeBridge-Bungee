package com.jumanjicraft.bungeechat;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class BungeeChat extends Plugin implements Listener {

    private boolean filter;
    private boolean whitelist;
    private List<String> channels;
    private boolean log;
    private boolean logFilter;

    @Override
    public void onEnable() {
        getProxy().registerChannel("BungeeChat");
        getProxy().getPluginManager().registerListener(this, this);
        try {
            loadConfig();
        } catch (IOException e) {
            getProxy().getLogger().warning("Error while loading config:");
            getProxy().getLogger().warning(e.getMessage());

            this.filter = false;
        }
        getProxy().getPluginManager().registerCommand(this, new Command("bungeechatreload") {
            @Override
            public void execute(CommandSender arg0, String[] arg1) {
                try {
                    BungeeChat.this.loadConfig();

                    BungeeChat.this.getProxy().getLogger().info("Reloaded config");
                } catch (IOException e) {
                    BungeeChat.this.getProxy().getLogger().warning("Error while loading config:");
                    BungeeChat.this.getProxy().getLogger().warning(e.getMessage());

                    BungeeChat.this.filter = false;
                }
            }
        });
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel("BungeeChat");
    }

    private void loadConfig()
            throws IOException {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            file.createNewFile();
            InputStream in = getResourceAsStream("example_config.yml");
            Throwable localThrowable6 = null;
            try {
                OutputStream out = new FileOutputStream(file);
                Throwable localThrowable7 = null;
                try {
                    ByteStreams.copy(in, out);
                } catch (IOException localThrowable1) {
                    localThrowable7 = localThrowable1;
                    throw localThrowable1;
                } finally {
                }
            } catch (IOException localThrowable4) {
                localThrowable6 = localThrowable4;
                throw localThrowable4;
            } finally {
                if (in != null) {
                    if (localThrowable6 != null) {
                        try {
                            in.close();
                        } catch (IOException localThrowable5) {
                            localThrowable6.addSuppressed(localThrowable5);
                        }
                    } else {
                        in.close();
                    }
                }
            }
        }
        Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

        this.filter = configuration.getBoolean("filter");
        this.whitelist = configuration.getBoolean("whitelist");
        this.log = configuration.getBoolean("log");
        this.logFilter = configuration.getBoolean("log_filter");

        Object newChannels = new ArrayList();
        for (String channel : configuration.getStringList("channels")) {
            ((List) newChannels).add(channel.toLowerCase());
        }
        this.channels = ((List) newChannels);
    }

    @EventHandler
    public void receievePluginMessage(PluginMessageEvent event)
            throws IOException {
        if (!event.getTag().equalsIgnoreCase("BungeeChat")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String chatChannel = in.readUTF().toLowerCase();
        String message = in.readUTF();
        if ((this.log) && (!this.logFilter)) {
            getProxy().getLogger().info(message);
        }
        if (this.filter) {
            if (this.whitelist) {
                if (this.channels.contains(chatChannel)) {
                }
            } else if (this.channels.contains(chatChannel)) {
                return;
            }
        }
        if ((this.log) && (this.logFilter)) {
            getProxy().getLogger().info(message);
        }
        InetSocketAddress addr = event.getSender().getAddress();
        for (ServerInfo server : getProxy().getServers().values()) {
            if ((!server.getAddress().equals(addr))
                    && (!server.getPlayers().isEmpty())) {
                server.sendData("BungeeChat", event.getData());
            }
        }
    }
}

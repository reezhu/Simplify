# SimpleBukkit
A simple frame to reduce redundancy work

a sample to initialize config and dao:

    Config config = (Config) loadConfig(Config.config);
    Manager manager = (Manager) setupDatabase(config, this, new Manager());

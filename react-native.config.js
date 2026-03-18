module.exports = {
  dependency: {
    platforms: {
      android: {
        packageInstance: 'new NitroTelegramTabBarPackage()',
        packageImportPath:
          'import com.margelo.telegramtabbar.NitroTelegramTabBarPackage;',
      },
      // iOS is handled by expo-module.config.json + use_expo_modules! autolinking.
      // Defining it here would cause use_native_modules! to add the pod a second time.
      ios: null,
    },
  },
}

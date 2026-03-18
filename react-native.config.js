module.exports = {
  dependency: {
    platforms: {
      android: {
        packageInstance: 'new NitroTelegramTabBarPackage()',
        packageImportPath:
          'import com.margelo.telegramtabbar.NitroTelegramTabBarPackage;',
      },
      ios: {
        podspecPath: 'NitroTelegramTabBar.podspec',
        configurations: [],
      },
    },
  },
}

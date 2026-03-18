require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name             = "NitroTelegramTabBar"
  s.version          = package["version"]
  s.summary          = package["description"]
  s.homepage         = "https://github.com/milautonomos/react-native-telegram-tabbar"
  s.license          = { :type => "MIT" }
  s.author           = package["author"]
  s.source           = { :git => "https://github.com/milautonomos/react-native-telegram-tabbar.git", :tag => s.version.to_s }

  s.platforms        = { :ios => "15.1" }
  s.swift_version    = "5.9"
  s.requires_arc     = true

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.dependency "ExpoModulesCore"

  s.pod_target_xcconfig = {
    "SWIFT_VERSION" => "5.9",
    "DEFINES_MODULE" => "YES",
  }
end
